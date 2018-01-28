package com.github.sgdan.webviewredux

import kotlinx.html.*
import kotlinx.html.dom.create
import mu.KotlinLogging
import org.junit.Assert
import org.junit.Test
import org.w3c.dom.Node

private val log = KotlinLogging.logger {}

class NodeReplicatorTest : Assert() {
    private val testDocs = mapOf(
            "page" to createDoc().create.html {
                head {
                    title("Document title")
                }
                body {
                    h1("hclass") { +"a header" }
                    div {
                        p("pclass") { +"some more text" }
                        h2("hclass") { +"a header!" }
                        button { +"a button 1" }
                        button { +"a button 2" }
                        button { +"a button 3" }
                    }
                }
            }.ownerDocument,

            "short" to createDoc().create.html {
                body {
                    button { +"only" }
                }
            }.ownerDocument
    )

    @Test
    fun testCombinations() {
        // test empty->full, full->empty
        testDocs.forEach { k, v ->
            val doc = createDoc()
            testCopy(k to v, k to v, "empty" to doc)
            // doc is now full
            testCopy("empty" to createDoc(), "empty" to createDoc(), k to doc)
            check("Should be empty again", createDoc(), doc)
        }

        // test empty->A->B works and matches B for all A/B combinations
        testDocs.forEach { b, bdoc ->
            testDocs.forEach { a, adoc ->
                val doc = createDoc()
                copy(adoc, adoc, doc) // empty->A
                copy(bdoc, bdoc, doc) // A->B
                check("empty->$a->$b", bdoc, doc)
            }
        }
    }

    private fun testCopy(ref: Pair<String, Node>, src: Pair<String, Node>, dst: Pair<String, Node>) {
        val desc = "[${ref.first}]${src.first}->${dst.first}"
        copy(ref.second, src.second, dst.second)
        check(desc, src.second, dst.second)
    }

    private fun check(desc: String, src: Node, dst: Node) {
        assertTrue("$desc\nExpected:\n${toHtml(src)}\nActual: ${toHtml(dst)}\n", src.isEqualNode(dst))
        log.info { "$desc succeeded" }
    }

    @Test
    fun testAppendSibling() {
        val src = createDoc().create.html {
            body {
                +"some text"
                a("http://address") { +"site" }
            }
        }
        val dst = createDoc().create.html {
            body {
                +"some text"
            }
        }
        copy(null, src, dst)
        check("Should append hyperlink", src, dst)
    }

}