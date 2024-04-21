package xyz.duncanruns.julti.script.lua;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.gui.JultiGUI;
import xyz.duncanruns.julti.script.LuaScript;

import javax.annotation.Nullable;
import javax.swing.*;
import java.lang.reflect.Method;
import java.util.Optional;

public class CustomizingJultiLuaLibrary extends JultiLuaLibrary {
    private boolean checkedCustomizing = false;

    public CustomizingJultiLuaLibrary(CancelRequester requester, LuaScript luaScript) {
        super(requester, luaScript);
    }

    @NotALuaFunction
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = super.call(modname, env);
        for (Method method : JultiLuaLibrary.class.getDeclaredMethods()) {
            if (method.isAnnotationPresent(AllowedWhileCustomizing.class) || method.isAnnotationPresent(NotALuaFunction.class)) {
                continue;
            }
            library.set(method.getName(), new VarArgFunction() {
                @Override
                public Varargs invoke(Varargs args) {
                    throw new CustomizingException("Customization Error: julti." + method.getName() + " used while customizing.");
                }
            });
        }
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
        return JOptionPane.showInputDialog(JultiGUI.getJultiGUI().getControlPanel().openScriptsGUI(), message, "Julti Script: " + this.script.getName(), JOptionPane.PLAIN_MESSAGE, null, null, Optional.ofNullable(startingVal).orElse("")).toString();
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
