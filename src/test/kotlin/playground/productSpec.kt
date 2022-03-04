package playground

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.MockKGateway
import io.mockk.MockKGateway.CallRecorder
import io.mockk.MockKGateway.VerificationParameters
import io.mockk.MockKVerificationScope
import io.mockk.Ordering
import io.mockk.RecordedCall
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.JvmMockKGateway
import io.mockk.impl.eval.VerifyBlockEvaluator
import io.mockk.impl.log.SafeToString
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.recording.JvmAutoHinter
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.UnorderedCallVerifier
import io.mockk.mockk
import playground.proto.ProtoProduct

interface ProductService {
    suspend fun storeProduct(objectId: String, product: ProtoProduct)
    suspend fun deleteProduct(objectId: String)
}

class ProductSpec : DescribeSpec({
    describe("product") {
        it("should verify mock interactions") {
            // given:
            val productService = mockk<ProductService>()
            coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
            coEvery { productService.deleteProduct(any()) }.returns(Unit)

            // when:
            val product = ProtoProduct.newBuilder().build()
            productService.storeProduct("id", product)

            // then:
            coVerify(exactly = 1) { productService.storeProduct("id", any()) }
//            MockKDsl.internalCoVerify(exactly = 1) { productService.storeProduct("id", any()) }
        }

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
                    "RecordedCall(retValue=Unit(child of #5#8), retType=Unit, isRetValueMock=true " +
                    "matcher=ProductService(#5).storeProduct(eq(id), eq(), any())))" +
                    "]"
            )
            verifiedCalls.toString().shouldBe(
                "OK(verifiedCalls=[" +
                    "ProductService(#5).storeProduct(id, , continuation {})" +
                    "])"
            )

            // recordedCall:
            // - retValue: Any?
            // - retType: KClass<*>
            // - isRetValueMock: Boolean
            // - matcher: InvocationMatcher
            //   - self: Any
            //   - method: MethodDescription
            //   - args: List<Matcher<Any>>
            //   - allAny: Boolean
        }
    }
})

private fun verifyCalls(
    stubRepository: StubRepository,
    safeToString: SafeToString,
    verificationSequence: List<RecordedCall>,
    verificationParams: VerificationParameters
): MockKGateway.VerificationResult {
    val verifier = UnorderedCallVerifier(stubRepository, safeToString)
    return verifier.verify(
        verificationSequence = verificationSequence,
        params = verificationParams
    )
}

private fun recordCalls(
    callRecorder: CallRecorder,
    stubRepository: StubRepository,
    verificationParams: VerificationParameters,
    mockBlock: (MockKVerificationScope.() -> Unit)?,
    coMockBlock: (suspend MockKVerificationScope.() -> Unit)?
): List<RecordedCall> {
    class DerivedCallRecorder(private val base: CallRecorder) : CallRecorder by base {
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
