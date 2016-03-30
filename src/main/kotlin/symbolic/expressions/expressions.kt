package symbolic.expressions

import symbolic.parser.Token
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

interface Operator : Expression {
    fun token() : Token.Operator
}

data class Negation(val expression: Expression) : Expression {
    override fun text() = "-${expression.text()}"
}

interface BinaryOperator : Operator {
    companion object {
        fun fromToken(token: Token.BinaryOperator, left: Expression, right: Expression) = when(token) {
            is Token.BinaryOperator.Plus -> Sum(left, right)
            is Token.BinaryOperator.Times -> Product(left, right)
            is Token.BinaryOperator.Minus -> Sum(left, Negation(right))
            is Token.BinaryOperator.Division -> Division(left, right)
        }
    }
}

data class Sum(val terms: Iterable<Expression>) : BinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())

    override fun token() = Token.BinaryOperator.Plus
    override fun text(): String {
        val firstTerm = terms.take(1)
                .map(applyParentheses(this))
                .map(Expression::text)

        val remainingTerms = terms.drop(1)
                .map(applyParentheses(this))
                .map {
                    when {
                        it is Negation -> " - ${it.expression.text()}"
                        it is Integer && it.value < 0 -> " - " + Integer(-1 * it.value).text()
                        else -> " + ${it.text()}"
                    }
                }

        return (firstTerm + remainingTerms).reduce { a, b -> a + b }
    }

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

data class Product(val terms: Iterable<Expression>) : BinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())

    override fun token() = Token.BinaryOperator.Times
    override fun text() = terms
            .map(applyParentheses(this))
            .map(Expression::text)
            .reduce { a, b -> "$a * $b" }

    override fun simplify(): Expression = terms.reduce { left, right ->
        when {
            left is Integer && right is Integer -> Integer(left.value * right.value)
            left is Decimal && right is Decimal -> Decimal(left.value * right.value)
            left is Decimal && right is Integer -> Decimal(left.value * right.value)
            left is Integer && right is Decimal -> Decimal(left.value * right.value)
            else -> {
                val simplified = Product(left.simplify(), right.simplify())
                if (simplified != this) simplified.simplify() else simplified
            }
        }
    }
}

data class Division(val left: Expression, val right: Expression) : BinaryOperator {
    override fun text() = listOf(left, right)
            .map(applyParentheses(this))
            .map(Expression::text)
            .reduce { a, b -> "$a / $b" }

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

data class Parentheses(val expr: Expression) : Expression {
    override fun text() = "(${expr.text()})"
}

private fun applyParentheses(parentOperator: Operator) = { expr: Expression -> when {
    expr is Operator && expr.token().precedence() < parentOperator.token().precedence() -> Parentheses(expr)
    else -> expr
}}

fun applyUnaryOperator(token: Token.UnaryOperator, operand: Expression) = when(token) {
    is Token.UnaryOperator.Plus -> operand
    is Token.UnaryOperator.Minus -> when(operand) {
        is Integer -> Integer(-1 * operand.value)
        is Decimal -> Decimal(-1 * operand.value)
        else -> Negation(operand)
    }
}