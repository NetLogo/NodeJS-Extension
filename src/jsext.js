const net = require('net');
const readline = require('readline');
const util = require('util');

//--------------------------------Overview-------------------------------------
// The language specific code needs to do a few things
// * Create a tcp server on localhost
// * listen for messages from the NetLogo extension
// * Execute/evaluate the code coming in those messages using something like
//   `eval`
// * Return the results back to the extension
// * Give error information if anything goes wrong or if the code passed in
//   throws any exceptions
//
// Input messages have 4 types, statements, expressions, assignment, and
// stringified expressions. Statements should be executed, expressions
// should be evaluated (and return a result), assignments should assign
// the result of an evaluation to a variable with the given nane, and
// stringified expressions should be evaluated, but converted into a helpful
// string representation before being returned to the extension
//
// Output messages have two types, success and failure. Failure should be
// accompanied by a cause of the failure.
//
// The only JS-specific weird stuff here has to do with the way JS handles eval
// and how to make the node.js features like require work within the eval
// execution environment.


//-----------------------------JS specific weird stuff-------------------------
// see https://262.ecma-international.org/5.1/#sec-10.4.2
// or  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval
const global_scope_eval = eval;

// re-exporting node "pseudo-globals" to actual global scope so that client JS code
// can access them.
// https://nodejs.org/en/knowledge/getting-started/globals-in-node-js/
global["require"] = require
global["module"] = module

//------------------------------Utilities--------------------------------------

// In
let STMT_MSG = 0
let EXPR_MSG = 1
let ASSN_MSG = 2
let EXPR_MSG_STRINGIFIED = 3

// Out
let SUCC_MSG = 0
let ERR_MSG = 1

function write_obj(sock, obj) {
    sock.write(JSON.stringify(obj) + "\n");
}

function send_error(sock, message, cause) {
    let err_msg = {
        "type" : ERR_MSG,
        "body" : {
            "message" : message,
            "cause"   : cause
        }
    }
    console.log(err_msg);
    write_obj(sock, err_msg)
}

function handle_exception(sock, e) {
    send_error(sock, e["name"], e["message"]);
}

//----------------------------Handle messages----------------------------------

function handle_statement(sock, body) {
    let res = global_scope_eval(body);
    // console.log("stmt:", res);
    let out = {
        "type" : SUCC_MSG,
        "body" : ""
    }
    write_obj(sock, out);
}

function handle_expression(sock, body) {
    let res = global_scope_eval(body);
    // console.log("expr:", res);
    let out = {
        "type" : SUCC_MSG,
        "body" : res
    }
    write_obj(sock, out);
}

function handle_expression_stringified(sock, body) {
    let res = util.inspect(global_scope_eval(body))
    let out = {
        "type" : SUCC_MSG,
        "body" : res
    }
    write_obj(sock, out);
}

function handle_assignment(sock, body) {
    if (body.hasOwnProperty("varName") && body.hasOwnProperty("value")) {
        let varName = body["varName"];
        let value = body["value"];
        // console.log("assn:", varName, value);
        global_scope_eval('var ' + varName + ' = ' + JSON.stringify(value) + ';');
        let out = {
            "type" : SUCC_MSG,
            "body" : ""
        }
        write_obj(sock, out);
    }
}

//------------------------------Main Server------------------------------------

const server = net.createServer((sock) => {
    let rl = readline.createInterface(sock, sock);
    rl.on('line', (line) => {
        let data = JSON.parse(line);
        if (data.hasOwnProperty("type") && data.hasOwnProperty("body")) {
            let type = data["type"];
            let body = data["body"];

            try {
                switch (type) {
                    case STMT_MSG:
                        handle_statement(sock, body);
                        break;
                    case EXPR_MSG:
                        handle_expression(sock, body)
                        break;
                    case ASSN_MSG:
                        handle_assignment(sock, body);
                        break;
                    case EXPR_MSG_STRINGIFIED:
                        handle_expression_stringified(sock, body);
                        break;
                    default:
                        send_error(sock, "Bad message type:" + type, "");
                        break;
                }
            } catch (e) {
                handle_exception(sock, e);
            }
        } else {
            send_error(sock, "Bad message: no type and or body", "");
        }
    })

    sock.on('close', (hadError) => {
        if (hadError) {
            process.exit(1);
        } else {
            process.exit(0);
        }
    })
}).on('error', (err) => {
    console.log("could not create listener: ", err);
})

//------------------------------Start server-----------------------------------
if (process.argv.length === 3) { // If port is specified -- Useful for testing
    let port = +process.argv[3]
    server.listen(port, () => {
        console.log(server.address().port); // Write the port number to stdout so the extension can connect
    })
} else { // if port is not specified -- production code path
    server.listen(() => {
        console.log(server.address().port); // Write the port number to stdout so the extension can connect
    })
}

