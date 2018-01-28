package com.github.sgdan.webviewredux

import com.github.sgdan.webviewredux.ManyActionsExample.TestAction.*
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.dom.create
import org.w3c.dom.Node

class ManyActionsExample : Application() {
    data class State(
            val running: Boolean = false,
            val counter: Int = 0,
            val frame: Int = 0)

    enum class TestAction {
        START, STOP, INCREMENT, NEXT
    }

    private val max = 20

    fun view(state: State): Node = createDoc().create.html {
        body {
            h1 { +"Many Actions Example" }
            br
            button {
                +"Start"
                disabled = state.running
                onClick = "performAction('START');"
            }
            button {
                +"Stop"
                disabled = !state.running
                onClick = "performAction('STOP');"
            }
            br
            state.frame.let { +"${"-".repeat(it)}_${"-".repeat(max - it)}" }
            br
            br
            +"""
                A coroutine is running creating a constant stream of INCREMENT actions which increase the
                counter: ${state.counter}. Although background CPU usage should be high, the animation and
                counter should run smoothly, and the buttons should be responsive. There are two reasons for
                this:
            """
            ul {
                li {
                    +"""
                        Although there are many actions which update the state, not every update is rendered
                        to the WebView. An AnimationTimer is used so that the WebView is only updated once
                        per screen refresh.
                    """
                }
                li {
                    +"""
                        The perform(Action) method blocks due to the underlying channel. If the calling thread
                        is also generating actions then it won't generate more state updates than the processing thread
                        can handle.
                    """
                }
            }
        }
    }

    fun update(action: Action, state: State): State = when (action.to<TestAction>()) {
        START -> state.copy(running = true)
        STOP -> state.copy(running = false)
        INCREMENT -> state.copy(counter = state.counter + 1)
        NEXT -> when {
            !state.running -> state
            state.frame == max -> state.copy(frame = 0)
            else -> state.copy(frame = state.frame + 1)
        }
        else -> throw Exception("Unexpected action: $action")
    }

    override fun start(stage: Stage) {
        val webview = WebView()
        val redux = Redux(
                webview,
                State(), // initial state
                ::view,
                ::update
        )

        // generate endless increment actions
        launch { while (true) redux?.perform(INCREMENT) }

        // timer for next frame of simple animation
        launch {
            while (true) {
                redux.perform(NEXT)
                delay(50)
            }
        }

        stage.scene = Scene(webview)
        stage.show()
    }
}

fun main(vararg args: String) {
    Application.launch(ManyActionsExample::class.java)
}