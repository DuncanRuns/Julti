package xyz.duncanruns.julti.script.lua;

import org.apache.logging.log4j.Level;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import xyz.duncanruns.julti.Julti;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;
import xyz.duncanruns.julti.util.ExceptionUtil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Optional;

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

    public static void addMethodsToLibrary(LuaValue library, LuaLibrary libraryObject) {
        for (Method method : libraryObject.getClass().getDeclaredMethods()) {
            if (Modifier.isStatic(method.getModifiers()) || method.isAnnotationPresent(NotALuaFunction.class)) {
                continue;
            }
            library.set(method.getName(), convertToArgFunctionObj(libraryObject, method));
        }
    }

    @NotALuaFunction
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaValue library = tableOf();
        addMethodsToLibrary(library, this);
        env.set(this.libraryName, library);
        return library;
    }

    @NotALuaFunction
    TwoArgFunction asCustomizable() {
        return new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue modname, LuaValue env) {
                LuaTable out = tableOf();
                LuaTable library = LuaLibrary.this.call(modname, env).checktable();
                LuaValue k = LuaValue.NIL;
                while (true) {
                    Varargs n = library.next(k);
                    if ((k = n.arg1()).isnil()) {
                        break;
                    }
                    String funcName = k.checkjstring();
                    Class<? extends LuaLibrary> clazz = LuaLibrary.this.getClass();
                    Optional<Method> methodOpt = Arrays.stream(clazz.getMethods()).filter(method -> method.getName().equals(funcName)).findAny();
                    if (methodOpt.isPresent() && methodOpt.get().isAnnotationPresent(AllowedWhileCustomizing.class)) {
                        out.set(funcName, n.arg(2));
                    } else {
                        out.set(funcName, new VarArgFunction() {
                            @Override
                            public Varargs invoke(Varargs args) {
                                throw new CustomizingException("Customization Error: " + LuaLibrary.this.libraryName + "." + funcName + " used while customizing.");
                            }
                        });
                    }
                }
                env.set(LuaLibrary.this.libraryName, out);
                return out;
            }
        };
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface AllowedWhileCustomizing {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface NotALuaFunction {
    }
}
