package symbolic.parser

interface Token {
    interface Constant : Token

    data class Integer(val value: Int) : Constant
    data class Decimal(val value: Double) : Constant

    data class Name(val value: String) : Token

    interface Paranthesis : Token
    object LeftParanthesis : Paranthesis
    object RightParanthesis : Paranthesis

    interface BinaryOperator : Token {
        enum class Associativity {
            Left,
            Right,
            Both
        }

        open fun associativity(): Associativity
        open fun precedence(): Int
        open fun presentation(): String

        fun isLeftAssociative() = when (associativity()) {
            Associativity.Left -> true
            Associativity.Right -> false
            Associativity.Both -> true
        }

        fun isRightAssociative() = when (associativity()) {
            Associativity.Left -> false
            Associativity.Right -> true
            Associativity.Both -> true
        }
    }

    object Plus : BinaryOperator {
        override fun associativity() =  BinaryOperator.Associativity.Both
        override fun precedence() = 2
        override fun presentation() = "+"
    }

    object Minus : BinaryOperator {
        override fun associativity() =  BinaryOperator.Associativity.Left
        override fun precedence() = 2
        override fun presentation() = "-"
    }

    object Times : BinaryOperator {
        override fun associativity() =  BinaryOperator.Associativity.Both
        override fun precedence() = 3
        override fun presentation() = "*"
    }

    object Division : BinaryOperator {
        override fun associativity() = BinaryOperator.Associativity.Left
        override fun precedence() = 3
        override fun presentation() = "/"
    }
}

private val integerValidator = Regex("[0-9]+")
private val decimalValidator = Regex("[0-9]*[.]*[0-9]+")
private val nameValidator = Regex("[a-zA-Z][a-zA-Z0-9_]*")

// TODO: Implement scientific notation support for integer/decimal?
fun isValidName(str: String) = nameValidator.matches(str)
fun isValidInteger(str: String) = integerValidator.matches(str)
fun isValidDecimal(str: String) = decimalValidator.matches(str)








