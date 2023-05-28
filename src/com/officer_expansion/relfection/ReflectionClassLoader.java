package com.officer_expansion.relfection;

import java.net.URL;
import java.net.URLClassLoader;

public class ReflectionClassLoader extends URLClassLoader
{
    static {
        ClassLoader.registerAsParallelCapable();
    }

    public ReflectionClassLoader(URL[] urls, ClassLoader parent) {
    super(urls, parent);
}
}