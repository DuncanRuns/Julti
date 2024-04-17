package xyz.duncanruns.julti.script.lua;

import com.google.common.primitives.Primitives;
import org.luaj.vm2.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class LuaConverter {
    private static final Map<Class<?>, Function<Varargs, Object>> luaToJavaMap = generateLuaToJavaMap();
    private static final Map<Class<?>, Function<Object, Varargs>> javaToLuaMap = generateJavaToLuaMap();

    private LuaConverter() {

    }

    private static Map<Class<?>, Function<Varargs, Object>> generateLuaToJavaMap() {
        Map<Class<?>, Function<Varargs, Object>> map = new HashMap<>();
        map.put(int.class, varargs -> ((LuaValue) varargs).checknumber().toint());
        map.put(LuaInteger.class, varargs -> ((LuaValue) varargs).checknumber().toint());
        map.put(float.class, varargs -> ((LuaValue) varargs).checknumber().tofloat());
        map.put(long.class, varargs -> ((LuaValue) varargs).checknumber().tolong());
        map.put(double.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(LuaNumber.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(LuaDouble.class, varargs -> ((LuaValue) varargs).checknumber().todouble());
        map.put(char.class, varargs -> ((LuaValue) varargs).checknumber().tochar());
        map.put(short.class, varargs -> ((LuaValue) varargs).checknumber().toshort());
        map.put(byte.class, varargs -> ((LuaValue) varargs).checknumber().tobyte());
        map.put(boolean.class, varargs -> ((LuaValue) varargs).checkboolean());
        map.put(LuaBoolean.class, varargs -> ((LuaValue) varargs).checkboolean());
        map.put(String.class, varargs -> ((LuaValue) varargs).isnil() ? null : ((LuaValue) varargs).checkjstring());
        map.put(LuaString.class, varargs -> ((LuaValue) varargs).isnil() ? null : ((LuaValue) varargs).checkjstring());
        map.put(LuaValue.class, varargs -> varargs);
        map.put(Varargs.class, varargs -> varargs);
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, varargs -> varargs == LuaValue.NIL ? null : map.get(Primitives.unwrap(clazz)).apply(varargs)));
        return map;
    }

    private static Map<Class<?>, Function<Object, Varargs>> generateJavaToLuaMap() {
        Map<Class<?>, Function<Object, Varargs>> map = new HashMap<>();
        map.put(int.class, o -> LuaValue.valueOf((int) o));
        map.put(LuaInteger.class, o -> LuaValue.valueOf((int) o));
        map.put(float.class, o -> LuaValue.valueOf((float) o));
        map.put(long.class, o -> LuaValue.valueOf((long) o));
        map.put(double.class, o -> LuaValue.valueOf((double) o));
        map.put(LuaNumber.class, o -> LuaValue.valueOf((double) o));
        map.put(LuaDouble.class, o -> LuaValue.valueOf((double) o));
        map.put(char.class, o -> LuaValue.valueOf((char) o));
        map.put(short.class, o -> LuaValue.valueOf((short) o));
        map.put(byte.class, o -> LuaValue.valueOf((byte) o));
        map.put(boolean.class, o -> LuaValue.valueOf((boolean) o));
        map.put(LuaBoolean.class, o -> LuaValue.valueOf((boolean) o));
        map.put(void.class, o -> LuaValue.NIL);
        map.put(String.class, o -> LuaValue.valueOf((String) o));
        map.put(LuaString.class, o -> LuaValue.valueOf((String) o));
        map.put(LuaValue.class, o -> (LuaValue) o);
        map.put(Varargs.class, o -> (Varargs) o);
        Primitives.allWrapperTypes().forEach(clazz -> map.put(clazz, o -> o == null ? LuaValue.NIL : map.get(Primitives.unwrap(clazz)).apply(o)));
        return map;
    }

    public static Varargs convertToLua(Object value) {
        return javaToLuaMap.get(value.getClass()).apply(value);
    }

    public static Varargs convertToLua(Object value, Class<?> conversionClass) {
        return javaToLuaMap.get(conversionClass).apply(value);
    }

    public static Object convertToJava(Varargs value) {
        return luaToJavaMap.get(value.getClass()).apply(value);
    }

    public static <T> Object convertToJava(Varargs value, Class<?> conversionClass) {
        return luaToJavaMap.get(conversionClass).apply(value);
    }
}
