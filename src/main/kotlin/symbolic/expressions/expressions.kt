package symbolic.expressions

import symbolic.parser.Token
import symbolic.util.isDivisible
import java.util.*

object EmptyExpression : Expression {
    override fun text() = ""
}

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

operator fun Expression.plus(other: Expression) = when {
    this is Integer && other is Integer -> Integer(this.value + other.value)
    this is Constant && other is Constant -> Decimal(this.decimalValue().value + other.decimalValue().value)
    this is EmptyExpression -> other
    other is EmptyExpression -> this
    else -> Sum(this, other)
}

operator fun Expression.times(other: Expression) = when {
    this is Integer && other is Integer -> Integer(this.value * other.value)
    this is Constant && other is Constant -> Decimal(this.decimalValue().value * other.decimalValue().value)
    this is EmptyExpression -> other
    other is EmptyExpression -> this
    else -> Product(this, other)
}

data class Variable(val value: String) : Expression {
    override fun text() = value

    companion object {
        fun fromToken(token: Token.Name) = Variable(token.value)
    }
}

data class Integer(val value: Int) : Constant {
    override fun text() = value.toString()
    override fun decimalValue() = Decimal(value.toDouble())
}
data class Decimal(val value: Double) : Constant {
    override fun text() = value.toString()
    override fun decimalValue() = this
}

interface Operator : Expression {
    fun token() : Token.Operator
}

data class Negation(val expression: Expression) : Expression {
    override fun text() = "-${expression.text()}"
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

interface SuperBinaryOperator : BinaryOperator {
    val terms: Iterable<Expression>
}

data class Sum(override val terms: Iterable<Expression>) : SuperBinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())

    override fun token() = Token.BinaryOperator.Plus
    override fun text() = terms
            .map(applyParenthesesIfNecessary(this))
            .fold("", { accumulated, term ->
                accumulated + when {
                    accumulated.isEmpty() -> term.text()
                    term is Negation -> " - ${term.expression.text()}"
                    term is Integer && term.value < 0 -> " - " + Integer(-1 * term.value).text()
                    else -> " + " + term.text()
                }
            })

    override fun simplify(): Expression = terms
            .map { it.simplify() }
            .flatMap { if (it is Sum) it.terms else listOf(it) }
            .partition { it is Constant }
            .let {
                val factor = it.first.fold(EmptyExpression as Expression, { a, b -> a + b })
                return sum(listOf(factor) + it.second)
            }
}

data class Product(override val terms: List<Expression>) : SuperBinaryOperator {
    constructor(vararg terms: Expression) : this(terms.asList())

    override fun token() = Token.BinaryOperator.Times
    override fun text() = terms
            .map(applyParenthesesIfNecessary(this))
            .map(Expression::text)
            .reduce { a, b -> "$a * $b" }

    override fun simplify(): Expression = terms
            .map { it.simplify() }
            .flatMap { if (it is Product) it.terms else listOf(it) }
            .partition { it is Constant }
            .let {
                val factor = it.first.fold(EmptyExpression as Expression, { a, b -> a * b })
                return product(listOf(factor) + it.second)
            }
}

data class Division(val left: Expression, val right: Expression) : BinaryOperator {
    override fun text() = listOf(left, right)
            .map(applyParenthesesIfNecessary(this))
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

object ExpressionTypeComparator : Comparator<Expression> {
    override fun compare(a: Expression, b: Expression) = when {
        a is Integer && b !is Variable -> -1
        b is Integer && a !is Variable -> 1
        a is Constant && b !is Constant -> -1
        b is Constant && a !is Constant -> 1
        a is Variable && b !is Variable -> -1
        b is Variable && a !is Variable -> 1
        else -> 0
    }
}

fun product(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Product(it)
        }}

fun sum(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Sum(it)
        }}

private fun applyParenthesesIfNecessary(parentOperator: Operator) = { expr: Expression -> when {
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