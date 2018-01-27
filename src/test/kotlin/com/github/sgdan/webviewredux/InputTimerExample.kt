package com.github.sgdan.webviewredux

import com.github.sgdan.webviewredux.InputTimerExample.ActionType.*
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.dom.create
import org.w3c.dom.Node

class InputTimerExample : Application() {
    enum class ActionType {
        ENTER, CLEAR, FLIP
    }

    data class State(
            val big: Boolean = false,
            val entered: String? = null
    )

    fun view(state: State): Node = createDoc().create.html {
        body {
            h1 { +"Input Timer Example" }
            +"Updates from timer should not affect content or focus of input fields"
            br
            br
            +"Input 1:"
            textInput {
                id = "input1"
            }
            br
            +"Input 2:"
            textInput {
                id = "input2"
            }
            br
            button {
                +"Enter"
                onClick = "performAction('ENTER', document.getElementById('input1').value, document.getElementById('input2').value);"
            }
            button {
                +"Clear"
                onClick = "performAction('CLEAR');"
            }
            br

            val enteredText = state.entered ?: "Nothing entered"
            if (state.big) h1 { +enteredText }
            else h2 { +enteredText }
        }
    }

    fun update(action: Action, state: State) = when (action.to<ActionType>()) {
        CLEAR -> state.copy(entered = null)
        FLIP -> state.copy(big = !state.big)
        ENTER -> state.copy(
                entered = action.params?.map { it.toString().trim() }
                        ?.filter { !it.isEmpty() }
                        ?.joinToString(separator = " and ")
        )
        else -> throw Exception("Unexpected action: $action")
    }

    override fun start(stage: Stage) {
        val webview = WebView()
        val redux = Redux(
                webview,
                State(),
                ::view,
                ::update
        )
        stage.scene = Scene(webview)
        stage.show()

        // timer to send flip actions
        launch {
            while (true) {
                delay(500)
                redux.perform(FLIP)
            }
        }
    }
}

fun main(vararg args: String) {
    Application.launch(InputTimerExample::class.java)
}