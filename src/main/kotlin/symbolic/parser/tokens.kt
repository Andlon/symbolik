package symbolic.parser

/**
 * Created by andreas on 27.03.16.
 */
interface Token {
    interface Constant : Token
    data class IntegerConstant(val value: Int) : Constant
    object Times : Token
    object Plus : Token
    object Minus : Token
}





