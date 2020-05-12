package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.CloudNet;
import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.AbstractConsoleAnimation;
import de.dytanic.cloudnet.event.setup.SetupCancelledEvent;
import de.dytanic.cloudnet.event.setup.SetupCompleteEvent;
import de.dytanic.cloudnet.event.setup.SetupInitiateEvent;
import de.dytanic.cloudnet.event.setup.SetupResponseEvent;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ConsoleQuestionListAnimation extends AbstractConsoleAnimation {
    private final Supplier<String> headerSupplier;
    private final Supplier<String> footerSupplier;
    private final Supplier<Collection<String>> lastCachedMessagesSupplier;

    private final String overwritePrompt;

    private String previousPrompt;
    private boolean previousPrintingEnabled;
    private List<String> previousHistory;

    private final Map<String, Object> results = new HashMap<>();

    private Queue<QuestionListEntry<?>> entries = new LinkedBlockingQueue<>();

    private final Collection<BiConsumer<QuestionListEntry<?>, Object>> entryCompletionListeners = new ArrayList<>();

    private int currentCursor = 1;

    private boolean cancelled = false;
    private boolean cancellable = true;

    public ConsoleQuestionListAnimation(Supplier<Collection<String>> lastCachedMessagesSupplier, Supplier<String> headerSupplier, Supplier<String> footerSupplier, String overwritePrompt) {
        this(null, lastCachedMessagesSupplier, headerSupplier, footerSupplier, overwritePrompt);
    }

    public ConsoleQuestionListAnimation(String name, Supplier<Collection<String>> lastCachedMessagesSupplier, Supplier<String> headerSupplier, Supplier<String> footerSupplier, String overwritePrompt) {
        super(name);
        this.lastCachedMessagesSupplier = lastCachedMessagesSupplier;
        this.headerSupplier = headerSupplier;
        this.footerSupplier = footerSupplier;
        this.overwritePrompt = overwritePrompt;

        super.setStaticCursor(true);
        super.setCursor(0);
    }

    public void addEntry(QuestionListEntry<?> entry) {
        this.entries.add(entry);
    }

    public void addEntriesFirst(QuestionListEntry<?>... entries) {
        if (entries.length == 0) {
            return;
        }
        Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>(Arrays.asList(entries));
        newEntries.addAll(this.entries);
        this.entries = newEntries;
    }

    public void addEntriesAfter(String keyBefore, QuestionListEntry<?>... entries) {
        if (entries.length == 0) {
            return;
        }
        Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>();
        for (QuestionListEntry<?> oldEntry : this.entries) {
            newEntries.add(oldEntry);
            if (oldEntry.getKey().equals(keyBefore)) {
                newEntries.addAll(Arrays.asList(entries));
            }
        }
        this.entries = newEntries;
    }

    public void addEntriesBefore(String keyAfter, QuestionListEntry<?>... entries) {
        if (entries.length == 0) {
            return;
        }
        Queue<QuestionListEntry<?>> newEntries = new LinkedBlockingQueue<>();
        for (QuestionListEntry<?> oldEntry : this.entries) {
            if (oldEntry.getKey().equals(keyAfter)) {
                newEntries.addAll(Arrays.asList(entries));
            }
            newEntries.add(oldEntry);
        }
        this.entries = newEntries;
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancellable(boolean cancellable) {
        this.cancellable = cancellable;
    }

    public boolean isCancellable() {
        return this.cancellable;
    }

    public Map<String, Object> getResults() {
        return this.results;
    }

    public Object getResult(String key) {
        return this.results.get(key);
    }

    public boolean hasResult(String key) {
        return this.results.containsKey(key);
    }

    public void addEntryCompletionListener(BiConsumer<QuestionListEntry<?>, Object> listener) {
        this.entryCompletionListeners.add(listener);
    }

    @Override
    public void setConsole(IConsole console) {
        super.setConsole(console);
        this.previousPrintingEnabled = console.isPrintingEnabled();
        this.previousPrompt = console.getPrompt();
        this.previousHistory = console.getCommandHistory();

        console.setCommandHistory(null);

        if (this.overwritePrompt != null) {
            console.setPrompt(this.overwritePrompt);
        }

        String header = this.headerSupplier.get();
        if (header != null) {
            console.forceWriteLine(header);
        }

        console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-explain"));
        if (this.isCancellable()) {
            console.forceWriteLine("&e" + LanguageManager.getMessage("ca-question-list-cancel"));
        }

        console.disableAllHandlers();

        CloudNet.getInstance().getEventManager().callEvent(new SetupInitiateEvent(this));
    }

    @Override
    protected boolean handleTick() {
        QuestionListEntry<?> entry;

        if (this.entries.isEmpty() || (entry = this.entries.poll()) == null) {
            return true;
        }

        entry.getSelector().setConsole(super.getConsole());
        entry.getSelector().setResultTester(input -> {
            if (this.isCancellable() && input.equalsIgnoreCase("cancel")) {
                this.cancelled = true;
                this.entries.clear();
                return false;
            }
            return true;
        });

        super.getConsole().togglePrinting(false);
        entry.getSelector().init();

        UUID listenerId = UUID.randomUUID();

        super.getConsole().addKeyListener(listenerId, entry.getSelector());

        try {
            entry.getSelector().getCompletionHandler().get();
        } catch (InterruptedException | ExecutionException exception) {
            exception.printStackTrace();
        }

        super.getConsole().removeKeyListener(listenerId);

        if (this.entries.isEmpty()) {
            this.resetConsole();
        }

        this.results.put(entry.getKey(), entry.getSelector().getResult());
        for (BiConsumer<QuestionListEntry<?>, Object> listener : this.entryCompletionListeners) {
            listener.accept(entry, entry.getSelector().getResult());
        }

        CloudNet.getInstance().getEventManager().callEvent(new SetupResponseEvent(this, entry, entry.getSelector().getResult()));

        return false;
    }

    private void resetConsole() {
        if (this.cancelled) {
            super.getConsole().forceWriteLine("&c" + LanguageManager.getMessage("ca-question-list-cancelled"));
            CloudNet.getInstance().getEventManager().callEvent(new SetupCancelledEvent(this));
        } else {
            String footer = this.footerSupplier.get();

            if (footer != null) {
                super.getConsole().forceWriteLine("&r" + footer);
            }

            CloudNet.getInstance().getEventManager().callEvent(new SetupCompleteEvent(this));
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

        super.getConsole().clearScreen();
        if (this.lastCachedMessagesSupplier != null) {
            for (String line : this.lastCachedMessagesSupplier.get()) {
                super.getConsole().forceWriteLine(line);
            }
        }

        super.getConsole().enableAllHandlers();
        super.getConsole().togglePrinting(this.previousPrintingEnabled);
        super.getConsole().setPrompt(this.previousPrompt);
        super.getConsole().setCommandHistory(this.previousHistory);
    }

}
