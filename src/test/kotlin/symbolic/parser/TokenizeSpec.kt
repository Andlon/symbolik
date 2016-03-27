package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual
import org.jetbrains.spek.api.shouldThrow

/**
 * Created by andreas on 27.03.16.
 */
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
                    shouldThrow(TokenizationException::class.java, { tokenize("1.")})
                }
            }

            on("tokenizing a name") {
                it("should return a name token") {
                    shouldEqual(listOf(Token.Name("a0BC")), tokenize("a0BC"))
                }
            }

            on("tokenizing a multiplication symbol") {
                it("should return a times token") {
                    shouldEqual(listOf(Token.Times), tokenize("*"))
                }
            }

            on("tokenizing a plus symbol") {
                it("should return a plus token") {
                    shouldEqual(listOf(Token.Plus), tokenize("+"))
                }
            }

            on("tokenizing a minus symbol") {
                it("should return a minus token") {
                    shouldEqual(listOf(Token.Minus), tokenize("-"))
                }
            }
        }

    }
}