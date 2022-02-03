package playground

import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.eq.eq
import io.kotest.assertions.failure
import io.kotest.assertions.print.print
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe

class IntellijFormatErrorSpec : ExpectSpec({
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
})
