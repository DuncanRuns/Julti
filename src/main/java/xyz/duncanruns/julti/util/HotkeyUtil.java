package xyz.duncanruns.julti.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class HotkeyUtil {
    private static final List<SingleHotkeyChecker> GLOBAL_HOTKEYS = Collections.synchronizedList(new ArrayList<>());
    private static ScheduledExecutorService GLOBAL_HOTKEY_EXECUTOR = null;

    private HotkeyUtil() {
    }

    /**
     * This method is <b>non-blocking</b>.
     * Starts a background task which waits for a hotkey to be pressed. Once a hotkey is pressed, it will be accepted
     * by the hotkeyConsumer. The background task will consistently check the shouldContinueFunction, it is important
     * that the method does not take long to process.
     *
     * @param shouldContinueFunction a method which is checked in the background task loop to determine if the task should continue
     * @param hotkeyConsumer         a method which should accept a hotkey once found
     */
    public static void onNextHotkey(BooleanSupplier shouldContinueFunction, Consumer<Hotkey> hotkeyConsumer) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean done = new AtomicBoolean(false);
        executor.scheduleWithFixedDelay(() -> {
            if (done.get()) {
                return;
            }
            if (!shouldContinueFunction.getAsBoolean()) {
                executor.shutdown();
                done.set(true);
                return;
            }
            List<Integer> keyList = KeyboardUtil.getPressedKeyWithMods();
            if (!keyList.isEmpty()) {
                Hotkey hotkey = new Hotkey(keyList);
                hotkeyConsumer.accept(hotkey);
                executor.shutdown();
            }
        }, 25, 25, TimeUnit.MILLISECONDS);

    }

    /**
     * Add a new global hotkey binding that the global hotkey checker will use. This takes effect immediately.
     *
     * @param hotkey   the hotkey to be checked
     * @param runnable the method to be run when the hotkey is pressed
     */
    public static void addGlobalHotkey(Hotkey hotkey, Runnable runnable) {
        GLOBAL_HOTKEYS.add(new SingleHotkeyChecker(hotkey, runnable));

    }

    /**
     * Clears all stored hotkeys that the global hotkey checker will use. This takes effect immediately.
     */
    public static void clearGlobalHotkeys() {
        GLOBAL_HOTKEYS.clear();
    }

    /**
     * Starts the global hotkey checker. Will also stop one that may still exist.
     */
    public static void startGlobalHotkeyChecker() {
        stopGlobalHotkeyChecker();
        GLOBAL_HOTKEY_EXECUTOR = Executors.newSingleThreadScheduledExecutor();
        GLOBAL_HOTKEY_EXECUTOR.scheduleWithFixedDelay(() -> {
            // Make a copy of checker list with ArrayList constructor and run check() on each of them.
            ((List<SingleHotkeyChecker>) new ArrayList<>(GLOBAL_HOTKEYS)).forEach(SingleHotkeyChecker::check);
        }, 25, 25, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the global hotkey checker if it exists.
     *
     * @return true if a global hotkey checker existed and was closed
     */
    public static boolean stopGlobalHotkeyChecker() {
        if (GLOBAL_HOTKEY_EXECUTOR != null && !GLOBAL_HOTKEY_EXECUTOR.isShutdown()) {
            GLOBAL_HOTKEY_EXECUTOR.shutdown();
            try {
                GLOBAL_HOTKEY_EXECUTOR.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ignored) {
            }
            GLOBAL_HOTKEY_EXECUTOR = null;
            return true;
        }
        return false;
    }

    public static String formatKeys(List<Integer> keys) {
        return new Hotkey(keys).toString();
    }

    private static class SingleHotkeyChecker {
        private final Hotkey hotkey;
        private final Runnable runnable;

        private SingleHotkeyChecker(Hotkey hotkey, Runnable runnable) {
            this.hotkey = hotkey;
            this.runnable = runnable;
        }

        public void check() {
            if (hotkey.wasPressed()) {
                Thread thread = new Thread(runnable);
                thread.setName("hotkeys");
                thread.start();
            }
        }
    }

    public static class Hotkey {

        protected final List<Integer> keys;
        private boolean hasBeenPressed;

        // Should only be used in extended classes
        private Hotkey() {
            keys = null;
            hasBeenPressed = true;
        }

        public Hotkey(List<Integer> keys) {
            // Copy the list by wrapping in the ArrayList constructor, and use unmodifiableList to give an unmodifiable view of it.
            // This is the best way to prevent the hotkey from being tampered with, which also keeps it thread-safe.
            this.keys = List.copyOf(keys);
            hasBeenPressed = false;
        }

        /**
         * Returns an unmodifiable list of integers representing the windows virtual-key codes for the hotkey.
         *
         * @return an unmodifiable list of integers representing the windows virtual-key codes for the hotkey
         */
        public List<Integer> getKeys() {
            return keys;
        }

        /**
         * Like {@link #isPressed()} except only returns true at most once per key press.
         * Will only return true if the last key pressed was the main key.
         * Re-pressing modifier keys will not cause the method to return true.
         * The method will need to be called often to ensure intended results.
         *
         * @return true if called consistenly and follows the descriptions
         */
        public boolean wasPressed() {
            if (isPressed()) {
                if (!hasBeenPressed) {
                    hasBeenPressed = true;
                    return true;
                }
            } else {
                hasBeenPressed = isMainKeyPressed();
            }
            return false;
        }

        /**
         * Checks if the specified keys are pressed. If any extra modifier keys are pressed, the check will fail.
         * Extra keys that are not modifiers do not affect the check.
         *
         * @return true if the correct keys are pressed without any extra modifier keys
         */
        public boolean isPressed() {
            // If any keys belonging to the hotkey are not pressed, return false
            for (int vKey : keys) {
                if (!KeyboardUtil.isPressed(vKey)) return false;
            }

            // If any modifier keys that do not belong to the hotkey are pressed, return false
            for (int vKey : KeyboardUtil.SINGLE_MODIFIERS) {
                if (!keys.contains(vKey) && KeyboardUtil.isPressed(vKey)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Returns true if any non-modifier key is pressed. Hotkeys should be constructed with only one non-modifier
         * key for intended behaviour, so this should only be checking a single key.
         *
         * @return true if any non-modifier key is pressed
         */
        public boolean isMainKeyPressed() {
            for (int vKey : keys) {
                if (!KeyboardUtil.ALL_MODIFIERS.contains(vKey) && KeyboardUtil.isPressed(vKey)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Hotkey hotkey = (Hotkey) o;

            return Objects.equals(keys, hotkey.keys);
        }

        @Override
        public String toString() {
            StringBuilder out = new StringBuilder();
            for (int vKey : keys) {
                if (!out.toString().isEmpty()) {
                    out.append(" + ");
                }
                out.append(KeyboardUtil.getKeyName(vKey));
            }
            return out.toString();
        }
    }

    /**
     * Much like the regular Hotkey class, except ignores extra pressed modifier keys.
     */
    public static class HotkeyIM extends Hotkey {

        public HotkeyIM(List<Integer> keys) {
            super(keys);
        }

        @Override
        public boolean isPressed() {
            // If any keys belonging to the hotkey are not pressed, return false
            for (int vKey : keys) {
                if (!KeyboardUtil.isPressed(vKey)) return false;
            }
            return true;
        }
    }
}
