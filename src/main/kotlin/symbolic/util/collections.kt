package symbolic.util

import java.util.Stack
import java.util.EmptyStackException

fun <T> Stack<T>.popOrNull(): T? = try { this.pop() } catch(e: EmptyStackException) { null }
