package xyz.duncanruns.julti.script.lua;

import org.luaj.vm2.LuaValue;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.script.LuaScript;

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

    @NotALuaFunction
    public boolean hasCheckedCustomizing() {
        return this.checkedCustomizing;
    }
}
