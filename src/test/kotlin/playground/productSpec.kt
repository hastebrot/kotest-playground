package playground

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.MockKDsl
import io.mockk.MockKGateway
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKSettings
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.RecordedCall
import io.mockk.StackTracesAlignment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.JvmMockKGateway
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.recording.JvmAutoHinter
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.AllCallsCallVerifier
import io.mockk.impl.verify.OrderedCallVerifier
import io.mockk.impl.verify.SequenceCallVerifier
import io.mockk.impl.verify.TimeoutVerifier
import io.mockk.impl.verify.UnorderedCallVerifier
import io.mockk.mockk
import playground.proto.ProtoProduct

interface ProductService {
    suspend fun storeProduct(objectId: String, product: ProtoProduct)
    suspend fun deleteProduct(objectId: String)
}

class ProductSpec : DescribeSpec({
    describe("product") {
        MockKSettings.setStackTracesAlignment(StackTracesAlignment.LEFT.name)

        describe("mock interaction verification") {
            it("should verify with success") {
                // given:
                val productService = mockk<ProductService>()
                coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
                coEvery { productService.deleteProduct(any()) }.returns(Unit)

                // when:
                val product = ProtoProduct.newBuilder().build()
                productService.storeProduct("id", product)

                // then:
                coVerify(ordering = Ordering.UNORDERED, exactly = 1) {
                    productService.storeProduct(eq("id"), any())
                }
            }

            it("should verify with failure") {
                // given:
                val productService = mockk<ProductService>()
                coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
                coEvery { productService.deleteProduct(any()) }.returns(Unit)

                // when:
                val product = ProtoProduct.newBuilder().build()
                productService.storeProduct("id", product)

                // then:
                try {
                    MockKDsl.internalCoVerify(exactly = 1) {
                        productService.storeProduct("invalid", any())
                    }
                } catch (exception: AssertionError) {
                    println(exception.message)
                    println(exception.stackTrace)
                }
            }
        }

        describe("custom mock interaction verification") {
            it("should record and verify mock interactions") {
                // given:
                val productService = mockk<ProductService>()
                coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
                coEvery { productService.deleteProduct(any()) }.returns(Unit)

                // when:
                val product = ProtoProduct.newBuilder().build()
                productService.storeProduct("id", product)

                // then:
                val mockGateway = MockKGateway.implementation() as JvmMockKGateway
                val stubRepository = mockGateway.gatewayAccess.stubRepository
                val safeToString = mockGateway.gatewayAccess.safeToString
                val callRecorder = mockGateway.gatewayAccess.callRecorder() as CommonCallRecorder

                val verificationParams = VerificationParameters(
                    ordering = Ordering.UNORDERED,
                    min = 1,
                    max = 1,
                    inverse = false,
                    timeout = 0
                )

                val recordedCalls = recordCalls(
                    callRecorder = callRecorder,
                    stubRepository = stubRepository,
                    verificationParams = verificationParams,
                    mockBlock = null,
                    coMockBlock = {
                        productService.storeProduct(eq("id"), product)
                    }
                )

                val verifiedCalls = verifyCalls(
                    stubRepository = stubRepository,
                    safeToString = safeToString,
                    verificationSequence = recordedCalls,
                    verificationParams = verificationParams
                )

                println(stubRepository.allStubs.sortedBy { it.name }.map { it.toStr() })
                println(stubRepository.allStubs.sortedBy { it.name }.map { it.allRecordedCalls() })

                recordedCalls.toString().shouldBe(
                    "[" +
                        "RecordedCall(retValue=Unit(child of #9#12), retType=Unit, isRetValueMock=true " +
                        "matcher=ProductService(#9).storeProduct(eq(id), eq(), any())))" +
                        "]"
                )
                verifiedCalls.toString().shouldBe(
                    "OK(verifiedCalls=[" +
                        "ProductService(#9).storeProduct(id, , continuation {})" +
                        "])"
                )
            }

            it("should have readable test result output") {
                // given:
                val productService = mockk<ProductService>()
                coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
                coEvery { productService.deleteProduct(any()) }.returns(Unit)

                // when:
                val product = ProtoProduct.newBuilder()
                    .setName("product name")
                    .setDescription("product description")
                    .build()
                val otherProduct = ProtoProduct.newBuilder()
                    .setName("other product name")
                    .setDescription("other product description")
                    .build()
                productService.storeProduct("invalid", otherProduct)
                productService.storeProduct("also invalid", product)

                // then:
                coVerify {
                    productService.storeProduct(eq("id"), product)
                    productService.storeProduct(eq("other id"), eq(product))
                }
            }
        }
    }
})

internal typealias MockBlock = MockKVerificationScope.() -> Unit
internal typealias CoMockBlock = suspend MockKVerificationScope.() -> Unit

// recordedCall:
// - retValue: Any?
// - retType: KClass<*>
// - isRetValueMock: Boolean
// - matcher: InvocationMatcher
//   - self: Any
//   - method: MethodDescription
//   - args: List<Matcher<Any>>
//   - allAny: Boolean

// verificationResult:
// - OK
//   - verifiedCalls: List<Invocation>
//     - self: Any
//     - stub: Any
//     - method: MethodDescription
//     - args: List<Any?>
//     - timestamp: Long
//     - callStack: () -> List<StackElement>
//     - originalCall: () -> Any?
//     - fieldValueProvider: BackingFieldValueProvider
// - Failure
//   - message: String

internal fun recordCalls(
    callRecorder: MockKGateway.CallRecorder,
    stubRepository: StubRepository,
    verificationParams: MockKGateway.VerificationParameters,
    mockBlock: MockBlock?,
    coMockBlock: CoMockBlock?,
): List<RecordedCall> {
    class DerivedCallRecorder(private val base: MockKGateway.CallRecorder) : MockKGateway.CallRecorder by base {
        override fun reset() = Unit
    }

    fun copyRecordedCalls(recordedCalls: List<RecordedCall>) = recordedCalls.toList()
    val derivedCallRecorder = DerivedCallRecorder(callRecorder)

    val evaluator = VerifyBlockEvaluator(
        callRecorder = { derivedCallRecorder },
        stubRepo = stubRepository,
        autoHinterFactory = ::JvmAutoHinter
    )

    val recordedCalls = try {
        evaluator.verify(
            params = verificationParams,
            mockBlock = mockBlock,
            coMockBlock = coMockBlock,
        )
        copyRecordedCalls(callRecorder.calls)
    } catch (ignore: Throwable) {
        copyRecordedCalls(callRecorder.calls)
    } finally {
        callRecorder.reset()
    }

    return recordedCalls
}

internal fun verifyCalls(
    stubRepository: StubRepository,
    safeToString: SafeToString,
    verificationSequence: List<RecordedCall>,
    verificationParams: MockKGateway.VerificationParameters
): MockKGateway.VerificationResult {
    val callOrderVerifier = when (verificationParams.ordering) {
        Ordering.UNORDERED -> UnorderedCallVerifier(stubRepository, safeToString)
        Ordering.ALL -> AllCallsCallVerifier(stubRepository, safeToString)
        Ordering.ORDERED -> OrderedCallVerifier(stubRepository, safeToString)
        Ordering.SEQUENCE -> SequenceCallVerifier(stubRepository, safeToString)
    }
    val callVerifier = when {
        verificationParams.timeout > 0 -> TimeoutVerifier(stubRepository, callOrderVerifier)
        else -> callOrderVerifier
    }
    return callVerifier.verify(
        verificationSequence = verificationSequence,
        params = verificationParams
    )
}
