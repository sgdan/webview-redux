# webview-redux [![Build Status](https://travis-ci.org/sgdan/webview-redux.svg?branch=master)](https://travis-ci.org/sgdan/webview-redux)
Provides a simple redux-like framework for the JavaFx WebView component.

See [CounterExample.kt](src/test/kotlin/com/github/sgdan/webviewredux/CounterExample.kt) which demonstrates
basic usage.

### Redux class
The [Redux.kt](src/main/kotlin/com/github/sgdan/webviewredux/Redux.kt) class can be instantiated by providing:
- a WebView component to use as display
- the initial state: State
- a view function: (State) -> View
- an update function: (State, Action) -> State

### View function
The view function can use the [kotlinx.html DSL](https://github.com/Kotlin/kotlinx.html) to
build the view from the state:
```
fun view(state: Int): Node = createDoc().create.html {
        body {
            h1 { +"Counter" }
            +"Current value: $state"
        }
    }
```
An Animation timer is used so that the view function won't be used more than once per screen
refresh cycle.

### Events
The Redux class adds a `performAction` function to the JavaScript
context of the WebView. This allows code in the view function to create named action events
with optional parameters:
```
button {
    +"Clear"
    onClick = "performAction('CLEAR');"
}
button {
    +"Enter"
    onClick = "performAction('ENTER', document.getElementById('input1').value, 'arg');"
}
```

Events can also be triggered from a background thread or co-routine:
```
launch {
    while (true) {
        delay(1000)
        redux.perform(Action.TICK)
    }
}
```
