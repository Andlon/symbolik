package symbolic.parser

interface Token {
    interface Constant : Token

    data class Integer(val value: Int) : Constant
    data class Decimal(val value: Double) : Constant

    data class Name(val value: String) : Token

    interface BinaryOperator : Token {
        enum class Associativity {
            Left,
            Right,
            Both
        }

        open fun associativity(): Associativity
        open fun precedence(): Int

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
    }

    object Minus : BinaryOperator {
        override fun associativity() =  BinaryOperator.Associativity.Left
        override fun precedence() = 2
    }

    object Times : BinaryOperator {
        override fun associativity() =  BinaryOperator.Associativity.Both
        override fun precedence() = 3
    }

    object Division : BinaryOperator {
        override fun associativity() = BinaryOperator.Associativity.Left
        override fun precedence() = 3
    }
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





