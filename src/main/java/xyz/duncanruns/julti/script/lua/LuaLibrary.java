package xyz.duncanruns.julti.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public abstract class LuaLibrary extends TwoArgFunction {

    protected final CancelRequester cancelRequester;
    private final String libraryName;

    public LuaLibrary(CancelRequester requester, String libraryName) {
        this.cancelRequester = requester;
        this.libraryName = libraryName;
    }

    private static LibFunction convertToArgFunctionObj(LuaLibrary obj, Method method) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Object[] params = new Object[method.getParameterCount()];
                Class<?>[] parameterTypes = method.getParameterTypes();

                for (int i = 0; i < method.getParameterCount(); i++) {
                    try {
                        params[i] = LuaConverter.convertToJava(args.arg(i + 1), parameterTypes[i]);
                    } catch (Throwable t) {
                        Julti.log(Level.ERROR, "Failed to convert parameter " + i + " (" + parameterTypes[i].getSimpleName() + ") for method \"" + method.getName() + "\": " + ExceptionUtil.toDetailedString(t));
                        throw t;
                    }
                }

                try {
                    return LuaConverter.convertToLua(method.invoke(obj, params), method.getReturnType());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.getName().equals("call") || Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            library.set(method.getName(), convertToArgFunctionObj(this, method));
        }
        env.set(this.libraryName, library);
        return library;
    }
}
