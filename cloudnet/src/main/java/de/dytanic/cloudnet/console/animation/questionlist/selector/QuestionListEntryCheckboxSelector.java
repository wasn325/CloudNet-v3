package de.dytanic.cloudnet.console.animation.questionlist.selector;

import de.dytanic.cloudnet.console.IConsole;
import de.dytanic.cloudnet.console.animation.questionlist.QuestionListEntry;

import java.util.Arrays;

public class QuestionListEntryCheckboxSelector extends QuestionListEntrySelector {

    static final int MAX_ENTRIES = 3;

    private int selectedElement = 0; // TODO use 2 dimensions for more entries?
    private String[] elements;

    private String oldPrompt;

    public QuestionListEntryCheckboxSelector(QuestionListEntry<?> entry) {
        super(entry);
        this.elements = entry.getAnswerType().getPossibleAnswers().toArray(new String[0]);
        if (entry.getAnswerType().getRecommendation() != null) {
            this.selectedElement = Arrays.asList(this.elements).indexOf(entry.getAnswerType().getRecommendation());
        }
    }

    @Override
    public void setConsole(IConsole console) {
        super.setConsole(console);
        this.oldPrompt = console.getPrompt();
        console.setPrompt(null);
    }

    @Override
    public void init() {
        StringBuilder elements = new StringBuilder();
        for (int i = 0; i < this.elements.length; i++) {
            elements.append(' ').append(this.elements[i]).append('[').append(this.selectedElement == i ? '*' : ' ').append(']').append("  ");
        }
        super.console.forceWriteLine("&r" + entry.getQuestion() + " &r> &e" + elements);
    }

    public void redraw() {
        //super.console.forceWriteLine(Ansi.ansi().reset().cursorUp(1).eraseLine().toString());
        this.init();
    }

    @Override
    public void drawWrongInput(String input) {
        // wrong input is impossible
    }

    @Override
    public void drawSuccess() {
        super.drawSuccess();
        super.console.setPrompt(this.oldPrompt);
    }

    @Override
    public InputAction handleInput(InputAction action) {
        switch (action) {
            case ENTER:
                super.tryFinish(this.elements[this.selectedElement]);
                return action;
            case ARROW_LEFT:
                if (this.selectedElement == 0) {
                    this.selectedElement = this.elements.length - 1;
                } else {
                    --this.selectedElement;
                }
                this.redraw();
                break;
            case ARROW_RIGHT:
                if (this.selectedElement == this.elements.length - 1) {
                    this.selectedElement = 0;
                } else {
                    ++this.selectedElement;
                }
                this.redraw();
                break;
        }
        return null;
    }

    @Override
    public boolean handleKey(int c) {
        if (c == ' ') {
            super.tryFinish(this.elements[this.selectedElement]);
        }
        return true;
    }

}
