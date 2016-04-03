package symbolic.util

import java.util.Stack
import java.util.EmptyStackException

fun <T> Stack<T>.popOrNull(): T? = try { this.pop() } catch(e: EmptyStackException) { null }

fun <T> Stack<T>.popWhile(predicate: (T) -> Boolean): List<T> {
    val elements = mutableListOf<T>()
    while (this.isNotEmpty() && predicate(this.peek())) {
        elements.add(this.pop())
    }
    return elements
}

fun <T> repeat(times: Int, obj: T): List<T> = (1 .. times).map { obj }