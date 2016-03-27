package symbolic.parser

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual

/**
 * Created by andreas on 27.03.16.
 */
class TokenizeSpec : Spek() {
    init {
        given("the tokenizer on single inputs") {

            on("tokenizing a single integer constant") {
                it("should return an integer constant token") {
                    shouldEqual(listOf(Token.Integer(5)), tokenize("5"))
                }
            }

            on("tokenizing a single decimal symbol") {
                it("should return a decimal token") {
                    shouldEqual(listOf(Token.Decimal(3.34)), tokenize("3.34"))
                }
                it("should understand omitted leading zeros") {
                    shouldEqual(listOf(Token.Decimal(0.55)), tokenize(".55"))
                }
            }

            on("tokenizing a name") {
                it("should return a name token") {
                    shouldEqual(listOf(Token.Name("a0BC")), tokenize("a0BC"))
                }
            }

            on("tokenizing a single multiplication symbol") {
                it("should return a times token") {
                    shouldEqual(listOf(Token.Times), tokenize("*"))
                }
            }

            on("tokenizing a single plus symbol") {
                it("should return a plus token") {
                    shouldEqual(listOf(Token.Plus), tokenize("+"))
                }
            }

            on("tokenizing a single minus symbol") {
                it("should return a minus token") {
                    shouldEqual(listOf(Token.Minus), tokenize("-"))
                }
            }
        }

    }
}