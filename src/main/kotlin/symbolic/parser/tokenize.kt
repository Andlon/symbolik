package symbolic.parser

class TokenizationException(message: String) : Exception(message)

fun tokenize(str: String) : List<Token> = processUnaryOperators(recursivelyTokenize(emptyList(), str))

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

private fun processUnaryOperators(tokens: List<Token>): List<Token> {
    val firstElement = tokens.take(1)
            .map {
                when(it) {
                    is Token.BinaryPlus -> Token.UnaryOperator.Plus
                    is Token.BinaryMinus -> Token.UnaryOperator.Minus
                    else -> it
                }
            }

    val remainingElements = tokens.zip(tokens.drop(1))
            .map {
                when (it.first) {
                    is Token.Constant -> it.second
                    is Token.Name -> it.second
                    else -> when(it.second) {
                        is Token.BinaryPlus -> Token.UnaryOperator.Plus
                        is Token.BinaryMinus -> Token.UnaryOperator.Minus
                        else -> it.second
                    }
                }
            }

    return firstElement + remainingElements
}

private fun parseSingleToken(str: String): Token? =
        when (str) {
            "*" -> Token.Times
            "+" -> Token.BinaryPlus
            "-" -> Token.BinaryMinus
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