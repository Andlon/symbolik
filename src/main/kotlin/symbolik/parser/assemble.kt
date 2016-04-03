package symbolik.parser

import symbolik.expressions.*
import symbolik.util.popOrNull
import symbolik.util.popWhile
import java.util.*

open class AssemblyException(message: String) : Exception(message)
class MismatchedParenthesisException : AssemblyException("Mismatched parenthesis")

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
            is Token.Operator -> {
                val o1 = token
                val isPoppable = { o2: Token -> when(o2) {
                    is Token.Operator -> when {
                        o1.isLeftAssociative() && o1.precedence() <= o2.precedence() -> true
                        o1.isRightAssociative() && o1.precedence() < o2.precedence() -> true
                        else -> false
                    }
                    else -> false
                }}

                stack.popWhile(isPoppable)
                        .filterIsInstance<Token.Operator>()
                        .forEach { applyOperatorToExpressions(it, output) }
                stack.push(token)
            }
            is Token.LeftParenthesis -> stack.push(token)
            is Token.RightParenthesis -> {
                stack.popWhile { it !is Token.LeftParenthesis }
                        .filterIsInstance<Token.Operator>()
                        .forEach { applyOperatorToExpressions(it, output) }

                if (stack.isEmpty()) {
                    throw MismatchedParenthesisException()
                } else {
                    stack.pop()
                }
            }
        }
    }

    stack.asReversed().forEach {
        when (it) {
            is Token.Operator -> applyOperatorToExpressions(it, output)
            is Token.Parenthesis -> throw MismatchedParenthesisException()
            else -> throw AssemblyException("Unexpected operator.")
        }
    }

    return when {
        output.isEmpty() -> EmptyExpression
        output.size == 1 -> output.pop()
        else -> throw AssemblyException("Unexpected error: Result is not single expression.")
    }
}

fun applyOperatorToExpressions(token: Token.Operator, expressions: Stack<Expression>) {
    if (token is Token.UnaryOperator) {
        val operand = expressions.popOrNull()
        val expr = when(operand) {
            null -> throw AssemblyException("Can not apply operator " + token.presentation() + " without operand.")
            else -> applyUnaryOperator(token, operand)
        }
        expressions.add(expr)
    } else if (token is Token.BinaryOperator) {
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
}

