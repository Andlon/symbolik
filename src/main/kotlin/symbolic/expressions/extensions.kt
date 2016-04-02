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
    this is Integer && other is Variable && this.value == 1 -> other
    this is Variable && other is Integer && other.value == 1 -> this
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

fun Expression.expand(): Expression = when(this) {
    is Product -> this.terms.fold(EmptyExpression as Expression, { acc, term ->
        when {
            acc is EmptyExpression -> term
            term is Sum -> Sum(term.terms.map { Product(acc, it).expand() }).flatten()
            acc is Sum -> Sum(acc.terms.map { Product(it, term).expand() }).flatten()
            else -> Product(acc, term).flatten()
        }
    })
    is Sum -> sum(this.terms.map { it.expand() })
    is Negation -> Product(Integer(-1), this.expression).expand()
    else -> this
}

private fun Expression.multiplyTerms(): Expression = when(this) {
    is Product -> this.combineTerms(Expression::times, ::product)
    is Sum -> sum(this.terms.map { it.multiplyTerms() })
    else -> this
}

private fun Expression.sumTerms(): Expression = when(this) {
    is Product -> product(this.terms.map { it.sumTerms() })
    is Sum -> this.combineTerms(Expression::plus, ::sum)
    else -> this
}

private fun Expression.combineTerms(combiner: (Expression, Expression) -> Expression,
                                          initializer: (List<Expression>) -> Expression): Expression {
    return if (this is AssociativeBinaryOperator) {
        val (constants, remaining) = this.terms.partition { it is Constant }
        val factor = constants.fold(EmptyExpression as Expression, combiner)
        initializer(listOf(factor) + remaining)
    } else { this }
}

fun Expression.simplify(): Expression = when(this) {
    is Negation -> if (this.expression is Constant) { Integer(-1) * this.expression } else { this }
    is Sum -> this.flatten()
            .sumTerms()
            .let {
                val collected = it.collect()
                val expandedAndCollected = it.expand().collect()
                if(collected.complexity() < expandedAndCollected.complexity()) { collected }
                else { expandedAndCollected }
            }
            .sumTerms()
            .multiplyTerms()
    is Product -> this.flatten()
            .multiplyTerms()
            .let {
                val collected = it.collect()
                val expandedAndCollected = it.expand().collect()
                if(collected.complexity() < expandedAndCollected.complexity()) { collected }
                else { expandedAndCollected }
            }
            .multiplyTerms()
            .sumTerms()
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

fun Expression.complexity(): Int = when(this) {
    is Constant -> 1
    is Variable -> 2
    is Negation -> 1
    is Sum -> this.terms.fold(0, { acc, term -> acc + term.complexity() }) + 2 * (this.terms.size - 1)
    is Product -> this.terms.fold(0, { acc, term -> acc + term.complexity() }) + 1 * (this.terms.size - 1)
    else -> 0
}

fun product(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Product(it).flatten()
        }}

fun sum(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Sum(it).flatten()
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

fun Expression.collect(): Expression = when(this) {
    is Sum -> factors(this).let { it.forEach { println(it.expression.text()) }; it }.minBy { it.expression.complexity() }?.expression ?: EmptyExpression
    else -> this
}

data class FactorizedExpression(val factor: Expression, val operand: Expression, val remainder: Expression) {
    val expression by lazy { sum(listOf(product(listOf(factor, operand)), remainder)) }
}

fun factors(expr: Sum): List<FactorizedExpression> {
    val factors = expr.terms.flatMap { when(it) {
        is Product -> it.terms
        else -> listOf(it)
    } }.distinct()

    data class IntermediateTermCollection(val factoredTerms: MutableList<Expression>,
                                          val remainderTerms: MutableList<Expression>)

    val factorTable = factors.associate { it to IntermediateTermCollection(mutableListOf(), mutableListOf()) }
    for (factor in factors) {
        val intermediateCollection = factorTable[factor]!!

        for (term in expr.terms) {
            when {
                term is Product && term.terms.contains(factor) ->
                    intermediateCollection.factoredTerms.add(product(term.terms - factor))
                term is Product -> intermediateCollection.remainderTerms.add(term)
                term == factor -> intermediateCollection.factoredTerms.add(Integer(1))
                else -> intermediateCollection.remainderTerms.add(term)
            }
        }
    }

    fun Expression.removeFactor(factor: Expression): Expression = when (this) {
        is Product -> product(this.terms - factor)
        else -> this
    }

    var adjustedTable = factorTable
    for ((factor1, intermediate1) in factorTable) {
        for ((factor2, intermediate2) in factorTable) {
            if (factor1 != factor2 && intermediate1.remainderTerms == intermediate2.remainderTerms) {
                val compositeFactoredTerms = intermediate1.factoredTerms.map { it.removeFactor(factor2) }
                val newIntermediate = IntermediateTermCollection(
                        compositeFactoredTerms.toMutableList(),
                        intermediate1.remainderTerms)
                val newFactor = product(listOf(factor1, factor2))
                adjustedTable = adjustedTable.filterNot { it.key in listOf(factor1, factor2) }
                    .plus((newFactor to newIntermediate))
            }
        }
    }

    return adjustedTable.asIterable()
        .map {
            val factoredSum = sum(it.value.factoredTerms)
                    .multiplyTerms()
                    .sumTerms()
            val remainderSum = sum(it.value.remainderTerms)
                    .multiplyTerms()
                    .sumTerms()
            FactorizedExpression(it.key, factoredSum, remainderSum)
        }
}
