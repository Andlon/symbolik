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
                    shouldEqual(listOf(Token.IntegerConstant(5)), tokenize("5"))
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