package de.dytanic.cloudnet.console;

public interface KeyListener {

    /**
     * Called when an input is registered from the terminal.
     *
     * @param action the action of the terminal
     * @return the same action, null to cancel or a new action which will be processed to the console
     */
    InputAction handleInput(InputAction action);

    /**
     * Called after {@link #handleInput(InputAction)} is called with {@link InputAction#CHARACTER}
     *
     * @param c the character which will be added to the line
     * @return {@code true} if the input should be cancelled or {@code false} if it should be normally processed
     */
    boolean handleKey(int c);

    enum InputAction {

        ARROW_LEFT, ARROW_RIGHT, ARROW_UP, ARROW_DOWN,
        BACKSPACE, DELETE,
        CHARACTER,
        ENTER,
        TAB_COMPLETE

    }

}
