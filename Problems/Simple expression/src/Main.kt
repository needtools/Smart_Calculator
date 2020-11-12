package calculator

import java.util.*
import java.math.BigInteger

class CalcException(s: String): RuntimeException(s)

private val operators = mapOf("*" to 2, "/" to 2, "+" to 1, "-" to 1)
private val variables: MutableMap<String, BigInteger> = mutableMapOf()

fun main() {
    val scanner = Scanner(System.`in`)
    var expression: String

    while (scanner.hasNextLine()) {
        expression = scanner.nextLine()

        if (expression.isEmpty()) continue
        if (expression == "/exit") break
        if (expression == "/help") {
            help()
            continue
        }

        try {
            if (expression[0] == '/') {
                throw CalcException("Unknown command")
            }

            if (expression.isAssignment()) {
                assignVariables(expression)
                continue
            }

            println("${calculate(infix2rpn(expression))}")
        } catch (e: CalcException) {
            println(e.message)
        } catch (e: Exception) {
            println("Invalid expression")
        }
    }

    println("Bye!")
}

fun help() {
    println("""
        This program calculates algebraic expressions.

        The following operators are supported:
        > addition (+)
        > subtraction (-)
        > multiplication (*)
        > integer division (/)
        
        Here is an example of a supported expressions:
        > 3 + 8 * ((4 + 3) * 2 + 1) - 6 / (2 + 1)
        > 3 + 2 * 4
        
        Type /help to see this text.
        Type /exit to exit.
    """.trimIndent())
}

fun String.isAssignment() = this.contains("=")

fun assignVariables(string: String) {
    val tokens = string.split("=").map { it.trim() }
    if (tokens.size != 2) {
        throw CalcException("Invalid assignment")
    }

    if (!tokens[0].isValidVariableName()) {
        throw CalcException("Invalid identifier")
    }

    if (!tokens[1].isNumeric() && !tokens[1].isValidVariableName()) {
        throw CalcException("Invalid assignment")
    }

    if (!tokens[1].isNumeric()) {
        if (!variables.containsKey(tokens[1])) {
            throw CalcException("Unknown variable")
        }

        variables[tokens[0]] = variables.getValue(tokens[1])
        return
    }

    variables[tokens[0]] = BigInteger(tokens[1])
}

fun calculate(expr: String): BigInteger {
    val stack = Stack<BigInteger>()
    val tokens = expr.split("\\s+".toRegex())

    // Is expr starts with a command?
    if (tokens.size == 1 && tokens[0].first() == '/') {
        throw CalcException("Unknown command")
    }

    // Consider it is a number
    if (tokens.size < 2) {
        return resolve(tokens[0])
    }

    // Missed operator
    if (tokens.size % 2 == 0) {
        throw CalcException("Invalid expression")
    }

    tokens.forEach {
        if (it.isNumeric() || it.isValidVariableName()) {
            stack.push(resolve(it))
        } else {
            if (stack.size < 2) {
                throw CalcException("Invalid expression")
            }

            val op2 = stack.pop()
            val op1 = stack.pop()

            stack.push(eval(it, op1, op2))
        }
    }
    return stack.pop()
}

fun infix2rpn(expr: String): String {
    var queue = ""
    val stack = Stack<String>()

    val pattern = "(?<=[-+*/^()])|(?=[-+*/^()])".toRegex()

    val tokens = expr
            .replace("([a-zA-Z]+)".toRegex()) {
                "${resolve(it.groupValues[1])}"
            }
            .replace("\\s+", "")
            .replace("[+]+".toRegex()) { "+" }
            .replace("[+]".toRegex(), "--")
            .replace("(--)+-".toRegex(), "-")
            .replace("--", "+")
            .replace("[+]\\s?-".toRegex()) { "-" }
            .split(pattern)
            .map { it.trim() }

    for (token in tokens) {
        if (token.isBlank()) continue

        if (token.isNumeric() || token.isValidVariableName()) {
            if (queue.trim().isOperator()) {
                queue = "${queue.trim()}$token"
            } else {
                queue += " $token"
            }
            continue
        }

        if (token.isOperator()) {
            if (queue.trim().isEmpty()) {
                queue += " $token"
                continue
            }

            while (!stack.isEmpty()) {
                val op2 = stack.pop()

                if (!op2.isOperator()) {
                    stack.push(op2)
                    break
                }

                if (op2.getPrecedence() >= token.getPrecedence()) {
                    queue += " $op2"
                } else {
                    stack.push(op2)
                    break
                }
            }

            stack.push(token)
            continue
        }

        if (token == "(") {
            stack.push(token)
            continue
        } else if (token == ")") {
            if (stack.isEmpty()) {
                throw CalcException("Invalid expression")
            }

            var lparen = false
            while (!stack.isEmpty()) {
                val tok = stack.pop()
                if (tok == "(") {
                    lparen = true
                    break
                }

                queue += " $tok"
            }

            while (stack.isEmpty() && !lparen) {
                throw CalcException("Invalid expression")
            }
        } else {
            throw CalcException("Invalid expression")
        }
    }

    while (!stack.isEmpty()) {
        if (stack.peek().isOperator()) {
            queue += " ${stack.pop()}"
            continue
        }

        throw CalcException("Invalid expression")
    }
    return queue.trim()
}

fun String.isValidVariableName(): Boolean {
    return this.isNotEmpty() && this.filterNot {
        it.isLetter()
    }.isEmpty()
}

fun String.isNumeric(): Boolean {
    return try {
        BigInteger(this)
        true
    } catch (e: NumberFormatException) {
        false
    }
}
fun String.isOperator(): Boolean {
    if (this.isEmpty()) {
        return false
    }

    if (this.length == 1) {
        return operators.containsKey(this)
    }

    if (this.trim().first() == '/' || this.trim().first() == '*') {
        return false
    }

    return operators.containsKey(this[0].toString())
}
fun String.getPrecedence(): Int {
    if (this.isNotEmpty() && operators.containsKey(this[0].toString())) {
        return operators.getValue(this[0].toString())
    }

    throw CalcException("Invalid expression")
}
fun resolve(string: String): BigInteger {
    return if (string.isValidVariableName()) {
        getValue(string)
    } else {
        BigInteger(string)
    }
}
fun eval(operator: String, lhs: BigInteger, rhs: BigInteger): BigInteger {
    if (operator.isEmpty()) {
        throw CalcException("Unknown command")
    }

    if (operator.length == 1) {
        return when (operator) {
            "+"  -> lhs + rhs
            "-"  -> lhs - rhs
            "*"  -> lhs * rhs
            "/"  -> lhs / rhs
            else -> throw CalcException("Unknown command")
        }
    }

    return when (operator[0].toString()) {
        "+"  -> lhs + rhs
        "-"  -> if (operator.length % 2 == 0) lhs + rhs else lhs - rhs
        else -> throw CalcException("Unknown command")
    }
}

fun getValue(string: String): BigInteger {
    if (!string.isValidVariableName()) {
        throw CalcException("Invalid identifier")
    }

    if (!variables.containsKey(string)) {
        throw CalcException("Unknown variable")
    }

    return variables.getValue(string)
}
