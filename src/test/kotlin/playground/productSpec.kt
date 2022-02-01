package playground

import io.kotest.core.spec.style.DescribeSpec
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

interface ProductService {
    suspend fun storeProduct(objectId: String, product: ProtoProduct)
    suspend fun deleteProduct(objectId: String)
}

class ProductSpec : DescribeSpec({
    describe("product") {
        it("should mock product service") {
            // given:
            val productService = mockk<ProductService>()
            coEvery { productService.storeProduct(any(), any()) }.returns(Unit)
            coEvery { productService.deleteProduct(any()) }.returns(Unit)

            // when:
            val product = ProtoProduct.newBuilder().build()
            productService.storeProduct("id", product)

            // then:
            coVerify { productService.storeProduct("id", any()) }
        }
    }
})
