package xyz.duncanruns.julti.hotkey;

import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.script.ScriptHotkeyData;
import xyz.duncanruns.julti.util.MouseUtil;

import java.util.concurrent.CopyOnWriteArrayList;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class HotkeyManager {
    private static final HotkeyManager INSTANCE = new HotkeyManager();
    public final CopyOnWriteArrayList<Pair<Hotkey, String>> hotkeys = new CopyOnWriteArrayList<>(); // This lets us run the hotkey checker without ever having to stop it

    private HotkeyManager() {
    }

    public static HotkeyManager getInstance() {
        return INSTANCE;
    }

    public void start() {
        new Thread(this::run, "hotkey-checker").start();
    }

    public void reloadHotkeys() {
        this.hotkeys.clear();
        JultiOptions options = JultiOptions.getInstance();

        this.addHotkey(Hotkey.of(options.resetHotkey, options.resetHotkeyIM), "reset");
        this.addHotkey(Hotkey.of(options.bgResetHotkey, options.bgResetHotkeyIM), "bgReset");
        this.addHotkey(Hotkey.of(options.wallResetHotkey, options.wallResetHotkeyIM), "wallReset");
        this.addHotkey(Hotkey.of(options.wallSingleResetHotkey, options.wallSingleResetHotkeyIM), "wallSingleReset");
        this.addHotkey(Hotkey.of(options.wallLockHotkey, options.wallLockHotkeyIM), "wallLock");
        this.addHotkey(Hotkey.of(options.wallPlayHotkey, options.wallPlayHotkeyIM), "wallPlay");
        this.addHotkey(Hotkey.of(options.wallFocusResetHotkey, options.wallFocusResetHotkeyIM), "wallFocusReset");
        this.addHotkey(Hotkey.of(options.cancelScriptHotkey, options.cancelScriptHotkeyIM), "cancelScript");
        this.addHotkey(Hotkey.of(options.wallPlayLockHotkey, options.wallPlayLockHotkeyIM), "wallPlayLock");

        options.scriptHotkeys.forEach(s -> {
            ScriptHotkeyData data = ScriptHotkeyData.parseString(s);
            if (data == null || data.scriptName.isEmpty()) {
                return;
            }
            this.addHotkey(Hotkey.of(data.keys, data.ignoreModifiers), "script:" + data.scriptName);
        });
    }

    private void addHotkey(Hotkey hotkey, String action) {
        if (hotkey.isEmpty()) {
            return;
        }
        this.hotkeys.add(Pair.of(hotkey, action));
    }

    private void run() {
        while (Julti.getInstance().isRunning()) {
            sleep(1);
            for (Pair<Hotkey, String> hotkeyAction : this.hotkeys) {
                if (hotkeyAction.getLeft().wasPressed()) {
                    Julti.getInstance().queueMessage(new HotkeyPressQMessage(hotkeyAction.getRight(), MouseUtil.getMousePos()));
                }
            }
        }
    }
}