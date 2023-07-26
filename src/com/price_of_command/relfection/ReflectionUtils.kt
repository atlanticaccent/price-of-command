@file:Suppress("UsePropertyAccessSyntax")

package com.price_of_command.relfection

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

internal object ReflectionUtils {
    private val fieldClass = Class.forName("java.lang.reflect.Field", false, Class::class.java.classLoader)
    private val setFieldHandle = MethodHandles.lookup()
        .findVirtual(fieldClass, "set", MethodType.methodType(Void.TYPE, Any::class.java, Any::class.java))
    private val getFieldHandle =
        MethodHandles.lookup().findVirtual(fieldClass, "get", MethodType.methodType(Any::class.java, Any::class.java))
    private val getFieldNameHandle =
        MethodHandles.lookup().findVirtual(fieldClass, "getName", MethodType.methodType(String::class.java))
    private val setFieldAccessibleHandle = MethodHandles.lookup()
        .findVirtual(fieldClass, "setAccessible", MethodType.methodType(Void.TYPE, Boolean::class.javaPrimitiveType))

    private val methodClass = Class.forName("java.lang.reflect.Method", false, Class::class.java.classLoader)
    internal val getMethodNameHandle =
        MethodHandles.lookup().findVirtual(methodClass, "getName", MethodType.methodType(String::class.java))
    internal val getMethodReturnHandle =
        MethodHandles.lookup().findVirtual(methodClass, "getReturnType", MethodType.methodType(Class::class.java))
    private val invokeMethodHandle = MethodHandles.lookup().findVirtual(
        methodClass, "invoke", MethodType.methodType(Any::class.java, Any::class.java, Array<Any>::class.java)
    )

    fun set(fieldName: String, instanceToModify: Any, newValue: Any?) {
        var field: Any? = null

        try {
            field = instanceToModify.javaClass.getField(fieldName)
        } catch (_: NoSuchFieldError) {
        }
        if (field == null) {
            field = instanceToModify.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        setFieldHandle.invoke(field, instanceToModify, newValue)
    }

    fun get(fieldName: String, instanceToGetFrom: Any): Any? {
        var field: Any? = null

        try {
            field = instanceToGetFrom.javaClass.getField(fieldName)
        } catch (_: NoSuchFieldError) {
        }
        if (field == null) {
            field = instanceToGetFrom.javaClass.getDeclaredField(fieldName)
        }

        setFieldAccessibleHandle.invoke(field, true)
        return getFieldHandle.invoke(field, instanceToGetFrom)
    }

    fun hasMethodOfName(name: String, instance: Any, contains: Boolean = false): Boolean {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

        return if (!contains) {
            instancesOfMethods.any { getMethodNameHandle.invoke(it) == name }
        } else {
            instancesOfMethods.any { (getMethodNameHandle.invoke(it) as String).contains(name) }
        }
    }

    inline fun <reified T> getMethodOfReturnType(instance: Any): String? {
        val instancesOfMethods: Array<out Any> = instance.javaClass.getDeclaredMethods()

        return instancesOfMethods.firstOrNull { getMethodReturnHandle.invoke(it) == T::class.java }
            ?.let { getMethodNameHandle.invoke(it) as String }
    }

    fun hasVariableOfName(name: String, instance: Any): Boolean {
        val instancesOfFields: Array<out Any> = instance.javaClass.getDeclaredFields()
        return instancesOfFields.any { getFieldNameHandle.invoke(it) == name }
    }

    fun instantiate(clazz: Class<*>, vararg arguments: Any?): Any? {
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it!!::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        val constructorHandle = MethodHandles.lookup().findConstructor(clazz, methodType)

        return constructorHandle.invokeWithArguments(arguments.toList())
    }

    fun invoke(methodName: String, instance: Any, vararg arguments: Any?, declared: Boolean = false): Any? {
        val method: Any?

        val clazz = instance.javaClass
        val args = arguments.map { it!!::class.javaPrimitiveType ?: it::class.java }
        val methodType = MethodType.methodType(Void.TYPE, args)

        method = if (!declared) {
            clazz.getMethod(methodName, *methodType.parameterArray())
        } else {
            clazz.getDeclaredMethod(methodName, *methodType.parameterArray())
        }

        return invokeMethodHandle.invoke(method, instance, arguments)
    }
}