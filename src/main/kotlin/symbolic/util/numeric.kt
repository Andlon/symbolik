package symbolic.util

import java.lang.Math.abs
import java.lang.Math.min

tailrec fun gcd(a: Int, b: Int): Int = when {
    a == 0 -> abs(b)
    b == 0 -> abs(a)
    abs(a) == abs(b) -> abs(a)
    else -> gcd(abs(b - a), min(a, b))
}

fun isDivisible(a: Int, b: Int) = gcd(a, b) == b