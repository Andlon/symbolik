package symbolic.expressions

import symbolic.parser.Token

interface Constant : Expression {
    fun decimalValue(): Decimal

    companion object {
        fun fromToken(token: Token.Constant) = when(token) {
            is Token.Integer -> Integer(token.value)
            is Token.Decimal -> Decimal(token.value)
            else -> throw Exception("Unsupported Constant token")
        }
    }
}

interface Operator : Expression {
    fun token() : Token.Operator
}

interface BinaryOperator : Operator {
    companion object {
        fun fromToken(token: Token.BinaryOperator, left: Expression, right: Expression) = when (token) {
            is Token.BinaryOperator.Plus -> when {
                left is Sum && right is Sum -> Sum(left.terms + right.terms)
                left is Sum -> Sum(left.terms + right)
                right is Sum -> Sum(listOf(left) + right.terms)
                else -> Sum(left, right)
            }
            is Token.BinaryOperator.Times -> when {
                left is Product && right is Product -> Product(left.terms + right.terms)
                left is Product -> Product(left.terms + right)
                right is Product -> Product(listOf(left) + right.terms)
                else -> Product(left, right)
            }
            is Token.BinaryOperator.Minus -> Sum(left, Negation(right))
            is Token.BinaryOperator.Division -> Division(left, right)
        }
    }
}

interface AssociativeBinaryOperator : BinaryOperator {
    val terms: List<Expression>
}

object EmptyExpression : Expression
data class Parentheses(val expr: Expression) : Expression
data class Negation(val expression: Expression) : Operator {
    override fun token() = Token.UnaryOperator.Minus
}

data class Variable(val value: String) : Expression {
    companion object {
        fun fromToken(token: Token.Name) = Variable(token.value)
    }
}

data class Integer(val value: Int) : Constant {
    override fun decimalValue() = Decimal(value.toDouble())
}
data class Decimal(val value: Double) : Constant {
    override fun decimalValue() = this
}

data class Sum(override val terms: List<Expression>) : AssociativeBinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())
    override fun token() = Token.BinaryOperator.Plus
}

data class Product(override val terms: List<Expression>) : AssociativeBinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())
    override fun token() = Token.BinaryOperator.Times
}

data class Division(val left: Expression, val right: Expression) : BinaryOperator {
    override fun token() = Token.BinaryOperator.Division
}