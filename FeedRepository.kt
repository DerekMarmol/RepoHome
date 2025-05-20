package com.petter.application.domain.repositories

import com.petter.application.domain.models.Comment
import com.petter.application.domain.models.Post
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface FeedRepository {
    // Posts
    fun getPosts(currentUserId: String): Flow<List<Post>>
    fun getPostById(postId: String): Flow<Post?>
    fun getUserPosts(userId: String): Flow<List<Post>>
    fun getFavoritePosts(userId: String): Flow<List<Post>>
    suspend fun createPost(post: Post, images: List<ByteArray>): Result<String>
    suspend fun updatePost(post: Post): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>

    // Comentarios
    fun getCommentsByPostId(postId: String): Flow<List<Comment>>
    suspend fun addComment(comment: Comment): Result<String>
    suspend fun deleteComment(commentId: String): Result<Unit>

    // Likes
    fun isPostLiked(postId: String, userId: String): Flow<Boolean>
    suspend fun likePost(postId: String, userId: String): Result<Unit>
    suspend fun unlikePost(postId: String, userId: String): Result<Unit>

    // Favoritos
    fun isPostFavorite(postId: String, userId: String): Flow<Boolean>
    suspend fun addToFavorites(postId: String, userId: String): Result<Unit>
    suspend fun removeFromFavorites(postId: String, userId: String): Result<Unit>
}