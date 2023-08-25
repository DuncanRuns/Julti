package xyz.duncanruns.julti.hotkey;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.JultiOptions;
import xyz.duncanruns.julti.messages.HotkeyPressQMessage;
import xyz.duncanruns.julti.plugin.PluginEvents;
import xyz.duncanruns.julti.script.ScriptHotkeyData;
import xyz.duncanruns.julti.util.MouseUtil;

import java.util.concurrent.CopyOnWriteArrayList;

import static xyz.duncanruns.julti.util.SleepUtil.sleep;

public final class HotkeyManager {
    private static final HotkeyManager INSTANCE = new HotkeyManager();
    public final CopyOnWriteArrayList<Pair<Hotkey, String>> hotkeys = new CopyOnWriteArrayList<>(); // This lets us run the hotkey checker without ever having to stop it

    private HotkeyManager() {
    }

    public static HotkeyManager getHotkeyManager() {
        return INSTANCE;
    }

    public void start() {
        new Thread(this::run, "hotkey-checker").start();
    }

    public void reloadHotkeys() {
        this.hotkeys.clear();
        JultiOptions options = JultiOptions.getJultiOptions();

        this.addHotkey(Hotkey.of(options.resetHotkey, options.resetHotkeyIM), "reset");
        this.addHotkey(Hotkey.of(options.bgResetHotkey, options.bgResetHotkeyIM), "bgReset");
        this.addHotkey(Hotkey.of(options.wallResetHotkey, options.wallResetHotkeyIM), "wallReset");
        this.addHotkey(Hotkey.of(options.wallSingleResetHotkey, options.wallSingleResetHotkeyIM), "wallSingleReset");
        this.addHotkey(Hotkey.of(options.wallLockHotkey, options.wallLockHotkeyIM), "wallLock");
        this.addHotkey(Hotkey.of(options.wallPlayHotkey, options.wallPlayHotkeyIM), "wallPlay");
        this.addHotkey(Hotkey.of(options.wallFocusResetHotkey, options.wallFocusResetHotkeyIM), "wallFocusReset");
        this.addHotkey(Hotkey.of(options.cancelScriptHotkey, options.cancelScriptHotkeyIM), "cancelScript");
        this.addHotkey(Hotkey.of(options.wallPlayLockHotkey, options.wallPlayLockHotkeyIM), "wallPlayLock");
        this.addHotkey(Hotkey.of(ImmutableList.of(0xA2, 0xA0, 0x44)), "debugHover"); // LCtrl + LShift + D debug hover

        options.scriptHotkeys.forEach(s -> {
            ScriptHotkeyData data = ScriptHotkeyData.parseString(s);
            if (data == null || data.scriptName.isEmpty()) {
                return;
            }
            this.addHotkey(Hotkey.of(data.keys, data.ignoreModifiers), "script:" + data.scriptName);
        });

        PluginEvents.RunnableEventType.HOTKEY_MANAGER_RELOAD.runAll();
    }

    public void addHotkey(Hotkey hotkey, String action) {
        if (hotkey.isEmpty()) {
            return;
        }
        this.hotkeys.add(Pair.of(hotkey, action));
    }

    private void run() {
        while (Julti.getJulti().isRunning()) {
            sleep(1);
            for (Pair<Hotkey, String> hotkeyAction : this.hotkeys) {
                if (hotkeyAction.getLeft().wasPressed()) {
                    Julti.getJulti().queueMessage(new HotkeyPressQMessage(hotkeyAction.getRight(), MouseUtil.getMousePos()));
                }
            }
        }
    }
}