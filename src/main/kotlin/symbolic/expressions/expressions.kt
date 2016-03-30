package symbolic.expressions

import symbolic.parser.Token
import symbolic.util.gcd
import symbolic.util.isDivisible

object EmptyExpression : Expression {
    override fun text() = ""
}

interface Constant : Expression {
    companion object {
        fun fromToken(token: Token.Constant) = when(token) {
            is Token.Integer -> Integer(token.value)
            is Token.Decimal -> Decimal(token.value)
            else -> throw Exception("Unsupported Constant token")
        }
    }
}

data class Variable(val value: String) : Expression {
    override fun text() = value

    companion object {
        fun fromToken(token: Token.Name) = Variable(token.value)
    }
}

data class Integer(val value: Int) : Constant {
    override fun text() = value.toString()
}
data class Decimal(val value: Double) : Constant {
    override fun text() = value.toString()
}

interface Operator : Expression

data class Negation(val expression: Expression) : Expression {
    override fun text() = "-${expression.text()}"
}

interface BinaryOperator : Operator {
    fun token() : Token.BinaryOperator

    companion object {
        fun fromToken(token: Token.BinaryOperator, left: Expression, right: Expression) = when(token) {
            is Token.BinaryOperator.Plus -> Sum(left, right)
            is Token.BinaryOperator.Times -> BinaryProduct(left, right)
            is Token.BinaryOperator.Minus -> Sum(left, Negation(right))
            is Token.BinaryOperator.Division -> Division(left, right)
        }
    }
}

data class Sum(val terms: Iterable<Expression>) : BinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())

    override fun token() = Token.BinaryOperator.Plus
    override fun text() = terms.firstOrNull()?.text() + terms.drop(1).map {
        when {
            it is Negation -> " - ${it.expression.text()}"
            it is Integer && it.value < 0 -> " - " + Integer(-1 * it.value).text()
            else -> " + ${it.text()}"
        }
    }.reduce { a, b -> a + b }

    override fun simplify(): Expression = terms.reduce { left, right ->
        when {
            left is Integer && right is Integer -> Integer(left.value+right.value)
            left is Decimal && right is Decimal -> Decimal(left.value+right.value)
            left is Decimal && right is Integer -> Decimal(left.value+right.value)
            left is Integer && right is Decimal -> Decimal(left.value+right.value)
            else -> {
                val simplified = Sum(left.simplify(), right.simplify())
                if (simplified != this) simplified.simplify() else simplified
            }
        }
    }
}

data class BinaryProduct(val left: Expression, val right: Expression) : BinaryOperator {
    override fun text() = when {
        left is Integer && left.value == -1 -> "-${right.text()}"
        else -> applyParentheses(this, left, right)
    }

    override fun simplify(): Expression =
            when {
                left is Integer && right is Integer -> Integer(left.value * right.value)
                left is Decimal && right is Decimal -> Decimal(left.value * right.value)
                left is Decimal && right is Integer -> Decimal(left.value * right.value)
                left is Integer && right is Decimal -> Decimal(left.value * right.value)
                else -> {
                    val simplified = BinaryProduct(left.simplify(), right.simplify())
                    if (simplified != this) simplified.simplify() else simplified
                }
            }

    override fun token() = Token.BinaryOperator.Times
}

data class Division(val left: Expression, val right: Expression) : BinaryOperator {
    override fun text() = applyParentheses(this, left, right)
    override fun token() = Token.BinaryOperator.Division

    override fun simplify(): Expression = when {
        left is Integer && right is Integer && right != Integer(0) && isDivisible(left.value, right.value) ->
            Integer(left.value / right.value)
        left is Decimal && right is Decimal && right != Decimal(0.0) -> Decimal(left.value / right.value)
        left is Decimal && right is Integer && right != Integer(0) -> Decimal(left.value / right.value)
        left is Integer && right is Decimal && right != Decimal(0.0) -> Decimal(left.value / right.value)
        else -> {
            val simplified = Division(left.simplify(), right.simplify())
            if (simplified != this) simplified.simplify() else simplified
        }
    }
}

private fun applyParentheses(operator: BinaryOperator, left: Expression, right: Expression): String {
    var leftString = left.text()
    var rightString = right.text()

    if (left is BinaryOperator && left.token().precedence() < operator.token().precedence()) {
        leftString = "(" + leftString + ")"
    }

    if (right is BinaryOperator && right.token().precedence() < operator.token().precedence()) {
        rightString = "(" + rightString + ")"
    }

    return "$leftString ${operator.token().presentation()} $rightString"
}

fun applyUnaryOperator(token: Token.UnaryOperator, operand: Expression) = when(token) {
    is Token.UnaryOperator.Plus -> operand
    is Token.UnaryOperator.Minus -> when(operand) {
        is Integer -> Integer(-1 * operand.value)
        is Decimal -> Decimal(-1 * operand.value)
        else -> Negation(operand)
    }
}