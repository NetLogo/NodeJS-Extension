const net = require('net');
const readline = require('readline');

// see https://262.ecma-international.org/5.1/#sec-10.4.2
// or  https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval
const global_scope_eval = eval;

// In
let STMT_MSG = 0
let EXPR_MSG = 1
let ASSN_MSG = 2

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

function handle_assignment(sock, body) {
    if (body.hasOwnProperty("varName") && body.hasOwnProperty("value")) {
        let varName = body["varName"];
        let value = body["value"];
        console.log("assn:", varName, value);
        let assn_status = global_scope_eval('var ' + varName + ' = ' + JSON.stringify(value) + ';');
        let out = {
            "type" : SUCC_MSG,
            "body" : ""
        }
        write_obj(sock, out);
    }
}

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

server.listen(1337, () => {
    // console.log("listening on:", server.address().port);
    console.log(server.address().port);
})
