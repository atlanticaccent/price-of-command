package com.price_of_command.reflection

import java.net.URL
import java.net.URLClassLoader


class ReflectionLoader(url: URL, parent: ClassLoader?) :
    URLClassLoader(arrayOf<URL>(url), parent) {
    companion object {
        const val PACKAGE = "com.price_of_command"
    }

    @Throws(ClassNotFoundException::class)
    override fun loadClass(name: String): Class<*> {
        return if (name.startsWith("java.lang.reflect")) {
            getSystemClassLoader().loadClass(name)
        } else super.loadClass(name)
    }

    @Throws(ClassNotFoundException::class)
    public override fun loadClass(name: String, resolve: Boolean): Class<*> {
        val c = findLoadedClass(name)
        if (c != null) {
            return c
        }
        // Be the defining classloader for all classes in the reflection whitelist
        // For classes defined by this loader, classes in java.lang.reflect will be loaded directly
        // by the system classloader, without the intermediate delegations.
        if (name.startsWith(PACKAGE)) {
            return findClass(name)
        }
        return super.loadClass(name, resolve)
    }
}
