package symbolic.parser

import symbolic.expressions.Expression

/**
 * Created by andreas on 27.03.16.
 */

fun tokenize(str: String) : List<Token> = listOf<Token>(Token.IntegerConstant(5))

fun parse(str: String) : Expression =
    object : Expression {

    }