package me.andreasmelone.compiledbook.classloader;

import java.util.HashSet;
import java.util.Set;

public final class BookClassLoader extends ClassLoader {
    private final Set<String> definedClassNames = new HashSet<>();
    private final Set<Class<?>> definedClasses = new HashSet<>();
    private final String name;
    public BookClassLoader(String name, ClassLoader parent) {
        super(parent);
        this.name = name;
    }

    public Class<?> defineClass(String className, byte[] bytecode) {
        if(definedClassNames.contains(className)) throw new IllegalArgumentException("Cannot register a second class with name " + className);

        Class<?> definedClass = this.defineClass(className, bytecode, 0, bytecode.length);
        definedClassNames.add(className);
        definedClasses.add(definedClass);
        return definedClass;
    }

    @SuppressWarnings("unchecked")
    public <T> Set<Class<? extends T>> getDefinedSubtypeClasses(Class<T> clazz) {
        Set<Class<? extends T>> classes = new HashSet<>();
        for (Class<?> definedClass : definedClasses) {
            if(isSubclassOf(definedClass, clazz)) classes.add((Class<? extends T>) definedClass);
        }
        return classes;
    }

    private static boolean isSubclassOf(Class<?> clazz, Class<?> target) {
        if (clazz == null) {
            return false;
        }

        if (clazz.equals(target)) {
            return true;
        }

        return isSubclassOf(clazz.getSuperclass(), target);
    }

    @Override
    public String toString() {
        return "BookClassLoader{" +
                "parent=" + getParent().toString() +
                ", definedClasses=" + definedClasses +
                ", name='" + name + '\'' +
                '}';
    }
}
