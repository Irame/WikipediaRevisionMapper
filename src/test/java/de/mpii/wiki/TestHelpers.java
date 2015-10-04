package de.mpii.wiki;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked")
public abstract class TestHelpers {
    public static <T> T invokePrivateMethod(Object targetObject, String methodName, Object... argObjects)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] argClasses = new Class[argObjects.length];
        for (int i = 0; i < argObjects.length; i++) {
            argClasses[i] = argObjects[i].getClass();
        }
        Method method = targetObject.getClass().getDeclaredMethod(methodName, argClasses);
        method.setAccessible(true);
        return (T)method.invoke(targetObject, argObjects);
    }

    public static <T> T invokePrivateStaticMethod(Class<?> targetClass, String methodName, Object... argObjects)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Class<?>[] argClasses = new Class[argObjects.length];
        for (int i = 0; i < argObjects.length; i++) {
            argClasses[i] = argObjects[i].getClass();
        }
        Method method = targetClass.getDeclaredMethod(methodName, argClasses);
        method.setAccessible(true);
        return (T)method.invoke(null, argObjects);
    }
}
