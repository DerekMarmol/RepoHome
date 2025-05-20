package com.petter.application.ui.screens.feed

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petter.application.domain.models.Comment
import com.petter.application.domain.models.Post
import com.petter.application.domain.repositories.AuthRepository
import com.petter.application.domain.repositories.FeedRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _feedState = MutableStateFlow(FeedState())
    val feedState: StateFlow<FeedState> = _feedState

    private val _postDetailState = MutableStateFlow(PostDetailState())
    val postDetailState: StateFlow<PostDetailState> = _postDetailState

    private val _postLikeStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val postLikeStates: StateFlow<Map<String, Boolean>> = _postLikeStates

    private val _createPostState = MutableStateFlow(CreatePostState())
    val createPostState: StateFlow<CreatePostState> = _createPostState

    private var _currentUserId: String = ""
    val currentUserId: String get() = _currentUserId

    init {
        viewModelScope.launch {
            _currentUserId = authRepository.getCurrentUserId() ?: ""
            loadFeed()
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _feedState.update { it.copy(isLoading = true) }

            try {
                feedRepository.getPosts(currentUserId).collectLatest { posts ->
                    _feedState.update {
                        it.copy(
                            posts = posts,
                            isLoading = false,
                            error = null
                        )
                    }

                    // Verificar estado de likes para cada post
                    if (currentUserId.isNotEmpty()) {
                        checkPostLikes(posts.map { it.id })
                    }
                }
            } catch (e: Exception) {
                _feedState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun checkPostLikes(postIds: List<String>) {
        viewModelScope.launch {
            postIds.forEach { postId ->
                feedRepository.isPostLiked(postId, currentUserId).collectLatest { isLiked ->
                    _postLikeStates.update { currentMap ->
                        currentMap + (postId to isLiked)
                    }
                }
            }
        }
    }

    fun loadPostDetail(postId: String) {
        viewModelScope.launch {
            _postDetailState.update { it.copy(isLoading = true) }

            try {
                // Cargar post
                feedRepository.getPostById(postId).collectLatest { post ->
                    _postDetailState.update {
                        it.copy(
                            post = post,
                            isLoading = false,
                            error = null
                        )
                    }

                    // Verificar si le ha dado like
                    if (post != null && currentUserId.isNotEmpty()) {
                        checkLikeAndFavorite(postId)
                        loadComments(postId)
                    }
                }
            } catch (e: Exception) {
                _postDetailState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    private fun checkLikeAndFavorite(postId: String) {
        viewModelScope.launch {
            feedRepository.isPostLiked(postId, currentUserId).collectLatest { isLiked ->
                _postDetailState.update { it.copy(isLiked = isLiked) }
            }
        }

        viewModelScope.launch {
            feedRepository.isPostFavorite(postId, currentUserId).collectLatest { isFavorite ->
                _postDetailState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    private fun loadComments(postId: String) {
        viewModelScope.launch {
            feedRepository.getCommentsByPostId(postId).collectLatest { comments ->
                _postDetailState.update { it.copy(comments = comments) }
            }
        }
    }

    // En FeedViewModel, asegurémonos de que haya una única función para dar like:
    fun likePost(postId: String) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                // Verificar el estado actual del like
                val isLiked = if (_postDetailState.value.post?.id == postId) {
                    _postDetailState.value.isLiked
                } else {
                    _postLikeStates.value[postId] ?: false
                }

                if (isLiked) {
                    // Actualizar la UI inmediatamente (optimista)
                    if (_postDetailState.value.post?.id == postId) {
                        _postDetailState.update { it.copy(isLiked = false) }
                    }
                    _postLikeStates.update { it + (postId to false) }

                    // Llamar al repositorio
                    feedRepository.unlikePost(postId, currentUserId)
                } else {
                    // Actualizar la UI inmediatamente (optimista)
                    if (_postDetailState.value.post?.id == postId) {
                        _postDetailState.update { it.copy(isLiked = true) }
                    }
                    _postLikeStates.update { it + (postId to true) }

                    // Llamar al repositorio
                    feedRepository.likePost(postId, currentUserId)
                }
            } catch (e: Exception) {
                // Revertir en caso de error
                _postDetailState.update { it.copy(error = e.message) }
            }
        }
    }

    fun toggleFavorite(postId: String) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                val isFavorite = _postDetailState.value.isFavorite

                if (isFavorite) {
                    feedRepository.removeFromFavorites(postId, currentUserId)
                } else {
                    feedRepository.addToFavorites(postId, currentUserId)
                }
            } catch (e: Exception) {
                _postDetailState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addComment(postId: String, content: String) {
        if (currentUserId.isEmpty() || content.isBlank()) return

        viewModelScope.launch {
            _postDetailState.update { it.copy(isSubmittingComment = true) }

            try {
                val comment = Comment(
                    postId = postId,
                    userId = currentUserId,
                    content = content
                )

                val result = feedRepository.addComment(comment)

                _postDetailState.update {
                    it.copy(
                        isSubmittingComment = false,
                        commentText = "",
                        commentError = result.exceptionOrNull()?.message
                    )
                }
            } catch (e: Exception) {
                _postDetailState.update {
                    it.copy(
                        isSubmittingComment = false,
                        commentError = e.message
                    )
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                feedRepository.deleteComment(commentId)
            } catch (e: Exception) {
                _postDetailState.update { it.copy(error = e.message) }
            }
        }
    }

    fun onCommentTextChange(text: String) {
        _postDetailState.update { it.copy(commentText = text) }
    }

    // Métodos para crear una publicación
    fun showCreatePost() {
        _createPostState.update {
            it.copy(
                isVisible = true,
                content = "",
                tags = "",
                location = "",
                images = emptyList(),
                error = null
            )
        }
    }

    fun hideCreatePost() {
        _createPostState.update { it.copy(isVisible = false) }
    }

    fun onPostContentChange(content: String) {
        _createPostState.update { it.copy(content = content) }
    }

    fun onPostTagsChange(tags: String) {
        _createPostState.update { it.copy(tags = tags) }
    }

    fun onPostLocationChange(location: String) {
        _createPostState.update { it.copy(location = location) }
    }

    fun addPostImage(imageBytes: ByteArray) {
        _createPostState.update {
            it.copy(images = it.images + imageBytes)
        }
    }

    fun removePostImage(index: Int) {
        _createPostState.update {
            it.copy(images = it.images.toMutableList().apply { removeAt(index) })
        }
    }

    fun createPost() {
        if (currentUserId.isEmpty()) return

        val content = _createPostState.value.content
        if (content.isBlank() && _createPostState.value.images.isEmpty()) {
            _createPostState.update {
                it.copy(error = "Debes agregar texto o al menos una imagen")
            }
            return
        }

        viewModelScope.launch {
            _createPostState.update { it.copy(isLoading = true) }

            try {
                // Convertir tags de string a lista
                val tagsList = _createPostState.value.tags
                    .split(",", " ", "#")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { if (!it.startsWith("#")) "#$it" else it }

                val post = Post(
                    userId = currentUserId,
                    content = content,
                    tags = tagsList,
                    location = _createPostState.value.location
                )

                val result = feedRepository.createPost(post, _createPostState.value.images)

                if (result.isSuccess) {
                    _createPostState.update {
                        it.copy(
                            isLoading = false,
                            isVisible = false,
                            successMessage = "Publicación creada con éxito"
                        )
                    }

                    // Recargar feed
                    loadFeed()
                } else {
                    _createPostState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exceptionOrNull()?.message
                        )
                    }
                }
            } catch (e: Exception) {
                _createPostState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    // Métodos para editar/eliminar post
    fun showEditPost(post: Post) {
        // Convertir lista de tags a string para la UI
        val tagsString = post.tags.joinToString(", ") {
            it.removePrefix("#")
        }

        _createPostState.update {
            it.copy(
                isVisible = true,
                isEditing = true,
                editPostId = post.id,
                content = post.content,
                tags = tagsString,
                location = post.location,
                error = null
            )
        }
    }

    fun updatePost() {
        if (currentUserId.isEmpty()) return

        val postId = _createPostState.value.editPostId
        if (postId.isEmpty()) return

        viewModelScope.launch {
            _createPostState.update { it.copy(isLoading = true) }

            try {
                // Convertir tags de string a lista
                val tagsList = _createPostState.value.tags
                    .split(",", " ", "#")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .map { if (!it.startsWith("#")) "#$it" else it }

                // Obtener el post original para mantener los campos que no se editan
                val originalPost = feedRepository.getPostById(postId).collectLatest { post ->
                    post?.let {
                        val updatedPost = it.copy(
                            content = _createPostState.value.content,
                            tags = tagsList,
                            location = _createPostState.value.location
                        )

                        val result = feedRepository.updatePost(updatedPost)

                        if (result.isSuccess) {
                            _createPostState.update {
                                it.copy(
                                    isLoading = false,
                                    isVisible = false,
                                    isEditing = false,
                                    successMessage = "Publicación actualizada con éxito"
                                )
                            }

                            // Recargar detalles del post si estamos en la vista de detalle
                            if (_postDetailState.value.post?.id == postId) {
                                loadPostDetail(postId)
                            }

                            // Recargar feed
                            loadFeed()
                        } else {
                            _createPostState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.exceptionOrNull()?.message
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _createPostState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                val result = feedRepository.deletePost(postId)

                if (result.isSuccess) {
                    // Si estamos viendo este post en detalle, volver al feed
                    if (_postDetailState.value.post?.id == postId) {
                        _postDetailState.update {
                            it.copy(
                                post = null,
                                successMessage = "Publicación eliminada con éxito"
                            )
                        }
                    }

                    // Recargar feed
                    loadFeed()
                } else {
                    _postDetailState.update {
                        it.copy(error = result.exceptionOrNull()?.message)
                    }
                }
            } catch (e: Exception) {
                _postDetailState.update { it.copy(error = e.message) }
            }
        }
    }

    fun clearSuccessMessage() {
        _createPostState.update { it.copy(successMessage = null) }
        _postDetailState.update { it.copy(successMessage = null) }
    }

    // Clase de estado para el feed principal
    data class FeedState(
        val posts: List<Post> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    )

    // Clase de estado para los detalles de un post
    data class PostDetailState(
        val post: Post? = null,
        val comments: List<Comment> = emptyList(),
        val isLiked: Boolean = false,
        val isFavorite: Boolean = false,
        val commentText: String = "",
        val isLoading: Boolean = false,
        val isSubmittingComment: Boolean = false,
        val error: String? = null,
        val commentError: String? = null,
        val successMessage: String? = null
    )

    // Clase de estado para crear/editar post
    data class CreatePostState(
        val isVisible: Boolean = false,
        val isEditing: Boolean = false,
        val editPostId: String = "",
        val content: String = "",
        val tags: String = "",
        val location: String = "",
        val images: List<ByteArray> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null
    ) {
        // Implementación de equals/hashCode necesaria debido a ByteArray
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CreatePostState

            if (isVisible != other.isVisible) return false
            if (isEditing != other.isEditing) return false
            if (editPostId != other.editPostId) return false
            if (content != other.content) return false
            if (tags != other.tags) return false
            if (location != other.location) return false
            if (images.size != other.images.size) return false
            for (i in images.indices) {
                if (!images[i].contentEquals(other.images[i])) return false
            }
            if (isLoading != other.isLoading) return false
            if (error != other.error) return false
            if (successMessage != other.successMessage) return false

            return true
        }

        override fun hashCode(): Int {
            var result = isVisible.hashCode()
            result = 31 * result + isEditing.hashCode()
            result = 31 * result + editPostId.hashCode()
            result = 31 * result + content.hashCode()
            result = 31 * result + tags.hashCode()
            result = 31 * result + location.hashCode()
            result = 31 * result + images.hashCode()
            result = 31 * result + isLoading.hashCode()
            result = 31 * result + (error?.hashCode() ?: 0)
            result = 31 * result + (successMessage?.hashCode() ?: 0)
            return result
        }
    }
}