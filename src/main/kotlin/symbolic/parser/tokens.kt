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





