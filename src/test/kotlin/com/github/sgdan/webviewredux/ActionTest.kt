package com.github.sgdan.webviewredux

import mu.KotlinLogging
import org.junit.Assert
import org.junit.Test

private val log = KotlinLogging.logger {}

class ActionTest : Assert() {
    private enum class TestType {
        ONE, TWO, THREE
    }

    @Test
    fun testAction() {
        // check no params
        val action = Action("aname")
        assertEquals(emptyList<Any>(), action.params)
        assertEquals("aname", action.name)
        val one = Action.from(TestType.ONE)
        assertEquals("ONE", one.name)
        assertNull(one.arg)

        // check that some parameters work
        val two = Action("one", arrayOf("a", 2, "c"))
        assertEquals("c", two.get(2))
        assertEquals(2, two.get(1))
        assertEquals(listOf("a", 2, "c"), two.params)

        val three = Action.from(TestType.THREE, "something")
        assertEquals("THREE", three.name)
        assertEquals("something", three.arg)
        assertNull(three.get(0))

        // check that the typed getters convert if possible or return null
        var str: String? = three.arg(String::class.java)
        assertEquals("something", str)
        var arr: Array<String>? = three.arg(Array<String>::class.java)
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