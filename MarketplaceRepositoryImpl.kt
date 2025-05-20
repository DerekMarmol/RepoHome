package com.petter.application.data.repositories

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.petter.application.domain.models.FavoriteProduct
import com.petter.application.domain.models.Product
import com.petter.application.domain.models.ProductReview
import com.petter.application.domain.models.ProductStatus
import com.petter.application.domain.repositories.MarketplaceRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class MarketplaceRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : MarketplaceRepository {

    private val productsCollection = firestore.collection("products")
    private val reviewsCollection = firestore.collection("product_reviews")
    private val favoritesCollection = firestore.collection("favorite_products")
    private val usersCollection = firestore.collection("users")

    override fun getAllProducts(includeStatus: List<ProductStatus>): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereIn("status", includeStatus.map { it.name })
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    override fun getProductsPendingApproval(): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("status", ProductStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    override fun getProductById(productId: String): Flow<Product?> = callbackFlow {
        val listener = productsCollection.document(productId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val product = snapshot?.let {
                    if (it.exists()) {
                        it.toObject(Product::class.java)?.copy(id = it.id)
                    } else null
                }

                trySend(product)
            }

        awaitClose { listener.remove() }
    }

    override fun getProductsByUserId(userId: String): Flow<List<Product>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("sellerId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    override fun searchProducts(
        query: String,
        tags: List<String>,
        minPrice: Double?,
        maxPrice: Double?
    ): Flow<List<Product>> = callbackFlow {
        // Base query con status APPROVED
        var baseQuery = productsCollection
            .whereEqualTo("status", ProductStatus.APPROVED.name)

        // No podemos aplicar múltiples condiciones de where a diferentes campos en Firestore
        // Por eso, obtenemos todos los productos aprobados y filtramos en memoria

        val listener = baseQuery
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                var products = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Product::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                // Aplicamos filtros en memoria
                if (query.isNotEmpty()) {
                    val lowerQuery = query.lowercase()
                    products = products.filter { product ->
                        product.title.lowercase().contains(lowerQuery) ||
                                product.description.lowercase().contains(lowerQuery) ||
                                product.tags.any { it.lowercase().contains(lowerQuery) }
                    }
                }

                if (tags.isNotEmpty()) {
                    products = products.filter { product ->
                        product.tags.any { tag -> tags.contains(tag) }
                    }
                }

                if (minPrice != null) {
                    products = products.filter { it.price >= minPrice }
                }

                if (maxPrice != null) {
                    products = products.filter { it.price <= maxPrice }
                }

                trySend(products)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createProduct(product: Product, images: List<ByteArray>): Result<String> = try {
        val imageUrls = if (images.isNotEmpty()) {
            uploadProductImages(images)
        } else {
            emptyList()
        }

        val productWithImages = product.copy(imageUrls = imageUrls)
        val docRef = productsCollection.add(productWithImages).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun uploadProductImages(images: List<ByteArray>): List<String> {
        val urls = mutableListOf<String>()

        images.forEach { imageData ->
            val imageName = "${UUID.randomUUID()}.jpg"
            val imageRef = storage.reference.child("product_images/$imageName")

            imageRef.putBytes(imageData).await()
            val downloadUrl = imageRef.downloadUrl.await()

            urls.add(downloadUrl.toString())
        }

        return urls
    }

    override suspend fun updateProduct(product: Product): Result<Unit> = try {
        productsCollection.document(product.id).set(product).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProductImages(productId: String, images: List<ByteArray>): Result<List<String>> = try {
        val imageUrls = uploadProductImages(images)

        val productSnapshot = productsCollection.document(productId).get().await()
        val product = productSnapshot.toObject(Product::class.java) ?: throw Exception("Producto no encontrado")

        val updatedUrls = product.imageUrls + imageUrls

        productsCollection.document(productId)
            .update("imageUrls", updatedUrls)
            .await()

        Result.success(imageUrls)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteProduct(productId: String): Result<Unit> = try {
        // Primero obtenemos el producto para eliminar sus imágenes del Storage
        val productSnapshot = productsCollection.document(productId).get().await()
        val product = productSnapshot.toObject(Product::class.java)

        // Eliminamos el producto de Firestore
        productsCollection.document(productId).delete().await()

        // Eliminamos reviews y favoritos relacionados
        val batch = firestore.batch()

        // Eliminamos reseñas
        reviewsCollection
            .whereEqualTo("productId", productId)
            .get().await()
            .forEach { batch.delete(it.reference) }

        // Eliminamos favoritos
        favoritesCollection
            .whereEqualTo("productId", productId)
            .get().await()
            .forEach { batch.delete(it.reference) }

        batch.commit().await()

        // No eliminamos las imágenes del storage para evitar romper otras referencias
        // (podríamos implementar un sistema de recolección de basura en el futuro)

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateProductStatus(
        productId: String,
        status: ProductStatus,
        adminComment: String?
    ): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>(
            "status" to status.name,
            "lastUpdatedAt" to Date()
        )

        if (status == ProductStatus.APPROVED) {
            updates["approvedAt"] = Date()
        } else if (status == ProductStatus.REJECTED && adminComment != null) {
            updates["rejectionReason"] = adminComment
        }

        if (adminComment != null) {
            updates["adminComment"] = adminComment
        }

        productsCollection.document(productId)
            .update(updates)
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getFavoriteProducts(userId: String): Flow<List<Product>> = callbackFlow {
        val listener = favoritesCollection
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val favoriteIds = snapshot?.documents?.mapNotNull { doc ->
                    doc.getString("productId")
                } ?: emptyList()

                if (favoriteIds.isEmpty()) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // Usamos otro listener para obtener productos actualizados
                productsCollection
                    .whereEqualTo("status", ProductStatus.APPROVED.name)
                    .addSnapshotListener { productsSnapshot, productsError ->
                        if (productsError != null) {
                            // Solo registramos el error, no cerramos el flujo principal
                            return@addSnapshotListener
                        }

                        val allProducts = productsSnapshot?.documents?.mapNotNull { doc ->
                            doc.toObject(Product::class.java)?.copy(id = doc.id)
                        }?.filter { product ->
                            favoriteIds.contains(product.id)
                        } ?: emptyList()

                        trySend(allProducts)
                    }
            }

        awaitClose { listener.remove() }
    }

    override fun isProductFavorited(productId: String, userId: String): Flow<Boolean> = callbackFlow {
        // Usamos un ID compuesto para el documento de favorito
        val favoriteId = "$userId-$productId"

        val listener = favoritesCollection.document(favoriteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val exists = snapshot?.exists() ?: false
                trySend(exists)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addToFavorites(userId: String, productId: String): Result<Unit> = try {
        // Usamos un ID compuesto para evitar duplicados
        val favoriteId = "$userId-$productId"

        val favorite = FavoriteProduct(
            id = favoriteId,
            userId = userId,
            productId = productId,
            createdAt = Date()
        )

        // Primero verificamos si ya existe para evitar incrementar el contador múltiples veces
        val favoriteSnapshot = favoritesCollection.document(favoriteId).get().await()
        val exists = favoriteSnapshot.exists()

        if (!exists) {
            favoritesCollection.document(favoriteId).set(favorite).await()

            // Incrementamos el contador de favoritos del producto solo si no existía antes
            productsCollection.document(productId)
                .update("favoriteCount", FieldValue.increment(1))
                .await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun removeFromFavorites(userId: String, productId: String): Result<Unit> = try {
        val favoriteId = "$userId-$productId"

        // Verificamos si existe antes de eliminar para evitar decrementar el contador incorrectamente
        val favoriteSnapshot = favoritesCollection.document(favoriteId).get().await()
        val exists = favoriteSnapshot.exists()

        if (exists) {
            favoritesCollection.document(favoriteId).delete().await()

            // Decrementamos el contador de favoritos del producto
            productsCollection.document(productId)
                .update("favoriteCount", FieldValue.increment(-1))
                .await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // En MarketplaceRepositoryImpl.kt
    override fun getReviewsByProductId(productId: String): Flow<List<ProductReview>> = callbackFlow {
        println("DEBUG: getReviewsByProductId para: $productId")

        val listener = reviewsCollection
            .whereEqualTo("productId", productId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("DEBUG: Error en snapshot de reseñas: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    println("DEBUG: Documento de reseña: ${doc.id}")
                    doc.toObject(ProductReview::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                println("DEBUG: Reseñas obtenidas: ${reviews.size}")
                trySend(reviews)
            }

        awaitClose {
            println("DEBUG: Cerrando listener de reseñas")
            listener.remove()
        }
    }

    override fun getReviewsByUserId(userId: String): Flow<List<ProductReview>> = callbackFlow {
        val listener = reviewsCollection
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val reviews = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ProductReview::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(reviews)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createReview(review: ProductReview): Result<String> = try {
        val docRef = reviewsCollection.add(review).await()
        Result.success(docRef.id)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun updateReview(review: ProductReview): Result<Unit> = try {
        reviewsCollection.document(review.id).set(review).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun deleteReview(reviewId: String): Result<Unit> = try {
        reviewsCollection.document(reviewId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun incrementViewCount(productId: String): Result<Unit> = try {
        productsCollection.document(productId)
            .update("viewCount", FieldValue.increment(1))
            .await()

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override fun getSellerStats(userId: String): Flow<Map<String, Int>> {
        return getProductsByUserId(userId).map { products ->
            mapOf(
                "totalProducts" to products.count { it.status == ProductStatus.APPROVED },
                "pendingProducts" to products.count { it.status == ProductStatus.PENDING },
                "totalViews" to products.sumOf { it.viewCount },
                "totalFavorites" to products.sumOf { it.favoriteCount }
            )
        }
    }

    override fun getAllCategories(): Flow<List<String>> = callbackFlow {
        val listener = productsCollection
            .whereEqualTo("status", ProductStatus.APPROVED.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents
                    ?.mapNotNull { it.getString("category") }
                    ?.filter { it.isNotEmpty() }
                    ?.distinct()
                    ?.sorted() ?: emptyList()

                trySend(categories)
            }

        awaitClose { listener.remove() }
    }

    override suspend fun updateSellerProfileImage(userId: String, newProfileImageUrl: String): Result<Unit> = try {
        // Obtenemos todos los productos del vendedor
        val productsQuery = productsCollection
            .whereEqualTo("sellerId", userId)
            .get()
            .await()

        // Si hay productos, actualizamos la foto de perfil en cada uno
        if (!productsQuery.isEmpty) {
            val batch = firestore.batch()

            productsQuery.documents.forEach { doc ->
                batch.update(doc.reference, "sellerProfileImage", newProfileImageUrl)
            }

            batch.commit().await()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}