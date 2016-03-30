package symbolic.util

import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.shouldEqual

class GcdSpec : Spek() { init {
    given("gcd(a, b) for integers a, b") {
        on("gcd for a, b > 0") {
            it("should yield gcd(a, 1) == 1 for a != 0") {
                shouldEqual(1, gcd(6, 1))
                shouldEqual(1, gcd(7, 1))
                shouldEqual(1, gcd(8, 1))
                shouldEqual(1, gcd(1, 6))
                shouldEqual(1, gcd(1, 7))
                shouldEqual(1, gcd(1, 8))
            }
            it("should yield gcd(a, 0) == a for a != 0") {
                shouldEqual(6, gcd(6, 0))
                shouldEqual(7, gcd(7, 0))
                shouldEqual(8, gcd(8, 0))
                shouldEqual(6, gcd(0, -6))
                shouldEqual(7, gcd(0, -7))
                shouldEqual(8, gcd(0, -8))
                shouldEqual(6, gcd(-6, 0))
                shouldEqual(7, gcd(-7, 0))
                shouldEqual(8, gcd(-8, 0))
                shouldEqual(6, gcd(0, -6))
                shouldEqual(7, gcd(0, -7))
                shouldEqual(8, gcd(0, -8))
            }
            it("should yield gcd(a, a) == a for a != 0") {
                shouldEqual(6, gcd(6, 6))
                shouldEqual(7, gcd(7, 7))
                shouldEqual(8, gcd(8, 8))
            }
            it("should yield gcd(-a, a) == a for a != 0") {
                shouldEqual(6, gcd(-6, 6))
                shouldEqual(7, gcd(-7, 7))
                shouldEqual(8, gcd(-8, 8))
                shouldEqual(6, gcd(6, -6))
                shouldEqual(7, gcd(7, -7))
                shouldEqual(8, gcd(8, -8))
            }
            it("should yield correct gcd for a selection of a != b != 0") {
                shouldEqual(8, gcd(16, 24))
                shouldEqual(7, gcd(14, 21))
            }
        }
    }
}
}
