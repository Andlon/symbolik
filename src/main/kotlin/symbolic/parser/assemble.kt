package symbolic.parser

import symbolic.expressions.*
import symbolic.util.popOrNull
import symbolic.util.popWhile
import java.util.*

open class AssemblyException(message: String) : Exception(message)
class MismatchedParanthesisException : AssemblyException("Mismatched paranthesis")

fun assemble(tokens: List<Token>): Expression {
    // The following is an implementation of the Shunting-yard algorithm (Dijkstra), as specified on Wikipedia:
    // https://en.wikipedia.org/wiki/Shunting-yard_algorithm
    val stack = Stack<Token>()
    val output = Stack<Expression>()

    for (token in tokens) {
        when (token) {
            is Token.Constant -> output.push(Constant.fromToken(token))
        // TODO: So far we assume all names are variables. Later we want to support functions, textual operators etc.
            is Token.Name -> output.push(Variable.fromToken(token))
            is Token.BinaryOperator -> {
                val o1 = token
                val isPoppable = { o2: Token -> when(o2) {
                    is Token.BinaryOperator -> when {
                        o1.isLeftAssociative() && o1.precedence() <= o2.precedence() -> true
                        o1.isRightAssociative() && o1.precedence() < o2.precedence() -> true
                        else -> false
                    }
                    else -> false
                }}

                stack.popWhile(isPoppable)
                        .filterIsInstance<Token.BinaryOperator>()
                        .forEach { applyOperatorToExpressions(it, output) }
                stack.push(token)
            }
            is Token.LeftParanthesis -> stack.push(token)
            is Token.RightParanthesis -> {
                stack.popWhile { it !is Token.LeftParanthesis }
                        .filterIsInstance<Token.BinaryOperator>()
                        .forEach { applyOperatorToExpressions(it, output) }

                if (stack.isEmpty()) {
                    throw MismatchedParanthesisException()
                } else {
                    stack.pop()
                }
            }
        }
    }

    while (stack.isNotEmpty()) {
        val token = stack.pop()
        when (token) {
            is Token.BinaryOperator -> applyOperatorToExpressions(token, output)
            is Token.Paranthesis -> throw MismatchedParanthesisException()
            else -> throw AssemblyException("Unexpected operator.")
        }

    }

    return when (output.isNotEmpty()) {
        true -> output.pop()
        false -> EmptyExpression
    }
}

fun applyOperatorToExpressions(token: Token.BinaryOperator, expressions: Stack<Expression>) {
    val right = expressions.popOrNull()
    val left = expressions.popOrNull()

    if (left == null || right == null) {
        val stem = "Can not apply binary operator " + token.presentation()
        val message = stem + when {
            left == null && right == null -> " without operands."
            left == null -> " with no left hand operand."
            right == null -> " with no right hand operand."
            else -> "."
        }
        throw AssemblyException(message)
    }

    val expr = BinaryOperator.fromToken(token, left, right)
    expressions.add(expr)
}

