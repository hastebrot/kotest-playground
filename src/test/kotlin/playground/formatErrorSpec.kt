package playground

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.eq.eq
import io.kotest.assertions.errorCollector
import io.kotest.assertions.failure
import io.kotest.assertions.print.print
import io.kotest.assertions.throwCollectedErrors
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class FormatErrorSpec : ExpectSpec({
    context("intellij format errors") {
        expect("with shouldBe") {
            "foobar".shouldBe("foo")
        }

        expect("with eq") {
            eq("foobar", "foo")?.let { throw it }
        }

        expect("with failure") {
            failure(Expected("foo".print()), Actual("foobar".print())).let { throw it }
        }

        expect("with Error") {
            throw Error("""expected:<"foo"> but was:<"foobar">""")
        }

        expect("with clue and failure") {
            throw withClue("foo") {
                failure("""expected:<"foo"> but was:<"foobar">""")
            }
        }

        expect("with failure and build string") {
            throw failure(
                buildString {
                    appendLine("foo")
                    append("""expected:<"foo"> but was:<"foobar">""")
                }
            )
        }

        expect("with clue and multiple equality matchers") {
            throw failure(
                buildString {
                    appendLine("foo")
                    appendLine("""expected:<"foo"> but was:<"foobar">""")
                    appendLine()
                    appendLine("bar")
                    append("""expected:<"bar"> but was:<"foobar">""")
                }
            )
        }

        expect("with error collector") {
            errorCollector.pushError(failure("""expected:<"foo"> but was:<"foobar">"""))
            errorCollector.pushError(failure("""expected:<"bar"> but was:<"foobar">"""))
            errorCollector.throwCollectedErrors()
        }
    }
})
