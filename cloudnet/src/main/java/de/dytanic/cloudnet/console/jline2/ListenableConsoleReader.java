package de.dytanic.cloudnet.console.jline2;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import de.dytanic.cloudnet.console.KeyListener;
import jline.console.ConsoleReader;
import jline.console.KeyMap;
import jline.console.Operation;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ListenableConsoleReader extends ConsoleReader {

    private static final BiMap<Operation, KeyListener.InputAction> ACTION_MAP = HashBiMap.create();

    static {
        ACTION_MAP.put(Operation.PREVIOUS_HISTORY, KeyListener.InputAction.ARROW_UP);
        ACTION_MAP.put(Operation.NEXT_HISTORY, KeyListener.InputAction.ARROW_DOWN);
        ACTION_MAP.put(Operation.BACKWARD_CHAR, KeyListener.InputAction.ARROW_LEFT);
        ACTION_MAP.put(Operation.FORWARD_CHAR, KeyListener.InputAction.ARROW_RIGHT);
        ACTION_MAP.put(Operation.BACKWARD_DELETE_CHAR, KeyListener.InputAction.BACKSPACE);
        ACTION_MAP.put(Operation.DELETE_CHAR, KeyListener.InputAction.DELETE);
        ACTION_MAP.put(Operation.SELF_INSERT, KeyListener.InputAction.CHARACTER);
        ACTION_MAP.put(Operation.ACCEPT_LINE, KeyListener.InputAction.ENTER);
        ACTION_MAP.put(Operation.COMPLETE, KeyListener.InputAction.TAB_COMPLETE);
    }

    private Map<UUID, KeyListener> listeners = new HashMap<>();

    private StringBuilder opBuffer;

    public ListenableConsoleReader() throws IOException {
        super();

        try {
            Field field = ConsoleReader.class.getDeclaredField("opBuffer");
            field.setAccessible(true);
            this.opBuffer = (StringBuilder) field.get(this);
        } catch (NoSuchFieldException | IllegalAccessException exception) {
            exception.printStackTrace();
            this.opBuffer = null;
        }
    }

    public Map<UUID, KeyListener> getListeners() {
        return this.listeners;
    }

    @Override
    public Object readBinding(KeyMap keys) throws IOException {
        if (this.listeners.isEmpty()) {
            return super.readBinding(keys);
        }

        Object binding = super.readBinding(keys);

        if (binding instanceof Operation) {
            KeyListener.InputAction action = ACTION_MAP.get(binding);

            if (action == null) {
                return binding;
            }

            int c = -1;
            if (action == KeyListener.InputAction.CHARACTER && this.opBuffer != null) {
                c = opBuffer.codePointBefore(opBuffer.length());
            }

            KeyListener.InputAction newAction = action;

            for (KeyListener listener : this.listeners.values()) {
                newAction = listener.handleInput(action);
                if (c != -1) {
                    newAction = listener.handleKey(c) ? null : action;
                }
            }

            if (newAction == null) {
                return null;
            }

            return newAction == action ? binding : ACTION_MAP.inverse().get(newAction);

        }

        return binding;
    }
}
