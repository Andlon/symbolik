package symbolic.parser

interface Token {
    interface Constant : Token
    data class Integer(val value: Int) : Constant
    data class Decimal(val value: Double) : Constant
    data class Name(val value: String) : Token
    object Times : Token
    object Plus : Token
    object Minus : Token
}

fun isValidName(str: String) =
        str.isNotEmpty() && str.first().isLetter() && str.all { it.isLetterOrDigit() }

fun isValidInteger(str: String) = str.isNotEmpty() && str.all { it.isDigit() }

fun isValidDecimal(str: String): Boolean {
    // TODO: Implement scientific notation support?
    val dotCount = str.count { it == '.' }
    val charactersAreAcceptable = str.all { it.isDigit() || it == '.' }
    val lastCharacterIsDigit = str.lastOrNull()?.isDigit()
    return str.isNotEmpty() && charactersAreAcceptable && (lastCharacterIsDigit == true) && dotCount <= 1
}





