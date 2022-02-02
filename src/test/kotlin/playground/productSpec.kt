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
import io.mockk.impl.recording.CommonCallRecorder
import io.mockk.impl.recording.JvmAutoHinter
import io.mockk.impl.stub.StubRepository
import io.mockk.impl.verify.UnorderedCallVerifier
import io.mockk.mockk

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
            val gateway = MockKGateway.implementation() as JvmMockKGateway
            val stubRepo = gateway.stubRepo
            val safeToString = gateway.stubRepo.safeToString

            val callRecorder = gateway.callRecorder as CommonCallRecorder
            val params = VerificationParameters(
                ordering = Ordering.UNORDERED,
                min = 1,
                max = 1,
                inverse = false,
                timeout = 0
            )

            val recordedCalls = recordCalls(
                callRecorder = callRecorder,
                stubRepo = stubRepo,
                params = params,
                mockBlock = null,
                coMockBlock = {
                    productService.storeProduct(eq("id"), product)
                }
            )
            recordedCalls.toString().shouldBe("[" +
                    "RecordedCall(retValue=Unit(child of #5#8), retType=Unit, isRetValueMock=true " +
                    "matcher=ProductService(#5).storeProduct(eq(id), eq(), any())))" +
                    "]"
            )

            val verifiedCalls = UnorderedCallVerifier(stubRepo, safeToString).verify(
                verificationSequence = recordedCalls,
                params = params
            )
            verifiedCalls.toString().shouldBe("OK(verifiedCalls=[" +
                    "ProductService(#5).storeProduct(id, , continuation {})" +
                    "])")
        }
    }
})

fun recordCalls(
    callRecorder: CallRecorder,
    stubRepo: StubRepository,
    params: VerificationParameters,
    mockBlock: (MockKVerificationScope.() -> Unit)?,
    coMockBlock: (suspend MockKVerificationScope.() -> Unit)?
): List<RecordedCall> {
    class DerivedCallRecorder(base: CallRecorder) : CallRecorder by base {
        override fun reset() {}
    }

    val derivedCallRecorder = DerivedCallRecorder(callRecorder)

    val verifier = VerifyBlockEvaluator(
        callRecorder = { derivedCallRecorder },
        stubRepo = stubRepo,
        autoHinterFactory = ::JvmAutoHinter
    )

    return try {
        verifier.verify(
            params = params,
            mockBlock = mockBlock,
            coMockBlock = coMockBlock,
        )
        derivedCallRecorder.calls.toList()
    } catch (ignore: Throwable) {
        derivedCallRecorder.calls.toList()
    } finally {
        callRecorder.reset()
    }
}
