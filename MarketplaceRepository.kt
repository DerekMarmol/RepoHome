package com.petter.application.domain.repositories

import com.petter.application.domain.models.FavoriteProduct
import com.petter.application.domain.models.Product
import com.petter.application.domain.models.ProductReview
import com.petter.application.domain.models.ProductStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface MarketplaceRepository {
    // Productos
    fun getAllProducts(includeStatus: List<ProductStatus> = listOf(ProductStatus.APPROVED)): Flow<List<Product>>
    fun getProductsPendingApproval(): Flow<List<Product>>
    fun getProductById(productId: String): Flow<Product?>
    fun getProductsByUserId(userId: String): Flow<List<Product>>
    fun searchProducts(query: String, tags: List<String> = emptyList(), minPrice: Double? = null, maxPrice: Double? = null): Flow<List<Product>>
    suspend fun createProduct(product: Product, images: List<ByteArray> = emptyList()): Result<String>
    suspend fun updateProduct(product: Product): Result<Unit>
    suspend fun updateProductImages(productId: String, images: List<ByteArray>): Result<List<String>>
    suspend fun deleteProduct(productId: String): Result<Unit>
    suspend fun updateProductStatus(productId: String, status: ProductStatus, adminComment: String? = null): Result<Unit>

    // Favoritos
    fun getFavoriteProducts(userId: String): Flow<List<Product>>
    fun isProductFavorited(productId: String, userId: String): Flow<Boolean>
    suspend fun addToFavorites(userId: String, productId: String): Result<Unit>
    suspend fun removeFromFavorites(userId: String, productId: String): Result<Unit>

    // Reseñas
    fun getReviewsByProductId(productId: String): Flow<List<ProductReview>>
    fun getReviewsByUserId(userId: String): Flow<List<ProductReview>>
    suspend fun createReview(review: ProductReview): Result<String>
    suspend fun updateReview(review: ProductReview): Result<Unit>
    suspend fun deleteReview(reviewId: String): Result<Unit>

    // Estadísticas
    suspend fun incrementViewCount(productId: String): Result<Unit>
    fun getSellerStats(userId: String): Flow<Map<String, Int>>

    // Categorías
    fun getAllCategories(): Flow<List<String>>

    // Actualización de perfil en productos
    suspend fun updateSellerProfileImage(userId: String, newProfileImageUrl: String): Result<Unit>
}