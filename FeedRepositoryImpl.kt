package com.petter.application.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.petter.application.domain.models.Comment
import com.petter.application.domain.models.Favorite
import com.petter.application.domain.models.Like
import com.petter.application.domain.models.Post
import com.petter.application.domain.models.User
import com.petter.application.domain.repositories.FeedRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject

class FeedRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : FeedRepository {

    override fun getPosts(currentUserId: String): Flow<List<Post>> = callbackFlow {
        // Primero obtenemos la lista de usuarios que sigue el usuario actual
        val followingIds = mutableListOf<String>()

        if (currentUserId.isNotEmpty()) {
            val followsQuery = firestore.collection("follows")
                .whereEqualTo("followerId", currentUserId)
                .get()
                .await()

            followingIds.addAll(followsQuery.documents.mapNotNull {
                it.getString("followedId")
            })
        }

        // Añadimos el ID del usuario actual para incluir sus propias publicaciones
        if (currentUserId.isNotEmpty()) {
            followingIds.add(currentUserId)
        }

        try {
            // Consulta principal: Obtener todas las publicaciones recientes
            val allPostsQuery = firestore.collection("posts")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)

            val listener = allPostsQuery.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allPosts = snapshot.documents.mapNotNull {
                        it.toObject(Post::class.java)
                    }

                    // Si el usuario sigue a alguien, damos prioridad a esas publicaciones
                    if (followingIds.isNotEmpty()) {
                        // Separamos posts de usuarios seguidos y otros posts
                        val followingPosts = allPosts.filter { it.userId in followingIds }
                        val otherPosts = allPosts.filter { it.userId !in followingIds }

                        // Combinamos ambas listas dando prioridad a los seguidos
                        val prioritizedPosts = followingPosts + otherPosts

                        trySend(prioritizedPosts)
                    } else {
                        // Si no sigue a nadie, mostramos las publicaciones recientes
                        trySend(allPosts)
                    }
                } else {
                    trySend(emptyList())
                }
            }

            awaitClose { listener.remove() }
        } catch (e: Exception) {
            close(e)
        }
    }

    override fun getPostById(postId: String): Flow<Post?> = callbackFlow {
        val listener = firestore.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val post = snapshot.toObject(Post::class.java)
                    trySend(post)
                } else {
                    trySend(null)
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getUserPosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("posts")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val posts = snapshot.documents.mapNotNull {
                        it.toObject(Post::class.java)
                    }
                    trySend(posts)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    override fun getFavoritePosts(userId: String): Flow<List<Post>> = callbackFlow {
        val listener = firestore.collection("favorites")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val postIds = snapshot.documents.mapNotNull { it.getString("postId") }

                    // Lanzar una corrutina dentro del listener
                    launch {
                        try {
                            if (postIds.isNotEmpty()) {
                                val postBatches = postIds.chunked(10)
                                val allPosts = mutableListOf<Post>()

                                for (batch in postBatches) {
                                    val postsQuery = firestore.collection("posts")
                                        .whereIn("id", batch)
                                        .get()
                                        .await()

                                    allPosts.addAll(postsQuery.documents.mapNotNull {
                                        it.toObject(Post::class.java)
                                    })
                                }

                                allPosts.sortByDescending { it.createdAt }
                                trySend(allPosts)
                            } else {
                                trySend(emptyList())
                            }
                        } catch (e: Exception) {
                            close(e)
                        }
                    }
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun createPost(post: Post, images: List<ByteArray>): Result<String> {
        return try {
            // Obtener información del usuario actual para incluirla en el post
            val userId = post.userId
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("No se pudo obtener la información del usuario"))

            // Crear un nuevo ID para el post
            val postId = firestore.collection("posts").document().id

            // Subir imágenes si hay
            val imageUrls = mutableListOf<String>()

            for (imageBytes in images) {
                val reference = storage.reference.child("post_images/$userId/$postId/${UUID.randomUUID()}")
                reference.putBytes(imageBytes).await()
                val downloadUrl = reference.downloadUrl.await().toString()
                imageUrls.add(downloadUrl)
            }

            // Crear el post con la información del usuario y las URLs de las imágenes
            val newPost = post.copy(
                id = postId,
                userName = user.username,
                userProfileImage = user.profileImageUrl,
                imageUrls = imageUrls,
                createdAt = Date(),
                lastModifiedAt = Date()
            )

            // Guardar el post
            firestore.collection("posts").document(postId).set(newPost).await()

            Result.success(postId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updatePost(post: Post): Result<Unit> {
        return try {
            // Verificar si el usuario tiene permisos para editar
            val currentUserId = auth.currentUser?.uid

            if (currentUserId != post.userId && currentUserId != ADMIN_UID) {
                return Result.failure(Exception("No tienes permiso para editar esta publicación"))
            }

            // Obtener el post actual para preservar datos que no deben cambiarse
            val currentPost = firestore.collection("posts").document(post.id).get().await()
                .toObject(Post::class.java) ?: return Result.failure(Exception("Publicación no encontrada"))

            // Actualizar solo los campos editables manteniendo el resto
            val updatedPost = currentPost.copy(
                content = post.content,
                tags = post.tags,
                location = post.location,
                lastModifiedAt = Date()
            )

            firestore.collection("posts").document(post.id).set(updatedPost).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private val ADMIN_UID = "T8xHRjRNJbYW6jjuaP35vDtvP9G3"
    private fun isCurrentUserAdmin(): Boolean {
        val currentUser = auth.currentUser ?: return false
        return currentUser.uid == ADMIN_UID
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Obtener el post para verificar permisos
            val postDoc = firestore.collection("posts").document(postId).get().await()
            val post = postDoc.toObject(Post::class.java)
                ?: return Result.failure(Exception("Publicación no encontrada"))

            val currentUserId = auth.currentUser?.uid ?: ""

            // Verificar si el usuario tiene permisos para eliminar
            if (currentUserId != post.userId && currentUserId != ADMIN_UID) {
                return Result.failure(Exception("No tienes permiso para eliminar esta publicación"))
            }

            // Eliminar comentarios
            val commentsQuery = firestore.collection("comments")
                .whereEqualTo("postId", postId)
                .get()
                .await()

            val batch = firestore.batch()

            for (commentDoc in commentsQuery.documents) {
                batch.delete(commentDoc.reference)
            }

            // Eliminar likes
            val likesQuery = firestore.collection("likes")
                .whereEqualTo("postId", postId)
                .get()
                .await()

            for (likeDoc in likesQuery.documents) {
                batch.delete(likeDoc.reference)
            }

            // Eliminar favoritos
            val favoritesQuery = firestore.collection("favorites")
                .whereEqualTo("postId", postId)
                .get()
                .await()

            for (favoriteDoc in favoritesQuery.documents) {
                batch.delete(favoriteDoc.reference)
            }

            // Eliminar el post
            batch.delete(firestore.collection("posts").document(postId))

            // Ejecutar las operaciones en batch
            batch.commit().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCommentsByPostId(postId: String): Flow<List<Comment>> = callbackFlow {
        val listener = firestore.collection("comments")
            .whereEqualTo("postId", postId)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull {
                        it.toObject(Comment::class.java)
                    }
                    trySend(comments)
                } else {
                    trySend(emptyList())
                }
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addComment(comment: Comment): Result<String> {
        return try {
            // Obtener información actualizada del usuario
            val userId = comment.userId
            val userDoc = firestore.collection("users").document(userId).get().await()
            val user = userDoc.toObject(User::class.java)
                ?: return Result.failure(Exception("No se pudo obtener la información del usuario"))

            // Crear un nuevo ID para el comentario
            val commentId = firestore.collection("comments").document().id

            // Crear el comentario con la información actualizada del usuario
            val newComment = comment.copy(
                id = commentId,
                userName = user.username,
                userProfileImage = user.profileImageUrl,
                createdAt = Date()
            )

            // Guardar el comentario
            firestore.collection("comments").document(commentId).set(newComment).await()

            // Incrementar el contador de comentarios en el post
            firestore.collection("posts").document(comment.postId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(commentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteComment(commentId: String): Result<Unit> {
        return try {
            // Obtener el comentario
            val commentDoc = firestore.collection("comments").document(commentId).get().await()
            val comment = commentDoc.toObject(Comment::class.java)
                ?: return Result.failure(Exception("Comentario no encontrado"))

            val currentUserId = auth.currentUser?.uid ?: ""

            // Obtener la publicación para verificar si el usuario actual es el dueño
            val postDoc = firestore.collection("posts").document(comment.postId).get().await()
            val post = postDoc.toObject(Post::class.java)

            // Verificar permisos: puedes eliminar si eres el autor del comentario, el dueño del post, o el admin
            val isCommentOwner = currentUserId == comment.userId
            val isPostOwner = post != null && currentUserId == post.userId
            val isAdmin = currentUserId == ADMIN_UID

            if (!isCommentOwner && !isPostOwner && !isAdmin) {
                return Result.failure(Exception("No tienes permiso para eliminar este comentario"))
            }

            // Eliminar el comentario
            firestore.collection("comments").document(commentId).delete().await()

            // Decrementar el contador de comentarios
            firestore.collection("posts").document(comment.postId)
                .update("commentsCount", com.google.firebase.firestore.FieldValue.increment(-1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isPostLiked(postId: String, userId: String): Flow<Boolean> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val likeId = "$userId-$postId"
        val listener = firestore.collection("likes").document(likeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot != null && snapshot.exists())
            }

        awaitClose { listener.remove() }
    }

    override suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val likeId = "$userId-$postId"
            val like = Like(
                id = likeId,
                postId = postId,
                userId = userId,
                createdAt = Date()
            )

            // Usar una operación más simple para evitar errores
            // Añadir el like
            firestore.collection("likes").document(likeId).set(like).await()

            // Incrementar el contador de likes en el post
            firestore.collection("posts").document(postId)
                .update("likes", com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            val likeId = "$userId-$postId"

            // Usar una transacción para garantizar la coherencia
            firestore.runTransaction { transaction ->
                // Verificar si existe el like
                val likeDoc = transaction.get(firestore.collection("likes").document(likeId))
                if (likeDoc.exists()) {
                    // Eliminar el like
                    transaction.delete(firestore.collection("likes").document(likeId))

                    // Decrementar el contador de likes en el post
                    val postRef = firestore.collection("posts").document(postId)
                    transaction.update(postRef, "likes", com.google.firebase.firestore.FieldValue.increment(-1))
                }
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isPostFavorite(postId: String, userId: String): Flow<Boolean> = callbackFlow {
        if (userId.isEmpty()) {
            trySend(false)
            close()
            return@callbackFlow
        }

        val favoriteId = "$userId-$postId"
        val listener = firestore.collection("favorites").document(favoriteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                trySend(snapshot != null && snapshot.exists())
            }

        awaitClose { listener.remove() }
    }

    override suspend fun addToFavorites(postId: String, userId: String): Result<Unit> {
        return try {
            val favoriteId = "$userId-$postId"
            val favorite = Favorite(
                id = favoriteId,
                postId = postId,
                userId = userId,
                createdAt = Date()
            )

            // Verificar si ya existe en favoritos
            val favoriteDoc = firestore.collection("favorites").document(favoriteId).get().await()
            if (!favoriteDoc.exists()) {
                // Añadir a favoritos
                firestore.collection("favorites").document(favoriteId).set(favorite).await()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFromFavorites(postId: String, userId: String): Result<Unit> {
        return try {
            val favoriteId = "$userId-$postId"

            // Eliminar de favoritos
            firestore.collection("favorites").document(favoriteId).delete().await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}