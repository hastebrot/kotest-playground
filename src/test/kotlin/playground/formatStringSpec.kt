package playground

import com.google.protobuf.MessageOrBuilder
import com.google.protobuf.util.JsonFormat
import com.google.protobuf.util.JsonFormat.Printer
import io.kotest.assertions.Actual
import io.kotest.assertions.Expected
import io.kotest.assertions.diffLargeString
import io.kotest.assertions.failure
import io.kotest.assertions.print.Printed
import io.kotest.assertions.print.print
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.ConstantMatcher
import io.mockk.EqMatcher
import io.mockk.Invocation
import io.mockk.InvocationMatcher
import io.mockk.Matcher
import io.mockk.MockKGateway
import io.mockk.MockKMatcherScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import playground.proto.ProtoProduct
import kotlin.coroutines.Continuation

class FormatStringSpec : ExpectSpec({
    context("to string") {
        expect("should represent matchers") {
            // given:
            val callRecorder = mockk<MockKGateway.CallRecorder>(relaxed = true)
            val capturingSlot = mockk<CapturingSlot<Function<*>>>(relaxed = true)
            val matcherSlot = slot<Matcher<Any>>()
            every<Any> { callRecorder.matcher(capture(matcherSlot), any()) }.answers { matcherSlot.captured }

            // when/then:
            val matcherScope = MockKMatcherScope(callRecorder, capturingSlot)
            matcherScope.any<Any>().toString().shouldBe("any()")
            matcherScope.eq<Any>("value").toString().shouldBe("eq(value)")
        }

        expect("diff large string") {
            val product = ProtoProduct.newBuilder()
                .setName("product name")
                .setDescription("product description")
                .build()
            val otherProduct = ProtoProduct.newBuilder()
                .setName("other product name")
                .setDescription("other product description")
                .build()

            val result = diffLargeString(product.print().value, otherProduct.print().value)!!
            throw failure(Expected(Printed(result.first)), Actual(Printed(result.second)))
        }
    }
})

private val jsonPrinter: Printer = JsonFormat.printer()
    .includingDefaultValueFields()
    .omittingInsignificantWhitespace()

private fun reprValue(value: Any?): Printed = when (value) {
    is MessageOrBuilder -> Printed(jsonPrinter.print(value))
    is Continuation<*> -> Printed("@continuation")
    else -> value.print()
}

private const val MATCH_OKAY = "+   "
private const val MATCH_NOT_OKAY = "-   "

private fun String.indentText(firstIndent: String, otherIndent: String): String {
    return firstIndent + prependIndent(otherIndent).trimStart()
}

private fun formatInvocationMatcherArgs(
    invocationMatcher: InvocationMatcher,
    invocation: Invocation,
    indent: Boolean = false
): String {
    val builder = StringBuilder()
    invocationMatcher.args.indices.forEach { index ->
        val argumentMatcher = invocationMatcher.args[index]
        val actualArgument = invocation.args[index]
        val expectedArgument = when (argumentMatcher) {
            is EqMatcher<Any> -> argumentMatcher.value
            is ConstantMatcher<Any> -> actualArgument
            else -> error("matcher not supported")
        }

        val matchedString = MATCH_OKAY
        val prependLine = "${" ".repeat(4)}$matchedString"
        val prependOtherLines = " ".repeat(8)
        builder.appendLine(expectedArgument
            .run { reprValue(this).value }
            .run { if (indent) indentText(prependLine, prependOtherLines) else this }
        )
    }
    return builder.trimEnd().toString()
}

private fun formatInvocationArgs(
    invocationMatcher: InvocationMatcher,
    invocation: Invocation,
    indent: Boolean = false
): String {
    val builder = StringBuilder()
    invocationMatcher.args.indices.forEach { index ->
        val argumentMatcher = invocationMatcher.args[index]
        val actualArgument = invocation.args[index]
        val expectedArgument = when (argumentMatcher) {
            is EqMatcher<Any> -> argumentMatcher.value
            is ConstantMatcher<Any> -> actualArgument
            else -> error("matcher not supported")
        }

        val matched = argumentMatcher.match(invocation)
        val matchedString = if (matched) MATCH_OKAY else MATCH_NOT_OKAY
        val prependLine = "${" ".repeat(4)}$matchedString"
        val prependOtherLines = " ".repeat(8)
        builder.appendLine(actualArgument
            .run { reprValue(this).value }
            .run { if (indent) indentText(prependLine, prependOtherLines) else this }
        )
    }
    return builder.trimEnd().toString()
}
