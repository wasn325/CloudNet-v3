package de.dytanic.cloudnet.console.animation.questionlist.selector;

import de.dytanic.cloudnet.common.language.LanguageManager;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;
import org.fusesource.jansi.Ansi;

import java.util.UUID;

public class QuestionListEntryTextInputSelector extends QuestionListEntrySelector {
    public QuestionListEntryTextInputSelector(QuestionListEntry<?> entry) {
        super(entry);
    }

    @Override
    public void init() {
        super.console.setCommandHistory(super.entry.getAnswerType().getCompletableAnswers());

        String recommendation = super.entry.getAnswerType().getRecommendation();
        if (recommendation != null) {
            super.console.setCommandInputValue(recommendation);
        }

        String possibleAnswers = super.entry.getAnswerType().getPossibleAnswersAsString();
        if (possibleAnswers != null) {
            for (String line : this.updateCursor("&r" + entry.getQuestion()
                    + " &r> &e" + LanguageManager.getMessage("ca-question-list-possible-answers-list").replace("%values%", possibleAnswers))) {
                super.console.forceWriteLine("&e" + line);
            }
        } else {
            for (String line : this.updateCursor("&r" + entry.getQuestion())) {
                super.console.forceWriteLine(line);
            }
        }

        UUID handlerId = UUID.randomUUID();

        super.getCompletionHandler().onComplete(o -> super.console.removeCommandHandler(handlerId));

        super.console.addCommandHandler(handlerId, super::tryFinish);
    }

    @Override
    public void drawWrongInput(String input) {
        try {
            super.eraseLastLine(); //erase prompt

            String[] lines = super.entry.getAnswerType().getInvalidInputMessage(input).split(System.lineSeparator());
            for (String line : lines) {
                super.console.forceWriteLine("&c" + line);
            }

            Thread.sleep(3000);

            super.console.writeRaw(this.eraseLines(Ansi.ansi().reset(), lines.length).toString()); //erase invalid input message

            super.console.setCommandHistory(super.entry.getAnswerType().getCompletableAnswers());
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public InputAction handleInput(InputAction action) {
        return action == InputAction.TAB_COMPLETE ? InputAction.ARROW_UP : action;
    }
}
