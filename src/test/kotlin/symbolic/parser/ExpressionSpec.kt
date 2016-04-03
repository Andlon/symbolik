package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import symbolic.expressions.*

class ExpressionSpec : Spek() {
    init {
        given("Expression text") {
            val x = Variable("x")
            val y = Variable("y")
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
            on("1 - [x - x]") {
                it("should return 1 - [x - x]") {
                    val expr = Sum(Integer(1), Negation(Sum(x, Negation(x))))
                    val expected = "1 - (x - x)"
                    shouldEqual(expected, expr.text())
                }
            }
            on("x * y - [x - x] * x * y") {
                it("should return x * y - [x - x] * x * y") {
                    val expr = Sum(Product(x, y), Negation(Product(Sum(x, Negation(x)), x, y)))
                    val expected = "x * y - (x - x) * x * y"
                    shouldEqual(expected, expr.text())
                }
            }
            on("x + y + x + y expressed in terms of nested sums") {
                it("should return x + y + x + y") {
                    val expr = Sum(Sum(x, y), Sum(x, y))
                    val expected = "x + y + x + y"
                    shouldEqual(expected, expr.text())
                }
            }
        }

        given("Expression simplify") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            val w = Variable("w")
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
            on("nested Sum expressions") {
                it("should flatten the expression") {
                    val expr = Sum(Integer(2), Sum(x, y))
                    shouldEqual(Sum(Integer(2), x, y), expr.simplify())
                }
            }
            on("nested Product expressions") {
                val expected = Product(Integer(2), x, y)
                it("should flatten the expression when the product is on the right") {
                    val expr = Product(Integer(2), Product(x, y))
                    shouldEqual(expected, expr.simplify())
                }
                it("should flatten the expression when the product is on the left") {
                    val expr = Product(Product(x, y), Integer(2))
                    shouldEqual(expected, expr.simplify())
                }
                it("should flatten the expression when both sides are products") {
                    val expr = Product(Product(x, y), Product(z, w))
                    shouldEqual(Product(x, y, z, w), expr.simplify())
                }
            }
            on("the product of two variables") {
                it("should not reorder the variables") {
                    val expr = Product(x, y)
                    val expr2 = Product(y, x)
                    shouldEqual(expr, expr.simplify())
                    shouldEqual(expr2, expr2.simplify())
                }
            }
            on("the sum of two variables") {
                it("should not reorder the variables") {
                    val expr = Sum(x, y)
                    val expr2 = Sum(y, x)
                    shouldEqual(expr, expr.simplify())
                    shouldEqual(expr2, expr2.simplify())
                }
            }
            on("collectible terms in a Sum expression") {
                it("should collect the terms for positive coefficients") {
                    val expr = Sum(Product(Integer(2), x), Product(Integer(3), x))
                    shouldEqual(Product(Integer(5), x), expr.simplify())
                }
                it("should collect the terms for negative coefficients") {
                    val expr = Sum(Product(Integer(-2), x), Product(Integer(-5), x))
                    shouldEqual(Product(Integer(-7), x), expr.simplify())
                }
                it("should collect the terms for negated coefficients") {
                    val expr = Sum(Product(Integer(-2), x), Negation(Product(Integer(5), x)))
                    shouldEqual(Product(Integer(-7), x), expr.simplify())
                }
            }
            on("collectible paranthesized terms") {
                it("should collect the terms") {
                    val expr = Integer(2) * (Integer(2) + x) + Integer(4) * (Integer(2) + x)
                    shouldEqual(Integer(12) + Integer(6) * x, expr.simplify())
                }
            }
        }
        given("Expression expand") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            val w = Variable("w")
            on("an expression with one set of parentheses on the left") {
                it("should correctly expand") {
                    val expr = Product(Sum(x, y), z)
                    val expected = Sum(Product(x, z), Product(y, z))
                    shouldEqual(expected, expr.expand())
                }
            }
            on("an expression with one set of parentheses on the right") {
                it("should correctly expand") {
                    val expr = Product(x, Sum(y, z))
                    val expected = Sum(Product(x, y), Product(x, z))
                    shouldEqual(expected, expr.expand())
                }
            }
            on("an expression with two sets of parentheses") {
                it("should correctly expand") {
                    val expr = Product(Sum(x, y), Sum(z, w))
                    val expected = Sum(Product(x, z), Product(y, z), Product(x, w), Product(y, w))
                    shouldEqual(expected, expr.expand())
                }
            }
        }
        given("Expression collect") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            val w = Variable("w")
            on("an expression with a simple common factor") {
                it("should recognize the common factor and collect it") {
                    val expr = Sum(Product(x, y), Product(x, z))
                    val expected = Product(x, Sum(y, z))
                    shouldEqual(expected, expr.collect())
                }
            }
            on("an expression where a product is the common factor") {
                val expr = Sum(Product(x, y, z), Product(x, y, w))
                val expected = Product(x, y, Sum(z, w))
                it("should recognize the product and collect it") {
                    shouldEqual(expected, expr.collect())
                }
            }
            on("a subtraction of two equal products") {
                it("should simplify to 0") {
                    val expr = Sum(Product(x, y), Negation(Product(x, y)))
                    shouldEqual(Integer(0), expr.collect())
                }
            }
        }

        given("Sum factors") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            val w = Variable("w")
            on("x + x") {
                val expr = Sum(x, x)
                val expected = listOf(FactorizedExpression(x, Sum(Integer(1), Integer(1)), EmptyExpression))
                it("should give x as the only factor") {
                    shouldEqual(expected, expr.factors())
                }
            }
            on("x - x") {
                val expr = Sum(x, Negation(x))
                val expected = listOf(FactorizedExpression(x, Sum(Integer(1), Negation(Integer(1))), EmptyExpression))
                it("should give x as the only factor") {
                    shouldEqual(expected, expr.factors())
                }
            }
            on("x * y + x * z") {
                val expr = Sum(Product(x, y), Product(x, z))
                val expected = setOf(
                        FactorizedExpression(x, Sum(y, z), EmptyExpression),
                        FactorizedExpression(y, x, Product(x, z)),
                        FactorizedExpression(z, x, Product(x, y))
                )
                it("should give x, y and z as factors with the appropriate remainders") {
                    shouldEqual(expected, expr.factors().toSet())
                }
            }
            on("x * y + x * y") {
                val expr = Sum(Product(x, y), Product(x, y))
                val expected = listOf(FactorizedExpression(Product(x, y), Sum(Integer(1), Integer(1))))
                it("should give x * y as the only factor") {
                    shouldEqual(expected, expr.factors())
                }
            }
            on("x * x + x * x") {
                val expr = Sum(Product(x, x), Product(x, x))
                val expected = listOf(FactorizedExpression(Product(x, x), Sum(Integer(1), Integer(1))))
                it("should give x * x as the only factor") {
                    shouldEqual(expected, expr.factors())
                }
            }
            on("x * x * x + x * x * x") {
                val expr = Sum(Product(x, x, x), Product(x, x, x))
                val expected = listOf(FactorizedExpression(Product(x, x, x), Sum(Integer(1), Integer(1))))
                it("should give x * x * x as the only factor") {
                    shouldEqual(expected, expr.factors())
                }
            }
            on("x * x + x * x * x") {
                val expr = Sum(Product(x, x), Product(x, x, x))
                val expected = listOf(FactorizedExpression(Product(x, x), Sum(Integer(1), x)))
                it("should give x * x as the only factor") {
                    shouldEqual(expected, expr.factors())
                }

            }
        }

        given("Expression combineTerms") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            val w = Variable("w")
            on("2 * 3") {
                it("should return 6") {
                    val expr = Product(Integer(2), Integer(2))
                    val expected = Integer(4)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("2 + 3") {
                it("should return 5") {
                    val expr = Sum(Integer(2), Integer(2))
                    val expected = Integer(4)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("2 + 3 * 4") {
                it("should return 24") {
                    val expr = Sum(Integer(2), Product(Integer(2), Integer(2)))
                    val expected = Integer(6)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("(2 + 3) * 4") {
                it("should return 20") {
                    val expr = Product(Sum(Integer(2), Integer(3)), Integer(4))
                    val expected = Integer(20)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("1 * 2") {
                it("should return 2") {
                    val expr = Product(Integer(1), Integer(2))
                    val expected = Integer(2)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 + 2") {
                it("should return 2") {
                    val expr = Sum(Integer(0), Integer(2))
                    val expected = Integer(2)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0") {
                it("should return 0") {
                    val expr = Integer(0)
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("1") {
                it("should return 1") {
                    val expr = Integer(1)
                    val expected = Integer(1)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("-0") {
                it("should return 0") {
                    val expr = Negation(Integer(0))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 + 0") {
                it("should return 0") {
                    val expr = Sum(Integer(0), Integer(0))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 * 0") {
                it("should return 0") {
                    val expr = Product(Integer(0), Integer(0))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 * 1") {
                it("should return 0") {
                    val expr = Product(Integer(0), Integer(1))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 - 0") {
                it("should return 0") {
                    val expr = Sum(Integer(0), Negation(Integer(0)))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("1 - 1") {
                it("should return 0 when Negation is used") {
                    val expr = Sum(Integer(1), Negation(Integer(1)))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
                it("should return 0 when a negative integer is used") {
                    val expr = Sum(Integer(1), Integer(-1))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("-1 + 1") {
                it("should return 0 when Negation is used") {
                    val expr = Sum(Negation(Integer(1)), Integer(1))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
                it("should return 0 when a negative integer is used") {
                    val expr = Sum(Integer(-1), Integer(1))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("1 * x") {
                it("should return x") {
                    val expr = Product(Integer(1), x)
                    val expected = x
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 * x") {
                it("should return 0") {
                    val expr = Product(Integer(0), x)
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0 * 0,0") {
                it("should return 0") {
                    val expr = Product(Integer(0), Decimal(0.0))
                    val expected = Integer(0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("0,0 + 1") {
                it("should return 1") {
                    val expr = Sum(Decimal(0.0), Integer(1))
                    val expected = Integer(1)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
            on("2,0 + 3,0") {
                it("should return 5,0") {
                    val expr = Sum(Decimal(2.0), Decimal(3.0))
                    val expected = Decimal(5.0)
                    shouldEqual(expected, expr.combineTerms())
                }
            }
        }


    }
}