package symbolic.expressions

import symbolic.parser.Token
import symbolic.util.isDivisible
import java.util.*

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

fun Expression.text(): String = when(this) {
    is Integer -> this.value.toString()
    is Decimal -> this.value.toString()
    is Variable -> this.value
    is Negation -> "-${this.expression.text()}"
    is Parentheses -> "(${this.expr.text()})"
    is ConstantCoefficientTerm -> product(listOf(this.coeff, this.term)).text()
    is Sum -> terms
            .map(applyParenthesesIfNecessary(this))
            .fold("", { accumulated, term ->
                accumulated + when {
                    accumulated.isEmpty() -> term.text()
                    term is Negation -> " - ${term.expression.text()}"
                    term is Integer && term.value < 0 -> " - " + Integer(-1 * term.value).text()
                    else -> " + " + term.text()
                }
            })
    is Product -> terms
            .map(applyParenthesesIfNecessary(this))
            .map(Expression::text)
            .reduce { a, b -> "$a * $b" }
    is Division -> listOf(left, right)
            .map(applyParenthesesIfNecessary(this))
            .map(Expression::text)
            .reduce { a, b -> "$a / $b" }
    else -> ""
}

fun Sum.flatten(): Sum = Sum(terms.flatMap { if (it is Sum) it.terms else listOf(it) })
fun Product.flatten(): Product = Product(terms.flatMap { if (it is Product) it.terms else listOf(it) })

private fun List<Expression>.combineConstants(combiner: (Expression, Expression) -> Expression,
                                      initializer: (List<Expression>) -> Expression): Expression {
    val (constants, remaining) = this.partition { it is Constant }
    val factor = constants.fold(EmptyExpression as Expression, combiner)
    return initializer(listOf(factor) + remaining)
}

fun Expression.simplify(): Expression = when(this) {
    is Negation -> if (this.expression is Constant) { Integer(-1) * this.expression } else { this }
    is ConstantCoefficientTerm -> ConstantCoefficientTerm(this.coeff, this.term.simplify())
    is Sum -> this.flatten().terms
            .map { it.simplify() }
            .combineConstants(Expression::plus, ::sum)
            .let {
                if (it is Sum) { it.collectTerms() } else { it }
            }
    is Product -> this.flatten().terms
            .map { it.simplify() }
            .combineConstants(Expression::times, ::product)
    is Division -> when {
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
    else -> this
}

private fun makeConstantCoefficientTerm(e: Expression): Expression = when {
    e is Product && e.terms.size > 1 -> {
        val coeff = e.terms.first()
        if (coeff is Constant) { ConstantCoefficientTerm(coeff, product(e.terms.drop(1))) } else { e }
    }
    e is Negation -> {
        val c = makeConstantCoefficientTerm(e.expression)
        if (c is ConstantCoefficientTerm) {
            val coeff = (c.coeff * Integer(-1)) as Constant
            ConstantCoefficientTerm(coeff, c.term)
        } else {
            e
        }
    }
    else -> e
}

private fun reduceConstantCoefficientTermToProduct(e: Expression) =
        if (e is ConstantCoefficientTerm) { Product(e.coeff, e.term).simplify() } else { e }

fun Sum.collectTerms(): Expression {
    val collectedTerms = terms
            .map(::makeConstantCoefficientTerm)
            .let {
                val coefficientTable = mutableMapOf<Expression, Constant>()
                val (ccterms, remainingTerms) = it.partition { it is ConstantCoefficientTerm }

                ccterms.forEach {
                    it as ConstantCoefficientTerm
                    val currentCoefficient = coefficientTable.getOrElse(it.term, { Integer(0) })
                    val newCoefficient = currentCoefficient + it.coeff
                    coefficientTable.put(it.term, newCoefficient as Constant)
                }

                coefficientTable.asIterable()
                        .map { it -> ConstantCoefficientTerm(it.value, it.key) }
                        .plus(remainingTerms)
            }.map(::reduceConstantCoefficientTermToProduct)
    return sum(collectedTerms)
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
