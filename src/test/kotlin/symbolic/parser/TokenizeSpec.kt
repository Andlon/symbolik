package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import org.jetbrains.spek.api.shouldThrow

class TokenizeSpec : Spek() {
    init {
        given("the tokenizer on single-token inputs") {

            on("tokenizing an integer") {
                it("should return an integer constant token") {
                    shouldEqual(listOf(Token.Integer(5)), tokenize("5"))
                }
            }

            on("tokenizing a decimal number") {
                it("should return a decimal token") {
                    shouldEqual(listOf(Token.Decimal(3.34)), tokenize("3.34"))
                }
                it("should understand omitted leading zeros") {
                    shouldEqual(listOf(Token.Decimal(0.55)), tokenize(".55"))
                }
                it("should throw TokenizationException upon a trailing dot") {
                    shouldThrow(TokenizationException::class.java, { tokenize("1.") })
                }
            }

            on("tokenizing a name") {
                it("should return a name token") {
                    shouldEqual(listOf(Token.Name("a0BC")), tokenize("a0BC"))
                }
            }

            on("tokenizing a multiplication symbol") {
                it("should return a times token") {
                    shouldEqual(listOf(Token.BinaryOperator.Times), tokenize("*"))
                }
            }

            on("tokenizing a plus symbol") {
                it("should return a plus token") {
                    shouldEqual(listOf(Token.UnaryOperator.Plus), tokenize("+"))
                }
            }

            on("tokenizing a minus symbol") {
                it("should return a minus token") {
                    shouldEqual(listOf(Token.UnaryOperator.Minus), tokenize("-"))
                }
            }

            on("tokenizing pure whitespace") {
                it("should return an empty list") {
                    shouldEqual(emptyList<Token>(), tokenize(" "))
                }
            }

            on("tokenizing a left parenthesis") {
                it("should return a LeftParenthesis") {
                    shouldEqual(listOf(Token.LeftParenthesis), tokenize("("))
                }
            }

            on("tokenizing a right parenthesis") {
                it("should return a RightParenthesis") {
                    shouldEqual(listOf(Token.RightParenthesis), tokenize(")"))
                }
            }
        }

        given("the tokenizer on multi-token inputs") {
            on("integers followed by names without spacing") {
                it("should give an integer token followed by a name token") {
                    shouldEqual(listOf(Token.Integer(3), Token.Name("x")), tokenize("3x"))
                }
            }

            on("two names separated by whitespace") {
                it("should yield two name tokens") {
                    shouldEqual(listOf(Token.Name("a"), Token.Name("b")), tokenize("a b"))
                }
            }

            on("a series of arbitrary tokens") {
                val tokenString = "3.3+4 abcd*+"
                val tokens = listOf(Token.Decimal(3.3), Token.BinaryOperator.Plus, Token.Integer(4),
                        Token.Name("abcd"), Token.BinaryOperator.Times, Token.UnaryOperator.Plus)

                it("should yield the expected tokenization") {
                    shouldEqual(tokens, tokenize(tokenString))
                }
            }

        }

    }
}