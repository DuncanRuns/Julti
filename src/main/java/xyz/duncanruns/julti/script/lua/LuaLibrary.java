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

import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers()) || method.isAnnotationPresent(NotALuaFunction.class)) {
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
                    if (this.getClass().isAnnotationPresent(AllowedWhileCustomizing.class) || (methodOpt.isPresent() && methodOpt.get().isAnnotationPresent(AllowedWhileCustomizing.class))) {
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

    @NotALuaFunction
    public void writeLuaFile(Writer writer) throws IOException {
        writer.write("-- Java methods defined by the \"" + this.libraryName + "\" library  automatically converted to lua functions for scripting environment usage.");
        writer.write("\n\n" + this.getLibraryName() + " = {}");
        for (Method method : this.getClass().getDeclaredMethods()) {
            if (method.isSynthetic() || Modifier.isStatic(method.getModifiers()) || method.isAnnotationPresent(NotALuaFunction.class)) {
                continue;
            }
            writer.write("\n");

            LuaDocumentation[] documentations = method.getAnnotationsByType(LuaDocumentation.class);
            Optional<LuaDocumentation> documentation = documentations.length == 0 ? Optional.empty() : Optional.of(documentations[0]);
            if (documentation.isPresent()) {
                writer.write("\n--- " + documentation.get().description());
            }

            List<String> paramNames = Arrays.stream(method.getParameters()).map(Parameter::getName).collect(Collectors.toList());
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramNames.size() != paramTypes.length) {
                continue; // method is stupid?
            }
            List<String> paramsForFunction = new ArrayList<>();
            for (int i = 0; i < paramTypes.length; i++) {
                String paramTypeName = "";
                if (documentation.isPresent() && i < documentation.get().paramTypes().length) {
                    paramTypeName = documentation.get().paramTypes()[i];
                } else {
                    paramTypeName = LuaConverter.classToLuaName(paramTypes[i]);
                }
                writer.write(String.format("\n--- @param %s %s", paramNames.get(i), paramTypeName));
                paramsForFunction.add(paramNames.get(i));
            }

            if (documentation.isPresent() && documentation.get().returnTypes().length > 0) {
                for (String returnType : documentation.get().returnTypes()) {
                    writer.write("\n--- @return " + returnType);
                }
            } else {
                writer.write("\n--- @return " + LuaConverter.classToLuaName(method.getReturnType()));
            }

            writer.write(String.format("\nfunction %s.%s(%s) end", this.getLibraryName(), method.getName(), String.join(", ", paramsForFunction)));
        }
    }

    @NotALuaFunction
    public String getLibraryName() {
        return this.libraryName;
    }

    /**
     * Mark a method with @AllowedWhileCustomizing to allow it to still function in customization mode.
     * <p>
     * Alternatively you can annotate the entire class with @AllowedWhileCustomizing to allow all methods
     * in it to function in customization mode (not recommended).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface AllowedWhileCustomizing {
    }

    /**
     * Mark a method with @NotALuaFunction to exclude it from automatic conversion to a lua library.
     * Static methods and synthetic (lambda) methods are already excluded.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface NotALuaFunction {
    }

    /**
     * Mark a method with @LuaDocumentation to give a documentation description that will appear in automatically generated lua.
     * Optionally you can define the return type(s) in cases where the java method returns "LuaValue" or "Varargs"
     * You can also define parameter types in a similar case for parameters.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @interface LuaDocumentation {
        String description();

        String[] returnTypes() default {};

        String[] paramTypes() default {};
    }
}
