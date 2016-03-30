package symbolic.cli

import symbolic.parser.*
import kotlin.system.measureTimeMillis

class App {
    fun handleInput(input: String) {
        try {
            val expr = assemble(tokenize(input))
            printLine(expr.toString())
        } catch (e: TokenizationException) {
            printError("Problem parsing the input: " + e.message)
        } catch (e: MismatchedParenthesisException) {
            printError("Input has mismatched parentheses.")
        } catch (e: AssemblyException) {
            printError("Failure to construct expression from input: " + e.message)
        }
    }

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

    fun prompt() {
        print("\n> ")
    }

    fun indent() {
        print("  ")
    }

    fun skipLine() {
        println()
    }

    fun printLine(str: String) { indent(); println(str) }
    fun printError(message: String) = printLine("ERROR: " + message)
}

fun main(args: Array<String>) {
    val app = App()
    System.exit(app.run())
}