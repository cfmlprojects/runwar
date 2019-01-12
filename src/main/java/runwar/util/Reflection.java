package runwar.util;

import org.xnio.Option;
import org.xnio.OptionMap;
import runwar.logging.RunwarLogger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class Reflection {

    public static Class<?> load(ClassLoader classLoader, String name) {
        try {
            return classLoader.loadClass(name);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    public static Method method(Class<?> clazz, String name) {
        return method(clazz, name, new Class[0]);
    }

    public static Method method(Class<?> clazz, String name, Class<?>... paramTypes) {
        Class<?> searchType = clazz;
        while (!Object.class.equals(searchType) && searchType != null) {
            Method[] methods = (searchType.isInterface() ? searchType.getMethods() : searchType.getDeclaredMethods());
            for (int i = 0; i < methods.length; i++) {
                Method method = methods[i];
                if (name.equals(method.getName()) && Arrays.equals(paramTypes, method.getParameterTypes())) {
                    return method;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

    public static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (Exception ex) {
            handleReflectionException(ex);
        }
        throw new IllegalStateException("Should never get here");
    }

    public static void handleReflectionException(Exception ex) {
        if (ex instanceof NoSuchMethodException) {
            throw new IllegalStateException("Method not found: " + ex.getMessage());
        }
        if (ex instanceof IllegalAccessException) {
            throw new IllegalStateException("Could not access method: " + ex.getMessage());
        }
        if (ex instanceof InvocationTargetException) {
            handleInvocationTargetException((InvocationTargetException) ex);
        }
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        handleUnexpectedException(ex);
    }

    public static void rethrowRuntimeException(Throwable ex) {
        if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
        }
        if (ex instanceof Error) {
            throw (Error) ex;
        }
        handleUnexpectedException(ex);
    }

    public static void handleInvocationTargetException(InvocationTargetException ex) {
        rethrowRuntimeException(ex.getTargetException());
    }

    private static void handleUnexpectedException(Throwable ex) {
        IllegalStateException isex = new IllegalStateException("Unexpected exception thrown");
        isex.initCause(ex);
        throw isex;
    }

    public static void setOptionMapValue(OptionMap.Builder builder, Class optionsClass, String name, String value){
        Field[] fields = optionsClass.getDeclaredFields();
        Option option;
        boolean foundOption = false;
        for (Field f : fields) {
            if (Modifier.isStatic(f.getModifiers()) && name.equals(f.getName())) {
                foundOption = true;
                try {
                    option = (Option) f.get(null);
                    String typename = f.getGenericType().getTypeName();
                    if (typename.contains("String>")) {
                        builder.set(option,value);
                    } else if (typename.contains("Integer>")) {
                        builder.set(option,Integer.valueOf(value));
                    } else if (typename.contains("Boolean>")) {
                        builder.set(option,Boolean.valueOf(value));
                    } else if (typename.contains("Double>")) {
                        builder.set(option,Double.valueOf(value));
                    } else {
                        throw new IllegalArgumentException("Bad type.");
                    }
                } catch (IllegalAccessException e) {
                    RunwarLogger.LOG.error(e);
                }
            }
        }
        if(!foundOption){
            RunwarLogger.LOG.error("No matching " + optionsClass.getName() + " option for:" + name + ':' + value);
        }
    }


}
