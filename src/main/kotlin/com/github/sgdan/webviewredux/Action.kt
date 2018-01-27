package com.github.sgdan.webviewredux

import netscape.javascript.JSObject

/**
 * Represents a named action with optional parameter of any type.
 */
class Action(
        /** The name of this action, optionally matching an enum */
        val name: String,

        /** An argument of any type, use list or array for multiple action parameters */
        val arg: Any? = null
) {
    companion object {
        /** Create using enum value */
        fun <E : Enum<E>> from(value: E, arg: Any? = null) = Action(value.name, arg)

        /** Create from JavaScript object, assume first argument is name */
        fun from(jso: JSObject): Action {
            val len = jso.getMember("length")
            if (len !is Int || len == 0) throw Exception("Arguments needed")
            val name = jso.getSlot(0).toString()
            val params = 1.until(len).map { jso.getSlot(it) }.toTypedArray()
            return Action(name, params)
        }
    }

    /** Allow easy access to parameters */
    val params: List<Any?> = when (arg) {
        null -> emptyList()
        is Array<*> -> arg.toList()
        is List<*> -> arg
        else -> emptyList()
    }

    /** @return the argument as the given class or null if it's the wrong type */
    fun <T> arg(c: Class<T>): T? = convert(arg, c)

    /** @return the ith parameter or null if there isn't one */
    fun get(i: Int) = params.getOrNull(i)

    /** @return the ith parameter as the given type or null if not possible */
    fun <T> get(i: Int, c: Class<T>): T? = convert(get(i), c)

    private fun <T> convert(obj: Any?, c: Class<T>): T? = when {
        obj == null -> null
        obj::class.java == c -> c.cast(obj)
        else -> null
    }

    /**
     * Convert action name to Enum,
     */
    inline fun <reified T : kotlin.Enum<T>> to(): T? = try {
        java.lang.Enum.valueOf(T::class.java, name)
    } catch (e: Exception) {
        null
    }
}