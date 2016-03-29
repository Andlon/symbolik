package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import org.jetbrains.spek.api.shouldThrow
import symbolic.expressions.*

class AssembleSpec : Spek() {
    init {
        given("assembly on single tokens") {
            on("an empty list of tokens") {
                it("should return an EmptyExpression") {
                    shouldEqual(EmptyExpression, assemble(tokenize("")))
                }
            }

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

        given("assembly on expressions with a unary operator") {
            val x = Variable("x")
            on("a unary plus and an integer") {
                it("should yield the integer") {
                    shouldEqual(Integer(2), assemble(tokenize("+2")))
                }
            }
            on("a unary minus and an integer") {
                it("should yield the integer multiplied by -1") {
                    shouldEqual(Integer(-2), assemble(tokenize("-2")))
                }
            }
            on("a unary plus and a variable") {
                it("should yield the variable") {
                    shouldEqual(x, assemble(tokenize("+x")))
                }
            }
            on("a unary minus and a variable") {
                it("should yield the variable") {
                    shouldEqual(BinaryProduct(Integer(-1), x), assemble(tokenize("-x")))
                }
            }
            on("a unary plus and a decimal") {
                it("should yield the decimal") {
                    shouldEqual(Decimal(4.3), assemble(tokenize("+4.3")))
                }
            }
            on("a unary minus and a decimal") {
                it("should yield the decimal multiplied by -1") {
                    shouldEqual(Decimal(-4.3), assemble(tokenize("-4.3")))
                }
            }
            on("subsequent unary plus operators applied to an integer") {
                it("should leave the operand unchanged") {
                    shouldEqual(Integer(3), assemble(tokenize("++3")))
                }
            }
            on("subsequent unary minus operators applied to an integer") {
                it("should negate the negative factor") {
                    shouldEqual(Integer(3), assemble(tokenize("--3")))
                }
            }
        }

        given("assembly on expressions with a binary operator") {
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

        given("assembly of more complicated expressions") {
            val x = Variable("x")
            val y = Variable("y")
            val z = Variable("z")
            on("nested binary operators of different precedence") {
                it("should return the expected composite expressions") {
                    val tokens = tokenize("x * y + 3 * z")
                    val expected = BinarySum(BinaryProduct(x, y), BinaryProduct(Integer(3), z))
                    shouldEqual(expected, assemble(tokens))
                }
            }
            on("taking the difference of two products") {
                it("should return the expected composite expressions") {
                    val tokens = tokenize("x * y - 3 * z")
                    val expected = BinarySum(BinaryProduct(x, y), BinaryProduct(Integer(-1), BinaryProduct(Integer(3), z)))
                    shouldEqual(expected, assemble(tokens))
                }
            }
            on("a single variable enclosed in parentheses") {
                it("should return an expression consisting of the single variable") {
                    shouldEqual(x, assemble(tokenize("(x)")))
                }
            }
            on("a single integer enclosed in parentheses") {
                it("should return an expression consisting of the single integer") {
                    shouldEqual(Integer(3), assemble(tokenize("(3)")))
                }
            }
            on("a single decimal number enclosed in parentheses") {
                it("should return an expression consisting of the single decimal number") {
                    shouldEqual(Decimal(5.4), assemble(tokenize("(5.4)")))
                }
            }
            on("an expression where parentheses are used to counter natural precedence") {
                var tokens = tokenize("(x + y) * (z + 1)")
                var expected = BinaryProduct(BinarySum(x, y), BinarySum(z, Integer(1)))
                it("should take parentheses into account and give the expected composite expression") {
                    shouldEqual(expected, assemble(tokens))
                }
            }
            on("composition of unary plus and parentheses") {
                var tokens = tokenize("+(x + y)")
                var expected = BinarySum(x, y)
                it("should leave the expression unchanged") {
                    shouldEqual(expected, assemble(tokens))
                }
            }
            on("composition of unary minus and parentheses") {
                var tokens = tokenize("-(x + y)")
                var expected = BinaryProduct(Integer(-1), BinarySum(x, y))
                it("should negate the expression") {
                    shouldEqual(expected, assemble(tokens))
                }
            }
        }

        given("assembly on input with mismatched parentheses") {
            on("a single left parenthesis token") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize("(")) })
                }
            }
            on("a single right parenthesis token") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize(")")) })
                }
            }
            on("missing right parenthesis on simple expression") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize("(1+2")) })
                }
            }
            on("missing left parenthesis on simple expression") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize("1+2)")) })
                }
            }
            on("missing right parenthesis in composite expression") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize("2 * (1 + 3")) })
                }
            }
            on("missing left parenthesis in composite expression") {
                it("should throw a MismatchedParenthesisException") {
                    shouldThrow(MismatchedParenthesisException::class.java, { assemble(tokenize("2 * 1 + 3)")) })
                }
            }
        }

        given("assembly on invalid input") {
            on("input with a trailing minus operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("1-"))})
                }
            }

            on("input with a trailing plus operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("1+"))})
                }
            }

            on("input with a trailing multiplication operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("1*"))})
                }
            }

            on("input with a trailing division operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("1/"))})
                }
            }

            on("input with a leading multiplication operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("*1"))})
                }
            }

            on("input with a leading division operator") {
                it("should throw an AssemblyException") {
                    shouldThrow(AssemblyException::class.java, { assemble(tokenize("/1"))})
                }
            }
        }
    }
}
