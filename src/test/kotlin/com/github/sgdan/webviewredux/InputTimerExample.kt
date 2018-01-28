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
        FLIP, UPDATE
    }

    data class State(
            val big: Boolean = false,
            val name: String = "Mary",
            val count: Int = 6,
            val pet: String = "turtle"
    )

    /**
     * @return JavaScript to get value of named field
     */
    private fun valueOf(name: String) = "document.getElementById('$name').value"

    fun view(state: State): Node = createDoc().create.html {
        body {
            h1 { +"Input Timer Example" }
            +"Updates from timer should not affect content or focus of input fields"
            br
            br

            // All 3 field types send the same event. A bit redundant but useful to demonstrate
            // the call function of the Action class to marshal the arguments.
            val command = "performAction('$UPDATE',${valueOf("name")},${valueOf("count")},${valueOf("pet")})"
            +"Name:"
            textInput {
                id = "name"
                onChange = command
                value = state.name
            }
            br
            +"Count:"
            select {
                id = "count"
                1.until(10).forEach {
                    option {
                        +"$it"
                        value = it.toString()
                        if (it == state.count) selected = true
                    }
                }
                onChange = command
            }
            br
            +"Pet:"
            select {
                id = "pet"
                listOf("dog", "cat", "turtle").forEach {
                    option {
                        +"$it"
                        value = it
                        if (it == state.pet) selected = true
                    }
                }
                onChange = command
            }

            br

            val enteredText = "%s has %d %s%s"
                    .format(state.name, state.count, state.pet, if (state.count > 1) "s" else "")
            if (state.big) h1 { +enteredText }
            else h2 { +enteredText }
        }
    }

    fun update(action: Action, state: State) = when (action.to<ActionType>()) {
        UPDATE -> action.call(state, ::updateMsg)
        FLIP -> state.copy(big = !state.big)
        else -> throw Exception("Unexpected action: $action")
    }

    fun updateMsg(state: State, name: String, count: String, pet: String) = state.copy(
            name = name,
            count = count.toIntOrNull() ?: state.count,
            pet = pet)

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