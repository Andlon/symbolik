package symbolic.expressions

import symbolic.parser.Token

object EmptyExpression : Expression

interface Constant : Expression {
    companion object {
        fun fromToken(token: Token.Constant) = when(token) {
            is Token.Integer -> Integer(token.value)
            is Token.Decimal -> Decimal(token.value)
            else -> throw Exception("Unsupported Constant token")
        }
    }
}

data class Variable(private val value: String) : Expression {
    companion object {
        fun fromToken(token: Token.Name) = Variable(token.value)
    }
}

data class Integer(val value: Int) : Constant
data class Decimal(val value: Double) : Constant

interface BinaryOperator : Expression {
    companion object {
        fun fromToken(token: Token.BinaryOperator, left: Expression, right: Expression) = when(token) {
            is Token.Plus -> BinarySum(left, right)
            is Token.Times -> BinaryProduct(left, right)
            is Token.Minus -> BinarySum(left, BinaryProduct(Integer(-1), right))
            else -> throw Exception("Unsupported binary operator")
        }
    }
}

data class BinarySum(val left: Expression, val right: Expression) : BinaryOperator
data class BinaryProduct(val left: Expression, val right: Expression) : BinaryOperator