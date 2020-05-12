package de.dytanic.cloudnet.console.animation.questionlist.selector;

import de.dytanic.cloudnet.common.concurrent.ITask;
import de.dytanic.cloudnet.common.concurrent.ListenableTask;
import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.KeyListener;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

public abstract class QuestionListEntrySelector implements KeyListener {

    protected final QuestionListEntry<?> entry;
    protected IConsole console;

    private Predicate<String> resultTester;

    protected int currentCursor = 0;

    protected String resultString;
    private Object result;

    private ITask<Object> completionHandler;

    public QuestionListEntrySelector(QuestionListEntry<?> entry) {
        this.entry = entry;
        this.completionHandler = new ListenableTask<>(() -> this.result);
    }

    public void setConsole(IConsole console) {
        this.console = console;
    }

    public String getResultString() {
        return this.resultString;
    }

    public Object getResult() {
        return this.result;
    }

    public void setResultTester(Predicate<String> resultTester) {
        this.resultTester = resultTester;
    }

    public ITask<Object> getCompletionHandler() {
        return this.completionHandler;
    }

    @Override
    public InputAction handleInput(InputAction action) {
        return action;
    }

    @Override
    public boolean handleKey(int c) {
        return false;
    }

    public void init() {
    }

    public void drawSuccess() {
        this.console.writeRaw( //print result message and remove question
                this.eraseLines(Ansi.ansi().reset(), this.currentCursor + 1)
                        .a("&r").a(entry.getQuestion())
                        .a(" &r> &a").a(this.resultString)
                        .a(System.lineSeparator())
                        .toString()
        );
    }

    public void drawWrongInput(String input) {
    }

    public void tryFinish(String result) {
        if (this.resultTester != null && !this.resultTester.test(result)) {
            try {
                this.completionHandler.call();
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return;
        }

        if (!this.entry.getAnswerType().isValidInput(result)) {
            this.drawWrongInput(result);
            return;
        }
        this.result = this.entry.getAnswerType().parse(result);
        this.resultString = result;
        this.drawSuccess();
        try {
            this.completionHandler.call();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    protected String[] updateCursor(String... texts) {
        Collection<String> result = new ArrayList<>(texts.length);
        int length = 0;
        for (String text : texts) {
            for (String line : text.split(System.lineSeparator())) {
                ++length;
                result.add(line);
            }
        }
        this.currentCursor = length;
        return result.toArray(new String[0]);
    }

    protected Ansi eraseLines(Ansi ansi, int count) {
        for (int i = 0; i < count; i++) {
            ansi.cursorUp(1).eraseLine();
        }
        return ansi;
    }

    protected void eraseLastLine() {
        this.console.writeRaw(
                Ansi.ansi()
                        .reset()
                        .cursorUp(1)
                        .eraseLine()
                        .toString()
        );
    }

    public static QuestionListEntrySelector create(QuestionListEntry<?> entry) {
        if (entry.getAnswerType().getPossibleAnswers() != null &&
                entry.getAnswerType().getPossibleAnswers().size() <= QuestionListEntryCheckboxSelector.MAX_ENTRIES) {
            return new QuestionListEntryCheckboxSelector(entry);
        }
        return new QuestionListEntryTextInputSelector(entry);
    }

}
