package com.github.sgdan.webviewredux

import netscape.javascript.JSObject
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.safeCast

/**
 * Represents a named action with optional arguments
 */
data class Action(
        /** The name of this action, optionally matching an enum */
        val name: String,

        /** Optional arguments */
        val args: List<Any?> = emptyList()
) {
    companion object {
        /** Create from JavaScript object, assume first argument is name */
        fun from(jso: JSObject): Action {
            val len = jso.getMember("length")
            if (len !is Int || len == 0) throw Exception("Arguments needed")
            val name = jso.getSlot(0).toString()
            val args = 1.until(len).map { jso.getSlot(it) }
            return Action(name, args)
        }
    }

    /** Vararg constructor alternative */
    constructor(name: Any, vararg args: Any?) : this(name.toString(), args.toList())

    /** @return the ith argument or null if there isn't one */
    fun get(i: Int) = args.getOrNull(i)

    /** @return the ith argument as the given type or null if not possible */
    fun <T> get(i: Int, c: Class<T>): T? = get(i)?.let {
        if (c.isInstance(it)) c.cast(it) else null
    }

    /** @return the ith argument as the given type or null if not possible */
    fun <T : Any> get(i: Int, k: KClass<T>): T? = k.safeCast(get(i))

    /**
     * Convert action name to Enum
     */
    inline fun <reified T : kotlin.Enum<T>> to(): T? = try {
        java.lang.Enum.valueOf(T::class.java, name)
    } catch (e: Exception) {
        null
    }

    /**
     * Convenience function to call the provided method which is assumed to
     * take the initial argument (usually the state) followed by the parameters
     * of this action.
     */
    inline fun <reified R> call(initial: Any? = null, fn: KFunction<R>): R = try {
        fn.call(initial, *args.toTypedArray())
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Call failed using $this", e)
    }
}