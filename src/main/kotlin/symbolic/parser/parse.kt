package symbolic.parser

fun tokenize(str: String) : List<Token> = recursivelyTokenize(emptyList(), str)

tailrec private fun recursivelyTokenize(tokens: List<Token>, remaining: String): List<Token> {
    // By ignoring leading whitespace we effectively make whitespace only work as separators of tokens
    val trimmedRemaining = remaining.trimStart()
    val result = longestTokenWithRemainder(trimmedRemaining)
    return when (result) {
        null -> tokens
        else -> recursivelyTokenize(tokens + result.token, result.remainder)
    }
}

private fun isValidName(str: String) =
        str.isNotEmpty() && str.first().isLetter() && str.all { it.isLetterOrDigit() }

private fun isValidInteger(str: String) = str.isNotEmpty() && str.all { it.isDigit() }

// TODO: Implement scientific notation support?
private fun isValidDecimal(str: String) =
        str.isNotEmpty() && str.all { it.isLetter() || it == '.' } && str.count { it == '.' } <= 1

private fun parseSingleToken(str: String): Token? =
    when (str) {
        "*" -> Token.Times
        "+" -> Token.Plus
        "-" -> Token.Minus
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
                .dropWhile { it == null }
                .takeWhile { it != null }
                .lastOrNull()
