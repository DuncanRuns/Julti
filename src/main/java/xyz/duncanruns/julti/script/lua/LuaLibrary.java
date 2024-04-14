package xyz.duncanruns.julti.script.lua;

import com.google.common.primitives.Primitives;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import xyz.duncanruns.julti.cancelrequester.CancelRequester;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public abstract class LuaLibrary extends TwoArgFunction {
    public static final Map<Class<?>, Function<Varargs, Object>> luaToJavaMap = generateLuaToJavaMap();
    public static final Map<Class<?>, Function<Object, Varargs>> javaToLuaMap = generateJavaToLuaMap();

    protected final CancelRequester cancelRequester;
    private final String libraryName;

    public LuaLibrary(CancelRequester requester, String libraryName) {
        this.cancelRequester = requester;
        this.libraryName = libraryName;
    }

    private static Map<Class<?>, Function<Varargs, Object>> generateLuaToJavaMap() {
        Map<Class<?>, Function<Varargs, Object>> map = new HashMap<>();
        map.put(int.class, varargs -> ((LuaValue) varargs).checknumber().toint());
        map.put(float.class, varargs -> ((LuaValue) varargs).checknumber().tofloat());
        map.put(long.class, varargs -> ((LuaValue) varargs).checknumber().tolong());
        map.put(double.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(char.class, varargs -> ((LuaValue) varargs).checknumber().tochar());
        map.put(short.class, varargs -> ((LuaValue) varargs).checknumber().toshort());
        map.put(byte.class, varargs -> ((LuaValue) varargs).checknumber().tobyte());
        map.put(boolean.class, varargs -> ((LuaValue) varargs).checknumber().checkboolean());
        map.put(String.class, varargs -> ((LuaValue) varargs).isnil() ? null : ((LuaValue) varargs).checkjstring());
        map.put(LuaValue.class, varargs -> varargs);
        map.put(Varargs.class, varargs -> varargs);
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, varargs -> varargs == NIL ? null : map.get(Primitives.unwrap(clazz)).apply(varargs)));
        return map;
    }

    private static Map<Class<?>, Function<Object, Varargs>> generateJavaToLuaMap() {
        Map<Class<?>, Function<Object, Varargs>> map = new HashMap<>();
        map.put(int.class, o -> valueOf((int) o));
        map.put(float.class, o -> valueOf((float) o));
        map.put(long.class, o -> valueOf((long) o));
        map.put(double.class, o -> valueOf((double) o));
        map.put(char.class, o -> valueOf((char) o));
        map.put(short.class, o -> valueOf((short) o));
        map.put(byte.class, o -> valueOf((byte) o));
        map.put(boolean.class, o -> valueOf((boolean) o));
        map.put(void.class, o -> NIL);
        map.put(String.class, o -> valueOf((String) o));
        map.put(LuaValue.class, o -> (LuaValue) o);
        map.put(Varargs.class, o -> (Varargs) o);
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, o -> o == null ? NIL : map.get(Primitives.unwrap(clazz)).apply(o)));
        return map;
    }

    private static LibFunction convertToArgFunctionObj(LuaLibrary obj, Method method) {
        return new VarArgFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Object[] params = new Object[method.getParameterCount()];
                Class<?>[] parameterTypes = method.getParameterTypes();

                for (int i = 0; i < method.getParameterCount(); i++) {
                    params[i] = luaToJavaMap.get(parameterTypes[i]).apply(args.arg(i + 1));
                }

                try {
                    return javaToLuaMap.get(method.getReturnType()).apply(method.invoke(obj, params));
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
