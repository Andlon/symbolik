package symbolic.cli

import symbolic.parser.*
import kotlin.system.measureTimeMillis

class App {
    fun run(): Int {
        printLine("")

        while (true) {
            prompt()
            val line = readLine()
            if (line == null || line.trim().toLowerCase() == "exit") {
                break
            } else {
                val time = measureTimeMillis { handleInput(line) }
                skipLine()
                printLine("Query completed in $time ms.")
            }
        }

        return 0
    }

    private fun handleInput(input: String) {
        try {
            val expr = assemble(tokenize(input))
            printLine("Input:                 " + expr.text())
            printLine("Simplified:            " + expr.simplify().text())
            printLine("With types:            " + expr.toString())
            printLine("Simplified with types: " + expr.simplify().toString())
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