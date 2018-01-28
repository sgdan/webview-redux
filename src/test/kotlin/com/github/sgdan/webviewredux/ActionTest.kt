package com.github.sgdan.webviewredux

import mu.KotlinLogging
import org.junit.Assert
import org.junit.Test

private val log = KotlinLogging.logger {}

class ActionTest : Assert() {
    private enum class TestType {
        ONE, TWO, THREE
    }

    fun testFn(initial: String, intArg: Int, strArg: String) = "$initial-$intArg-$strArg"

    @Test
    fun testFunctionCall() {
        val action = Action("TestAction", 4, "some arg")
        assertEquals("init-4-some arg", action.call("init", ::testFn))

        val mismatch = Action("MoreArgs", 7, "arg", "another")
        try {
            mismatch.call("init", ::testFn)
            fail("Should throw exception")
        } catch (e: IllegalArgumentException) {
            log.debug { "Expected error: ${e.message}" }
        }
    }

    @Test
    fun testAction() {
        // check no args
        val action = Action("aname")
        assertTrue(action.args.isEmpty())
        assertEquals("aname", action.name)

        // no args from enum
        val one = Action(TestType.ONE)
        assertEquals("ONE", one.name)
        assertTrue(one.args.isEmpty())

        // try some args
        val two = Action("one", "a", 2, "c")
        assertEquals("c", two.get(2))
        assertEquals(2, two.get(1))
        assertEquals(listOf("a", 2, "c"), two.args)

        // enum with arg
        val three = Action(TestType.THREE, "something")
        assertEquals("THREE", three.name)
        assertEquals("something", three.get(0))
        assertNull(three.get(1)) // null for args that don't exist

        // check that the typed getters convert if possible or return null
        var str: String? = three.get(0, String::class.java)
        assertEquals("something", str)
        var arr: Array<String>? = three.get(0, Array<String>::class.java)
        assertNull(arr)
        var int: Integer? = two.get(1, Integer::class.java)
        assertEquals(2, int)
        str = two.get(1, String::class.java)
        assertNull(str)

        // check conversion to enum value works
        assertEquals(TestType.THREE, three.to<TestType>())
        assertNull(action.to<TestType>())
    }

}