package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import symbolic.expressions.*

class BuildExpressionSpec : Spek() {
    init {
        given("the expression builder on single tokens") {
            on("a single integer") {
                it("should return an Integer expression with the same value") {
                    shouldEqual(Integer(3), assemble(tokenize("3")))
                }
            }
            on("a single decimal") {
                it("should return a Decimal expression with the same value") {
                    shouldEqual(Decimal(3.3), assemble(tokenize("3.3")))
                }
            }
            on("a single variable") {
                it("should return a Variable expression with the same value") {
                    shouldEqual(Variable("var"), assemble(tokenize("var")))
                }
            }
        }
        given("the expression builder on expressions with a binary operator") {
            on("two integers added together") {
                it("should return a BinarySum consisting of the two integers") {
                    shouldEqual(BinarySum(Integer(1), Integer(2)), assemble(tokenize("1+2")))
                }
            }
            on("two integers multiplied together") {
                it("should return a BinaryProduct consisting of the two integers") {
                    shouldEqual(BinaryProduct(Integer(1), Integer(2)), assemble(tokenize("1*2")))
                }
            }
            on("an integer subtracted from another") {
                it("should return a composite BinarySum where the right side is multiplied by -1") {
                    val expected = BinarySum(Integer(1), BinaryProduct(Integer(-1), Integer(2)))
                    shouldEqual(expected, assemble(tokenize("1-2")))
                }
            }

            val x = Variable("x")
            val y = Variable("y")
            on("two variables added together") {
                it("should return a BinarySum consisting of the two variables") {
                    shouldEqual(BinarySum(x, y), assemble(tokenize("x+y")))
                }
            }

            on("two variables multiplied together") {
                it("should return a BinaryProduct consisting of the two variables") {
                    shouldEqual(BinaryProduct(x, y), assemble(tokenize("x*y")))
                }
            }

            on("a variable subtracted from another") {
                it("should return a composite BinarySum where the right side is multiplied by -1") {
                    val expected = BinarySum(x, BinaryProduct(Integer(-1), y))
                    shouldEqual(expected, assemble(tokenize("x-y")))
                }
            }

            on("a variable and an integer added together") {
                it("should return a BinarySum of the two") {
                    shouldEqual(BinarySum(x, Integer(1)), assemble(tokenize("x+1")))
                }
            }

            on("a variable and an integer multiplied together") {
                it("should return a BinaryProduct of the two") {
                    shouldEqual(BinaryProduct(x, Integer(1)), assemble(tokenize("x*1")))
                }
            }

            on("an integer subtracted from a variable") {
                it("should return a composite BinarySum where the right side is multiplied by -1") {
                    val expected = BinarySum(x, BinaryProduct(Integer(-1), Integer(1)))
                    shouldEqual(expected, assemble(tokenize("x-1")))
                }
            }
        }
    }
}
