package xyz.duncanruns.julti.script.lua;

import org.luaj.vm2.LuaValue;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.script.LuaScript;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Optional;

class CustomizingJultiLuaLibrary extends JultiLuaLibrary {
    private boolean checkedCustomizing = false;

    CustomizingJultiLuaLibrary(CancelRequester requester, LuaScript luaScript) {
        super(requester, luaScript);
    }


    @NotALuaFunction
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        addMethodsToLibrary(library, this, JultiLuaLibrary.class);
        addMethodsToLibrary(library, this);
        env.set(this.getLibraryName(), library);
        return library;
    }

    @Override
    @AllowedWhileCustomizing
    public void log(String message) {
        if (this.hasCheckedCustomizing()) {
            super.log(message);
        }
    }

    @Override
    @AllowedWhileCustomizing
    public boolean isCustomizing() {
        this.checkedCustomizing = true;
        return true;
    }

    @Override
    @AllowedWhileCustomizing
    @Nullable
    public String askTextBox(String message, String startingVal) {
        Object o = JOptionPane.showInputDialog(JultiGUI.getJultiGUI().getControlPanel().openScriptsGUI(), message, "Julti Script: " + this.script.getName(), JOptionPane.PLAIN_MESSAGE, null, null, Optional.ofNullable(startingVal).orElse(""));
        return o == null ? null : o.toString();
    }

    @Override
    @AllowedWhileCustomizing
    @Nullable
    public Boolean askYesNo(String message) {
        int ans = JOptionPane.showConfirmDialog(JultiGUI.getJultiGUI().getControlPanel().openScriptsGUI(), message, "Julti Script: " + this.script.getName(), JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null);
        switch (ans) {
            case 0:
                return true;
            case 1:
                return false;
            default:
                return null;
        }
    }

    @NotALuaFunction
    public boolean hasCheckedCustomizing() {
        return this.checkedCustomizing;
    }
}
