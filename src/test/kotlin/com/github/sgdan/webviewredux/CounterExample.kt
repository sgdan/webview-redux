package com.github.sgdan.webviewredux

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import kotlinx.html.*
import kotlinx.html.dom.create
import org.w3c.dom.Node

class CounterExample : Application() {
    enum class CounterAction {
        INCREMENT, DECREMENT
    }

    val css = this::class.java.classLoader.getResource("CounterExample.css")
            .toURI().toURL().toExternalForm()

    fun view(state: Int): Node = createDoc().create.html {
        head {
            title("Title")
            link {
                rel = "stylesheet"
                href = css
            }
            script {
                type = "text/javascript"
                src = "https://getfirebug.com/firebug-lite.js#startOpened"
            }
        }
        body {
            h1 { +"Counter" }
            +"Current value: $state"
            br
            button {
                id = "inc"
                +"Increment"
                onClick = "performAction('INCREMENT');"
            }
            button {
                id = "dec"
                +"Decrement"
                onClick = "performAction('DECREMENT');"
            }
        }
    }

    fun update(action: Action, state: Int): Int = when (action.to<CounterAction>()) {
        CounterAction.INCREMENT -> state + 1
        CounterAction.DECREMENT -> state - 1
        else -> throw Exception("Unexpected action: $action")
    }

    override fun start(stage: Stage) {
        // Initialise the view and redux object
        val webview = WebView()
        Redux(webview,
                0, // initial state
                ::view,
                ::update)

        // Show the view
        stage.scene = Scene(webview)
        stage.show()
    }
}

fun main(vararg args: String) {
    Application.launch(CounterExample::class.java)
}