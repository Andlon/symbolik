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
                    shouldEqual("5 * x", BinaryProduct(Integer(5), x).text())
                }
            }
            on("a sum of an integer and a variable") {
                it("should equal the sum") {
                    shouldEqual("5 + x", BinarySum(Integer(5), x).text())
                }
            }
            on("a product of -1 and a variable subtracted from an integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - x", BinarySum(Integer(5), BinaryProduct(Integer(-1), x)).text())
                }
            }
            on("a product of -1 and an integer subtracted from an integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - 2", BinarySum(Integer(5), BinaryProduct(Integer(-1), Integer(2))).text())
                }
            }
            on("a sum of a positive and negative integer") {
                it("should be represented with a single minus operator") {
                    shouldEqual("5 - 2", BinarySum(Integer(5), Integer(-2)).text())
                }
            }
            on("an expression that requires parentheses for grouping") {
                it("should correctly apply the parentheses") {
                    shouldEqual("(x - 2) * 2", BinaryProduct(BinarySum(x, Integer(-2)), Integer(2)).text())
                }
            }
        }
    }
}