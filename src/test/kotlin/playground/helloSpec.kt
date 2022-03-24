package playground

import io.kotest.assertions.print.print
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNot
import io.kotest.matchers.throwable.shouldHaveMessage
import java.time.LocalDate

class HelloSpec : DescribeSpec({
    describe("hello") {
        it("should compare strings") {
            // given:
            val name = "planet"

            // when/then:
            "hello, $name!".shouldBe("hello, world!")
        }

        it("should have a clue and compare strings") {
            withClue("should be string '1'") {
                1.shouldBe("1")
            }
        }

        it("should have exception assertion") {
            shouldThrow<IllegalStateException> {
                error("error message")
            }.shouldHaveMessage("error message")
        }

        it("should format values for assertion error messages") {
            println(1.print())
            println("1".print())
            println(true.print())
            println(null.print())
            println("".print())
            println(listOf(1, 2, 3).print())
            println(listOf(1, 2, listOf(3, 4, listOf(5, 6))).print())
            println(LocalDate.parse("2020-01-02").print())

            data class AvroProduct(val name: String)
            data class Product(val name: String)

            listOf(1, 2).shouldBe(arrayOf(1, 2))
            Product("foo").shouldBe(AvroProduct("foo"))
        }

        it("should use custom matcher") {
            fun containFoo() = object : Matcher<String> {
                override fun test(value: String) = MatcherResult(
                    value.contains("foo"),
                    { "String '$value' should include 'foo'" },
                    { "String '$value' should not include 'foo'" }
                )
            }

            "hello foo".shouldNot(containFoo())
            "hello bar".shouldNot(containFoo())
        }
    }
})

/*
use soft assertions to group assertions.

```
assertSoftly(foo) {
  shouldNotEndWith("b")
  length.shouldBe(3)
}

custom matchers

```
interface Matcher<in T> {
  fun test(value: T): MatcherResult
}
```
*/