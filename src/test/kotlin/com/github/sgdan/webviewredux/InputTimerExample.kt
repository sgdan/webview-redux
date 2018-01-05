package com.github.sgdan.webviewredux

import javafx.application.Application
import javafx.scene.Scene
import javafx.stage.Stage
import kotlinx.coroutines.experimental.launch
import kotlinx.html.*
import kotlinx.html.dom.create
import mu.KotlinLogging
import org.w3c.dom.Node
import java.lang.Thread.sleep

private val log = KotlinLogging.logger {}

enum class InputActionType {
    ENTER, CLEAR, FLIP
}

data class InputAction(
        val type: InputActionType,
        val params: Array<Any>? = null
)

data class InputState(
        val big: Boolean = false,
        val entered: String? = null
)

class InputTimerExample : Application() {

    fun view(state: InputState): Node = createDoc().create.html {
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

    fun update(action: Any, state: InputState): InputState {
        if (action is InputAction) {
            when (action.type) {
                InputActionType.CLEAR -> return state.copy(entered = null)
                InputActionType.FLIP -> return state.copy(big = !state.big)
                InputActionType.ENTER -> return state.copy(
                        entered = action.params?.map { it.toString().trim() }
                                ?.filter { !it.isEmpty() }
                                ?.joinToString(separator = " and ")
                )
            }
        }
        throw Exception("Unexpected action: $action")
    }

    override fun start(stage: Stage) {
        val redux = Redux(
                InputState(),
                ::view,
                ::update,
                { name, params ->
                    InputAction(InputActionType.valueOf(name), params)
                }
        )
        stage.scene = Scene(redux.webview)
        stage.show()

        // timer to send flip actions every 1s
        launch {
            while (true) {
                sleep(1000)
                redux.perform(InputAction(InputActionType.FLIP))
            }
        }
    }
}

fun main(vararg args: String) {
    Application.launch(InputTimerExample::class.java)
}