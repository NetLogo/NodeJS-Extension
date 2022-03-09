
# Netlogo JavaScript Extension

This NetLogo extension allows you to run JavaScript code from NetLogo using the Node.js runtime.
Because it is built on top of Node.js, all the libraries built for Node work within the extension.

## Building

Make sure your sbt is at least at version 0.13.6

Run `sbt package`.

If compilation succeeds, `js.jar` will be created, and the required dependencies will be copied to the root of the repository.  Copy all the `jar` files and `jsext.js` from the repository root to a `js` directory inside your NetLogo `extensions` directory.

## Using

To run JavaScript code you must install the Node.js runtime and have the `node` executable on your `PATH`.
You can download Node.js from [their site](https://nodejs.org/).  Node versions 14 and 16 are supported.

To use this extension, you must first include it at the top of your NetLogo model code in an `extensions` declaration.

```netlogo
extensions [
    js
    ; ... your other extensions
]
```

You must then initialize the JS engine with `js:setup`. This only needs to be done once per session.
Any subsequent calls will reset your JS environment.

Here's an example to get you started:

```netlogo
observer> js:setup
;; js:runresult evaluates JS statements and returns the result back to NetLogo
observer> show js:runresult "2 + 2"
observer: 4
;; js:run runs JS code
observer> js:run "console.log(4); console.log(2)"
4
2
;; any standard output gets forwarded to the command center output
;; require() behaves as you would expect
observer> js:run "var util = require('util')"
observer> show js:runresult "util.format('Hello %s!', 'World')"
observer: "Hello World!"
;; js:set sets JS variables to values from NetLogo
observer> ask patch 0 0 [ set pcolor red ]
observer> js:set "center_patch_color" [pcolor] of patch 0 0
observer> show js:runresult "center_patch_color"
observer: 15 ;; the NetLogo representation of the color red
```

See the documentation for each of the particular primitives for details on, for instance, how to multi-line statements and how object type conversions work.

The extension also includes an interactive JavaScript console/REPL that is connected to the same JavaScript environment as the main window's NetLogo environment.
It is useful for executing longer blocks of JavaScript code or quickly examining or modifying JavaScript values.
This console can be opened via the menu bar under the JSExtension heading.

### Error handling

JavaScript errors will be reported in NetLogo as "Extension exceptions". For instance, this code:

```netlogo
js:run "throw new Error('hi')"
```

will result in the NetLogo error "Extension exception: hi".
To see the JavaScript stack trace of the exception, click "Show internal details".
If you then scroll down, you will find the JavaScript stack trace in the middle of the Java stack trace

## Primitives


### `js:setup`

```NetLogo
js:setup
```


Create the Node.js session that this extension will use to execute code.
This command *must* be run before running any other JavaScript extension primitive.
Running this command again will shutdown the current JavaScript environment and start a new one.



### `js:run`

```NetLogo
js:run javascript-statement
```



Runs the given JavaScript statements in the current Node.js session.
To make multi-line JavaScript code easier to run, this command will take multiple strings, each of which will be interpreted as a separate line of JavaScript code.
For instance:

```NetLogo
  (js:run
    "function tell_joke(setup, punchline) {"
    "   console.log(setup);"
    "   console.log('.');"
    "   console.log('.');"
    "   console.log('.');"
    "   console.log(punchline);"
    "}"
    "var my_setup = 'what do you call a tortoise who takes up photography?'"
    "var my_punchline = 'a snapping turtle!'"
    "tell_joke(my_setup, my_punchline)"
)
```

`js:run` will wait for the statements to finish before continuing.
If you have long-running JavaScript code, NetLogo may freeze for a bit while it runs.



### `js:runresult`

```NetLogo
js:runresult javascript-expression
```


Evaluates the given JavaScript expression and reports the result.
`js:runresult` attempts to convert from JavaScript data types to NetLogo data types.
Numbers, strings, and booleans convert as you would expect, except for outliers like Infinity and NaN which will be converted into `nobody`.
JavaScript arrays will be converted to NetLogo lists.
JavaScript objects will convert to a NetLogo list of key-value pairs (where each pair is itself represented as a list).
`undefined` and `null` will be converted to `nobody`.
Other objects will be converted to a string representation if possible and `nobody` if not.



### `js:set`

```NetLogo
js:set variable-name value
```


Sets a variable in the JavaScript session with the given name to the given NetLogo value.
NetLogo objects will be converted to JavaScript objects as expected.

```NetLogo
js:set "x" [1 2 3]
js:run "console.log(x)" ;; prints [1, 2, 3] to the command center
show js:runresult "x" ;; reports [1 2 3]
```

Agents are converted into dictionaries with elements for each agent variable.
Agentsets are converted into lists of agent dictionaries.

```NetLogo
breed [goats goat]
goats-own [energy ]
create-goats 1 [ set heading 0 set color 75 ]
ask goat 0 [ set energy 42 ]
js:set "goat" goat 0
show js:runresult "console.log(JSON.stringify(goat))"
;; Should output
;;{
;; "WHO": 0,
;; "COLOR": 75,
;; ...
;;}
```

Agents with variables containing references to agentsets will have those variables converted into the string representation of that agentset.


