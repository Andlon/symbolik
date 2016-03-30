package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import symbolic.expressions.*

class ExpressionSpec : Spek() {
    init {
        given("the textual representations of expressions") {
            val x = Variable("x")
            on("a single variable") {
                it("should equal the name of the variable") {
                    shouldEqual("x", x.text())
                }
            }
            on("a single integer") {
                it("should equal the integer") {
                    shouldEqual("3", Integer(3).text())
                }
            }
            on("a single decimal") {
                it("should equal the decimal") {
                    shouldEqual("5.3", Decimal(5.3).text())
                }
            }
            on("a product of an integer and a variable") {
                it("should equal the product") {
                    shouldEqual("5 * x", Product(Integer(5), x).text())
                }
            }
            on("a sum of an integer and a variable") {
                it("should equal the sum") {
                    shouldEqual("5 + x", Sum(Integer(5), x).text())
                }
            }
            on("a negated variable subtracted from an integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - x", Sum(Integer(5), Negation(x)).text())
                }
            }
            on("a product of -1 and an integer subtracted from an integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - 2", Sum(Integer(5), Negation(Integer(2))).text())
                }
            }
            on("a sum of a positive and negative integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - 2", Sum(Integer(5), Integer(-2)).text())
                }
            }
            on("an expression that requires parentheses for grouping") {
                it("should correctly apply the parentheses") {
                    shouldEqual("(x - 2) * 2", Product(Sum(x, Integer(-2)), Integer(2)).text())
                }
            }
        }

        given("simplification of expressions") {
            val x = Variable("x")
            on("a sum of integers") {
                it("should return a single integer representing the sum") {
                    shouldEqual(Integer(4), Sum(Integer(2), Integer(2)).simplify())
                }
            }
            on("a sum of decimals") {
                it("should return a single decimal representing the sum") {
                    shouldEqual(Decimal(4.8), Sum(Decimal(2.8), Decimal(2.0)).simplify())
                }
            }
            on("a sum of an integer and a decimal") {
                it("should return a single decimal representing the sum") {
                    shouldEqual(Decimal(4.8), Sum(Decimal(2.8), Integer(2)).simplify())
                }
                it("should return a single decimal representing the sum when the order is reversed") {
                    shouldEqual(Decimal(4.8), Sum(Integer(2), Decimal(2.8)).simplify())
                }
            }
            on("a product of integers") {
                it("should return a single integer representing the product") {
                    shouldEqual(Integer(4), Product(Integer(2), Integer(2)).simplify())
                }
            }
            on("a product of decimals") {
                it("should return a single decimal representing the product") {
                    shouldEqual(Decimal(4.8), Product(Decimal(2.4), Decimal(2.0)).simplify())
                }
            }
            on("a product of an integer and a decimal") {
                it("should return a single decimal representing the product") {
                    shouldEqual(Decimal(4.8), Product(Decimal(2.4), Integer(2)).simplify())
                }
                it("should return a single decimal representing the product when the order is reversed") {
                    shouldEqual(Decimal(4.8), Product(Integer(2), Decimal(2.4)).simplify())
                }
            }
            on("a decimal divided by a non-zero integer") {
                it("should return a single decimal representing the division") {
                    shouldEqual(Decimal(2.4), Division(Decimal(4.8), Integer(2)).simplify())
                }
            }
            on("a decimal divided by a non-zero decimal") {
                it("should return a single decimal representing the division") {
                    shouldEqual(Decimal(2.4), Division(Decimal(4.8), Decimal(2.0)).simplify())
                }
            }
            on("an integer A divided by an integer B") {
                it("should return the unchanged expression if A is not divisible by B") {
                    val expr = Division(Integer(3), Integer(2))
                    shouldEqual(expr, expr.simplify())
                }
                it("should return a single integer if A is divisible by B") {
                    val expr = Division(Integer(4), Integer(2))
                    shouldEqual(Integer(2), expr.simplify())
                }
            }
            on("a product of integers multiplied by a variable") {
                it("should multiply the integers and multiply with the variable") {
                    val expr = Product(Product(Integer(2), Integer(2)), x)
                    shouldEqual(Product(Integer(4), x), expr.simplify())
                }
            }
            on("integers added to a variable on both sides") {
                it("should sum the integers together") {
                    val expr = Sum(Integer(2), x, Integer(2))
                    shouldEqual(Sum(Integer(4), x), expr.simplify())
                }
            }
            on("a variable multiplied by an integer on both sides") {
                it("should multiply the integers together") {
                    val expr = Product(Integer(2), x, Integer(2))
                    shouldEqual(Product(Integer(4), x), expr.simplify())
                }
            }
            on("a product of several integers followed by a variable followed by several integers") {
                it("should multiply the integers together") {
                    val expr = Product(Integer(2), Integer(3), Integer(4), x, Integer(5), Integer(6))
                    shouldEqual(Product(Integer(720), x), expr.simplify())
                }
            }


        }
    }
}