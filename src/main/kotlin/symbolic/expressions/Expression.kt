package symbolic.expressions

interface Expression {
    fun text(): String
    fun simplify(): Expression = this
}