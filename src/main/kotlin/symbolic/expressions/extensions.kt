package symbolic.expressions

import symbolic.parser.Token
import symbolic.util.isDivisible
import symbolic.util.reduceOrNull
import symbolic.util.repeat
import java.util.*

operator fun Expression.plus(other: Expression): Expression = sum(this, other)
operator fun Expression.times(other: Expression) = product(this, other)

fun product(vararg terms: Expression) = product(terms.asIterable())
fun product(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Product(it).flatten()
        }}

fun sum(vararg terms: Expression) = sum(terms.asIterable())
fun sum(terms: Iterable<Expression>): Expression = terms
        .filterNot { it is EmptyExpression }
        .let { when (it.size) {
            0 -> EmptyExpression
            1 -> it.single()
            else -> Sum(it).flatten()
        }}

fun Expression.text(): String = when(this) {
    is Integer -> this.value.toString()
    is Decimal -> this.value.toString()
    is Variable -> this.value
    is Negation -> "-${this.expression.text()}"
    is Parentheses -> "(${this.expr.text()})"
    is ConstantCoefficientTerm -> product(listOf(this.coeff, this.term)).text()
    is Sum -> terms
            .map { applyParenthesesIfNecessary(this, it ) }
            .fold("", { accumulated, term ->
                accumulated + when {
                    accumulated.isEmpty() -> term.text()
                    term is Negation -> " - ${term.expression.text()}"
                    term is Integer && term.value < 0 -> " - " + Integer(-1 * term.value).text()
                    else -> " + " + term.text()
                }
            })
    is Product -> terms
            .map { applyParenthesesIfNecessary(this, it ) }
            .map(Expression::text)
            .reduce { a, b -> "$a * $b" }
    is Division -> listOf(left, right)
            .map { applyParenthesesIfNecessary(this, it ) }
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
    is Negation -> product(Integer(-1), this.expression).expand()
    else -> this
}

private fun Expression.multiplyTerms(): Expression = when(this) {
    is Product -> this.combineTermsWith({ a, b ->
        when {
            a is Integer && b is Integer -> Integer(a.value * b.value)
            a is Constant && b is Constant -> Decimal(a.decimalValue().value * b.decimalValue().value)
            else -> product(a, b)
        }
    }, ::product)
    is Sum -> sum(this.terms.map { it.multiplyTerms() })
    else -> this
}

private fun Expression.sumTerms(): Expression = when(this) {
    is Product -> product(this.terms.map { it.sumTerms() })
    is Sum -> this.combineTermsWith({ a, b ->
        when {
            a is Integer && b is Integer -> Integer(a.value + b.value)
            a is Constant && b is Constant -> Decimal(a.decimalValue().value + b.decimalValue().value)
        // This code is extremely verbose. TODO: Refactor
            else -> sum(a, b)
        }
    }, ::sum)
    else -> this
}

private fun Expression.combineTermsWith(combiner: (Expression, Expression) -> Expression,
                                        initializer: (List<Expression>) -> Expression): Expression {
    return if (this is AssociativeBinaryOperator) {
        val (constants, remaining) = this.terms.partition { it is Constant }
        val factor = constants.fold(EmptyExpression as Expression, combiner)
        val terms = when {
            this is Product && factor is Integer && factor.value == 1 -> remaining
            this is Product && factor is Integer && factor.value == 0 -> listOf(Integer(0))
            else -> listOf(factor) + remaining
        }

        initializer(terms)
    } else { this }
}

fun List<Integer>.sum(): Integer? = reduceOrNull { a, b -> Integer(a.value + b.value) }
fun List<Integer>.product(): Integer? = reduceOrNull  { a, b -> Integer(a.value * b.value) }
fun List<Decimal>.sum(): Decimal? = reduceOrNull { a, b -> Decimal(a.value + b.value) }
fun List<Decimal>.product(): Decimal? = reduceOrNull { a, b -> Decimal(a.value * b.value) }

fun Expression.combineTerms(): Expression = when(this) {
    is Product -> {
        val combinedTerms = terms.map { it.combineTerms() }
        val (constants, remainingTerms) = combinedTerms.partition { it is Constant }
        val integerFactor = constants.filterIsInstance<Integer>().product()
        val decimalFactor = constants.filterIsInstance<Decimal>().product()
        val remaining = product(remainingTerms)

        when {
            integerFactor?.value == 0 -> Integer(0)
            integerFactor?.value == 1 -> product(decimalFactor ?: EmptyExpression, remaining).combineTerms()
            decimalFactor == null -> product(integerFactor ?: EmptyExpression, remaining)
            integerFactor == null -> product(decimalFactor, remaining)
            else -> product(Decimal(integerFactor.value * decimalFactor.value), remaining)
        }
    }
    is Sum -> {
        val combinedTerms = terms.map { it.combineTerms() }
        val (constants, remainingTerms) = combinedTerms.partition { it is Constant }
        val integerSum = constants.filterIsInstance<Integer>().sum()
        val decimalSum = constants.filterIsInstance<Decimal>().sum()
        val remaining = sum(remainingTerms)

        when {
            integerSum?.value == 0 && remaining != EmptyExpression ->
                sum(decimalSum ?: EmptyExpression, remaining).combineTerms()
            decimalSum?.value == 0.0 -> sum(integerSum ?: EmptyExpression, remaining).combineTerms()
            decimalSum == null -> sum(integerSum ?: EmptyExpression, remaining)
            integerSum == null -> sum(decimalSum, remaining)
            else -> sum(Decimal(integerSum.value + decimalSum.value), remaining)
        }
    }
    is Negation -> {
        val combined = expression.combineTerms()
        if (combined is Constant) { product(Integer(-1), combined).combineTerms() }
        else { Negation(combined) }
    }
    else -> this
}

fun Expression.simplify(): Expression = when(this) {
    is Negation -> product(Integer(-1), this.expression).simplify()
    is Sum -> this.flatten()
            .let {
                val collected = it.collect()
                val expandedAndCollected = it.expand().collect()
                if(collected.complexity() < expandedAndCollected.complexity()) { collected }
                else { expandedAndCollected }
            }
            .combineTerms()
    is Product -> this.flatten()
            .let {
                val collected = it.collect()
                val expandedAndCollected = it.expand().collect()
                if(collected.complexity() < expandedAndCollected.complexity()) { collected }
                else { expandedAndCollected }
            }
            .combineTerms()
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
    is Negation -> 1 + this.expression.complexity()
    is Sum -> this.terms.fold(0, { acc, term -> acc + term.complexity() }) + 2 * (this.terms.size - 1)
    is Product -> this.terms.fold(0, { acc, term -> acc + term.complexity() }) + 1 * (this.terms.size - 1)
    else -> throw NotImplementedError("Complexity of expression cannot be determined")
}

private fun applyParenthesesIfNecessary(parentOperator: Operator, expr: Expression): Expression =
        when {
            expr is Operator && expr.token().precedence() < parentOperator.token().precedence() -> Parentheses(expr)
            expr is Negation
                    && expr.expression is Operator
                    && expr.expression.token().precedence() <= parentOperator.token().precedence()
            -> Negation(Parentheses(expr.expression))
            else -> expr
        }

fun applyUnaryOperator(token: Token.UnaryOperator, operand: Expression) = when(token) {
    is Token.UnaryOperator.Plus -> operand
    is Token.UnaryOperator.Minus -> when(operand) {
        is Integer -> Integer(-1 * operand.value)
        is Decimal -> Decimal(-1 * operand.value)
        else -> Negation(operand)
    }
}

fun Expression.collect(): Expression = when(this) {
    is Sum -> factors()
            .let {
                it
            }
            // Note: Instead of calling collect on the remainder, we should be able to compute
            // the same result on the remainder by using the provided factors. However,
            // it should yield the same result, so for now we take the easy route.
            .map { sum(product(it.factor, it.operand.collect()), it.remainder.collect()).combineTerms() }
            .minBy { it.complexity() }
            ?: this
    is Product -> product(terms.map { it.collect() })
    is Negation -> product(Integer(-1), this.expression).collect()
    else -> this
}

data class FactorizedExpression(val factor: Expression, val operand: Expression, val remainder: Expression = EmptyExpression) {
    //val expression by lazy { sum(product(factor, operand), remainder) }
}

private fun extractFactors(expr: Expression): List<Expression> = when (expr) {
    is Product -> expr.terms
    is Negation -> extractFactors(expr.expression)
    else -> listOf(expr)
}

/**
 * Computes the minimum multiplicity of the factor in the list of expressions.
 *
 * Given a sum
 * a_1 + a_2 + ... + a_n,
 * where
 * a_i = b_1 * b_2 * ... * b_m(i),
 * where m(i) is the number of factors in a_i, and
 * g(i) = #{ b_j == factor for j = 1, ..., m(i) },
 * then this function returns
 * multiplicity = min g(i) for i = [1, n]
 */
private fun multiplicity(factor: Expression, terms: List<Expression>): Int =
        terms.map {
            when (it) {
                factor -> 1
                is Product -> it.terms.count { it == factor }
                else -> 0
            }
        }.min() ?: 0

private fun Expression.removeFactors(factors: List<Expression>): Expression {
    var remainingFactors = factors.toMutableList()
    return when {
        this is Product -> terms.filterNot { remainingFactors.remove(it) }
                .let {
                    if (it.isEmpty()) { Integer(1) }
                    else { product(it) }
                }
        this is Negation -> Negation(expression.removeFactors(factors))
        factors.singleOrNull() == this -> Integer(1)
        else -> this
    }
}

fun Sum.factors(): List<FactorizedExpression> {
    val expr = this
    val factors = expr.terms
            .flatMap { extractFactors(it) }
            // Ignore constant terms, as they are not interesting to extract as factors
            .filterNot { it is Constant }
            .distinct()

    data class IntermediateTermCollection(val factoredTerms: MutableList<Expression>,
                                          val remainderTerms: MutableList<Expression>)

    val factorTable = factors.associate { it to IntermediateTermCollection(mutableListOf(), mutableListOf()) }
    for (factor in factors) {
        val intermediateCollection = factorTable[factor]!!

        for (term in expr.terms) {
            when {
                term == factor -> intermediateCollection.factoredTerms.add(Integer(1))
                term is Product && term.terms.contains(factor) ->
                    intermediateCollection.factoredTerms.add(Product(term.terms - factor).flatten())
                term is Product -> intermediateCollection.remainderTerms.add(term)
            // TODO: Rewrite this block as a function and call it recursively for Negation
                term is Negation && term.expression == factor -> intermediateCollection.factoredTerms.add(Negation(Integer(1)))
                term is Negation && term.expression is Product && term.expression.terms.contains(factor) ->
                    intermediateCollection.factoredTerms.add(Negation(product(term.expression.terms - factor)))
                term is Negation && term.expression is Product ->
                    intermediateCollection.remainderTerms.add(Negation(term.expression))
                else -> intermediateCollection.remainderTerms.add(term)
            }
        }
    }

    return factorTable
            .mapKeys {
                // Multiply the factor the maximum number of times, N, such that
                // it appears N times in each factored term. Note that in each factoredTerm it
                // only appears N - 1 times, as we've already factored out one
                val multiplicity = multiplicity(it.key, it.value.factoredTerms)
                repeat(multiplicity + 1, it.key)
            }
            .mapValues {
                val repeatedFactor = it.key
                // Adjust the factoredTerms accordingly, by removing the factor (N - 1) times
                val factoredTerms = it.value.factoredTerms.map { it.removeFactors(repeatedFactor.drop(1)) }
                IntermediateTermCollection(factoredTerms.toMutableList(), it.value.remainderTerms)
            }.mapKeys { product(it.key) }
            .asIterable()
            .groupBy { it.value.remainderTerms }
            .map {
                val remainder = sum(it.key)
                val individualFactors = it.value.map { it.key }
                val compositeFactor = product(individualFactors)
                val refactoredTerms = it.value.map {
                    val factor = it.key
                    it.value.factoredTerms.map { it.removeFactors(individualFactors - factor) }
                }.firstOrNull() ?: listOf(EmptyExpression)
                val operand = sum(refactoredTerms)
                FactorizedExpression(compositeFactor, operand, remainder)
            }
}
