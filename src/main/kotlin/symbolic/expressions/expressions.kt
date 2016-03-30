package symbolic.expressions

import symbolic.parser.Token

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

interface BinaryOperator : Expression {
    fun token() : Token.BinaryOperator

    companion object {
        fun fromToken(token: Token.BinaryOperator, left: Expression, right: Expression) = when(token) {
            is Token.BinaryOperator.Plus -> BinarySum(left, right)
            is Token.BinaryOperator.Times -> BinaryProduct(left, right)
            is Token.BinaryOperator.Minus -> BinarySum(left, BinaryProduct(Integer(-1), right))
            is Token.BinaryOperator.Division -> Division(left, right)
        }
    }
}

data class BinarySum(val left: Expression, val right: Expression) : BinaryOperator {
    override fun text() = when {
        right is BinaryProduct && right.left is Integer && right.left.value == -1 ->
            left.text() + " - " + right.right.text()
        right is Integer && right.value < 0 -> left.text() + " - " + (-1 * right.value)
        else -> applyParentheses(this, left, right)
    }

    override fun token() = Token.BinaryOperator.Plus
    override fun simplify(): Expression =
            when {
                left is Integer && right is Integer -> Integer(left.value + right.value)
                left is Decimal && right is Decimal -> Decimal(left.value + right.value)
                left is Decimal && right is Integer -> Decimal(left.value + right.value)
                left is Integer && right is Decimal -> Decimal(left.value + right.value)
                else -> {
                    val simplified = BinarySum(left.simplify(), right.simplify())
                    if (simplified != this) simplified.simplify() else simplified
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
        //TODO: Figure out if numbers are divisible left is Integer && right is Integer && right != Integer(0) -> Integer(left.value / right.value)
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
        else -> BinaryProduct(Integer(-1), operand)
    }
}