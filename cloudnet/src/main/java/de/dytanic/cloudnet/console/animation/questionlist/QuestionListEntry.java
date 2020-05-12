package de.dytanic.cloudnet.console.animation.questionlist;

import de.dytanic.cloudnet.console.animation.questionlist.selector.QuestionListEntrySelector;

public class QuestionListEntry<T> {
    private final String key;
    private final String question;
    private final QuestionAnswerType<T> answerType;
    private final QuestionListEntrySelector selector;

    public QuestionListEntry(String key, String question, QuestionAnswerType<T> answerType) {
        this.key = key;
        this.question = question;
        this.answerType = answerType;
        this.selector = QuestionListEntrySelector.create(this);
    }

    public String getKey() {
        return this.key;
    }

    public String getQuestion() {
        return this.question;
    }

    public QuestionAnswerType<T> getAnswerType() {
        return this.answerType;
    }

    public QuestionListEntrySelector getSelector() {
        return this.selector;
    }
}
