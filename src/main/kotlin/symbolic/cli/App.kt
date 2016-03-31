package symbolic.cli

import symbolic.expressions.EmptyExpression
import symbolic.expressions.Expression
import symbolic.parser.*
import kotlin.system.measureTimeMillis

private data class ComputationResult(val input: Expression, val time: Long) {
    val simplified by lazy { input.simplify() }

    companion object {
        fun fromInput(input: String): ComputationResult {
            var expr: Expression = EmptyExpression
            val time = measureTimeMillis { expr = assemble(tokenize(input)) }
            return ComputationResult(expr, time)
        }
    }
}

class App {
    private var debug = false

    fun run(): Int {
        var shouldContinue = true

        printLine("")
        while (shouldContinue) {
            prompt()
            val line = readLine()
            val normalized = line?.trim()?.toLowerCase()
            when {
                line == null -> shouldContinue = false
                normalized == "exit" -> shouldContinue = false
                normalized == "debug" -> toggleDebug()
                else -> handleInput(line)
            }
        }

        return 0
    }

    private fun toggleDebug() {
        debug = !debug
        when (debug) {
            true -> printLine("Debug mode enabled.")
            false -> printLine("Debug mode disabled.")
        }
    }

    private fun handleInput(input: String) {
        try {
            val result = ComputationResult.fromInput(input)
            printLine("Input:                 " + result.input.text())
            printLine("Simplified:            " + result.simplified.text())

            if (debug) {
                printLine("With types:            " + result.input.toString())
                printLine("Simplified with types: " + result.simplified.toString())
                skipLine()
                printLine("Query completed in ${result.time} ms.")
            }
        } catch (e: TokenizationException) {
            printError("Problem parsing the input: " + e.message)
        } catch (e: MismatchedParenthesisException) {
            printError("Input has mismatched parentheses.")
        } catch (e: AssemblyException) {
            printError("Failure to construct expression from input: " + e.message)
        }
    }

    private fun prompt() = print("\n> ")
    private fun indent() = print("  ")
    private fun skipLine() = println()
    private fun printLine(str: String) { indent(); println(str) }
    private fun printError(message: String) = printLine("ERROR: " + message)
}

fun main(args: Array<String>) {
    val exitCode = App().run()
    System.exit(exitCode)
}