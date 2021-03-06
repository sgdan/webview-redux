package com.github.sgdan.webviewredux

import javafx.animation.AnimationTimer
import javafx.concurrent.Worker
import javafx.scene.web.WebView
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import netscape.javascript.JSObject
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.Text
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicReference
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlinx.coroutines.experimental.javafx.JavaFx as UI

private val log = KotlinLogging.logger {}

/**
 * Provide a simple redux-like framework for a JavaFX WebView component.
 *
 * The State type S must be defined by calling code and should
 * ideally be an immutable data class. A "virtual dom" is maintained using
 * the Node instance returned by the view function. Unfortunately Node
 * implementations are mutable but here they're treated as if they
 * are immutable.
 *
 * Based on the concepts described at https://redux.js.org/docs/introduction/CoreConcepts.html
 *
 * Most of the code here is for updating the internal DOM of the WebView
 * when the virtual DOM trees change. There are 3 DOM structures for each
 * update:
 * - reference: The virtual DOM that was the source of the last update
 * - src: The virtual DOM that will be used for this update
 * - dst: The actual DOM in the WebView component
 *
 * If dst is missing nodes from src they will be added. Nodes in dst
 * that are not in src will be removed. Attributes and text values that
 * are present in all 3 structures will only be updated in dst if the src
 * value differs from the reference value. This prevents overwriting the
 * user's changes e.g. in text field, or component focus.
 */
class Redux<S>(
        private val webview: WebView,
        private val initialState: S,

        // fn to convert a given state to a DOM node suitable for display
        private val view: (S) -> Node,

        // fn to update the state by performing an action
        private val update: (Action, S) -> S
) {
    private val engine = webview.engine

    private val state = AtomicReference(initialState)

    private val actionProcessor = actor<Action>(
            context = newSingleThreadContext("Action Processor")
    ) {
        var currentState = state.get()
        for (action in channel) {
            // perform the action and update the state
            val nextState = update(action, currentState)
            state.set(nextState)
            currentState = nextState
        }
    }

    private var currentView = view(initialState)

    init {
        renderInitialView()
        launchTimer()
    }

    private fun renderInitialView() {
        launch(UI) {
            engine.loadWorker.stateProperty().addListener { _, _, newValue ->
                if (newValue == Worker.State.SUCCEEDED) {
                    // add hook for actions
                    val window = engine.executeScript("window") as JSObject
                    window.setMember("actions", hook)

                    // WebView doesn't support varargs, so marshal args with this function
                    window.eval("function performAction() { actions.perform(arguments) }")
                }
            }
            engine.loadContent(toHtml(currentView))
        }
    }

    /** Refresh view if required on JavaFX pulse */
    private fun launchTimer() {
        object : AnimationTimer() {
            var previousState: S? = null

            override fun handle(now: Long) {
                val currentState = state.get()
                if (currentState != previousState) {
                    // update the view based on the new state
                    val prevView = currentView
                    val nextView = view(currentState)
                    engine.document?.documentElement?.let { copy(prevView, nextView, it) }
                    currentView = nextView
                    previousState = currentState
                }
            }
        }.apply { start() }
    }

    /**
     * Actions go through the channel to be processed in order
     */
    fun perform(action: Action) {
        runBlocking { actionProcessor.send(action) }
    }

    /** Convenience method to perform an action */
    fun perform(name: Any, vararg args: Any?) = perform(Action(name, *args))

    /**
     * Provide hook for javascript code to perform an action
     */
    private val hook = object {
        fun perform(args: JSObject) {
            val action = Action.from(args) // UI thread
            launch { this@Redux.perform(action) } // don't block the UI thread
        }
    }
}

private val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

/**
 * Convenience method for creating a new document
 */
fun createDoc(): Document = builder.newDocument()

/**
 * Update dst structure using values from src. Don't update values if there's
 * no change between ref and src to avoid overwriting user changes and focus.
 *
 */
internal fun copy(ref: Node?, src: Node, dst: Node) {
    assert(src.nodeName == dst.nodeName && src.nodeType == dst.nodeType, {
        "Can't copy nodes of differing type: ${src.nodeName}/${src.nodeType} -> ${dst.nodeName}/${dst.nodeType}"
    })
    copyAttributes(ref, src, dst)
    copyChildren(ref, src, dst)

    // copy text
    if (src is Text && dst.textContent != src.textContent) {
        if (ref == null || ref.textContent != src.textContent) dst.textContent = src.textContent
    }

    // copy siblings
    val srcNext = src.nextSibling
    if (srcNext == null) discard(dst.nextSibling)
    else {
        val name = srcNext.nodeName
        val type = srcNext.nodeType
        val dstNext = dst.nextSibling ?: createNode(dst, name, type).apply {
            dst.parentNode.appendChild(this)
        }
        val refNext = ref?.nextSibling?.let { findMatching(it, name, type, srcNext.textContent) }
        copySibling(refNext, srcNext, dstNext)
    }
}

/**
 * Remove this node and all siblings after it
 */
private fun discard(n: Node?) {
    n?.nextSibling?.let { discard(it) }

    // don't discard firebug frame so it can be used for debugging
    if ("FirebugUI" == n?.attributes?.getNamedItem("id")?.nodeValue) return

    n?.let { n.parentNode.removeChild(n) }
}

/**
 * Remove all nodes up to the one specified
 */
private fun discardUntil(from: Node, to: Node) {
    if (from != to) {
        val next = from.nextSibling
        from.parentNode.removeChild(from)
        discardUntil(next, to)
    }
}

/**
 * Copy child nodes if there are any
 */
private fun copyChildren(ref: Node?, src: Node, dst: Node) {
    src.firstChild?.let { srcChild ->
        val name = srcChild.nodeName
        val type = srcChild.nodeType
        val refChild = ref?.firstChild?.let { findMatching(it, name, type, srcChild.textContent) }
        val dstSibling = dst.childNodes?.item(0)
                ?: dst.appendChild(createNode(dst, name, type)) // create one if necessary
        copySibling(refChild, srcChild, dstSibling)
    }
}

/**
 * The given dst doesn't necessarily match the src by name and type.
 * If it has a matching sibling, com.github.sgdan.webviewredux.discard anything before that one,
 * then copy. Otherwise, insert a new sibling before the given dst.
 */
private fun copySibling(ref: Node?, src: Node, dstSibling: Node) {
    val name = src.nodeName
    val type = src.nodeType
    val matching = findMatching(dstSibling, name, type, src.textContent)?.apply {
        discardUntil(dstSibling, this)
    }
    val dst = matching ?: insertNode(dstSibling, name, type)
    copy(ref, src, dst)
}

private fun insertNode(before: Node, name: String, type: Short): Node {
    return before.parentNode.insertBefore(createNode(before, name, type), before)
}

private fun createNode(creator: Node, name: String, type: Short): Node {
    val doc = creator.ownerDocument
    return when (type) {
        Node.ELEMENT_NODE -> doc.createElement(name)
        Node.ATTRIBUTE_NODE -> doc.createAttribute(name)
        Node.TEXT_NODE -> doc.createTextNode(name)
        else -> throw Exception("Unexpected node type: $type")
    }
}

/**
 * @return matching sibling or null if there isn't one
 */
private fun findMatching(n: Node, name: String, type: Short, text: String?): Node? {
    return when {
        n is Text && n.textContent == text -> n
        n.nodeName.equals(name, true) && n.nodeType == type -> n
        else -> n.nextSibling?.let { return findMatching(it, name, type, text) }
    }
}

private fun copyAttributes(ref: Node?, from: Node, to: Node) {
    val fromAtt = from.attributes
    val toAtt = to.attributes
    val refAtt = ref?.attributes
    if (fromAtt == null && toAtt == null) return

    // update/create attributes to match src
    0.until(fromAtt.length).forEach {
        val srcItem = fromAtt.item(it)
        val srcName = srcItem.nodeName
        val dstItem = toAtt.getNamedItem(srcName)
                ?: to.ownerDocument.createAttribute(srcName).apply {
                    toAtt.setNamedItem(this)
                }
        if (srcItem != dstItem) {
            val refItem = refAtt?.getNamedItem(srcName)
            if (refItem == null || refItem.nodeValue != srcItem.nodeValue) {
                dstItem.nodeValue = srcItem.nodeValue
            }
        }
    }

    // remove attributes not in src
    if (toAtt == null) return
    0.until(toAtt.length).forEach {
        toAtt.item(it)?.nodeName?.let {
            fromAtt.getNamedItem(it) ?: toAtt.removeNamedItem(it)
        }
    }
}

/**
 * Convert DOM node to HTML string
 */
fun toHtml(e: Node): String {
    val sr = StreamResult(StringWriter())
    TransformerFactory.newInstance().newTransformer().transform(DOMSource(e), sr)
    return sr.writer.toString()
}