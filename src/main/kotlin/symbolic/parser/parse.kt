package symbolic.parser

import symbolic.expressions.*
import symbolic.util.popOrNull
import symbolic.util.popWhile
import java.util.*

class TokenizationException(message: String) : Exception(message)

open class AssemblyException(message: String) : Exception(message)
class MismatchedParanthesisException : AssemblyException("Mismatched paranthesis")

fun tokenize(str: String) : List<Token> = recursivelyTokenize(emptyList(), str)

tailrec private fun recursivelyTokenize(tokens: List<Token>, remaining: String): List<Token> {
    // By ignoring leading whitespace we effectively make whitespace only work as separators of tokens
    val trimmedRemaining = remaining.trimStart()
    if (trimmedRemaining.isEmpty())
        return tokens

    val result = longestTokenWithRemainder(trimmedRemaining)
    return when (result) {
        null -> throw TokenizationException("Invalid token " + remaining)
        else -> recursivelyTokenize(tokens + result.token, result.remainder)
    }
}

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

private fun parseSingleToken(str: String): Token? =
        when (str) {
            "*" -> Token.Times
            "+" -> Token.Plus
            "-" -> Token.Minus
            "/" -> Token.Division
            "(" -> Token.LeftParanthesis
            ")" -> Token.RightParanthesis
            else -> when {
                isValidName(str) -> Token.Name(str)
                isValidInteger(str) -> Token.Integer(str.toInt())
                isValidDecimal(str) -> Token.Decimal(str.toDouble())
                else -> null
            }
        }

private data class TokenWithRemainder(val token: Token, val remainder: String)
private fun createTokenWithRemainder(token: Token?, remainder: String) =
        when(token) {
            null -> null
            else -> TokenWithRemainder(token, remainder)
        }

private fun longestTokenWithRemainder(str: String): TokenWithRemainder? =
        (1 .. str.length)
                .map { numChars -> createTokenWithRemainder(parseSingleToken(str.take(numChars)), str.drop(numChars)) }
                .filterNotNull()
                .lastOrNull()