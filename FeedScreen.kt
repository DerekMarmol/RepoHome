package com.petter.application.ui.screens.feed

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.petter.application.domain.models.Post
import com.petter.application.ui.utils.Screen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    navController: NavController,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val feedState by viewModel.feedState.collectAsState()
    val createPostState by viewModel.createPostState.collectAsState()
    val postLikeStates by viewModel.postLikeStates.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Lanzador para seleccionar imágenes
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    viewModel.addPostImage(bytes)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al cargar la imagen: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Observar mensajes de éxito
    LaunchedEffect(createPostState.successMessage) {
        createPostState.successMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearSuccessMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // SwipeRefresh para actualizar el feed
        SwipeRefresh(
            state = rememberSwipeRefreshState(feedState.isLoading),
            onRefresh = { viewModel.loadFeed() }
        ) {
            if (feedState.posts.isEmpty() && !feedState.isLoading) {
                // Mostrar mensaje cuando no hay publicaciones
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "¡Sé el primero en compartir una publicación!",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Mostrar lista de publicaciones
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // En FeedScreen, en el LazyColumn donde se renderizan los posts
                    items(
                        items = feedState.posts,
                        key = { it.id }
                    ) { post ->
                        PostCard(
                            post = post,
                            currentUserId = viewModel.currentUserId,
                            isLiked = postLikeStates[post.id] ?: false,
                            onPostClick = { navController.navigate(Screen.PostDetail.createRoute(post.id)) },
                            onUserProfileClick = { userId ->
                                navController.navigate(Screen.Profile.createRoute(userId))
                            },
                            onLikeClick = { viewModel.likePost(post.id) },
                            onCommentClick = { navController.navigate(Screen.PostDetail.createRoute(post.id)) },
                            onFavoriteClick = { viewModel.toggleFavorite(post.id) },
                            onEditClick = { viewModel.showEditPost(post) },
                            onDeleteClick = { viewModel.deletePost(post.id) }
                        )
                    }
                }
            }
        }

        // Botón flotante para crear publicación
        FloatingActionButton(
            onClick = { viewModel.showCreatePost() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Crear publicación")
        }

        // Diálogo para crear/editar publicación
        if (createPostState.isVisible) {
            CreatePostDialog(
                state = createPostState,
                onContentChange = { viewModel.onPostContentChange(it) },
                onTagsChange = { viewModel.onPostTagsChange(it) },
                onLocationChange = { viewModel.onPostLocationChange(it) },
                onAddImage = { imagePickerLauncher.launch("image/*") },
                onRemoveImage = { viewModel.removePostImage(it) },
                onSave = {
                    if (createPostState.isEditing) {
                        viewModel.updatePost()
                    } else {
                        viewModel.createPost()
                    }
                },
                onDismiss = { viewModel.hideCreatePost() }
            )
        }

        // Mostrar errores
        if (feedState.error != null) {
            LaunchedEffect(feedState.error) {
                Toast.makeText(context, feedState.error, Toast.LENGTH_LONG).show()
            }
        }
    }
}


@Composable
fun PostCard(
    post: Post,
    currentUserId: String,
    isLiked: Boolean,
    onUserProfileClick: (String) -> Unit,
    onPostClick: () -> Unit,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPostOwner = post.userId == currentUserId
    val isAdmin = currentUserId == "T8xHRjRNJbYW6jjuaP35vDtvP9G3" // UID del admin
    val showOptions = isPostOwner || isAdmin
    val showMenu = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable(onClick = onPostClick),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cabecera del post con foto de perfil y nombre de usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Foto de perfil clickeable
                // Alternativa con Box
                Box(
                    modifier = Modifier
                        .clickable { onUserProfileClick(post.userId) }
                ) {
                    AsyncImage(
                        model = post.userProfileImage.ifEmpty { "https://via.placeholder.com/50" },
                        contentDescription = "Profile image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Nombre de usuario y fecha
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.userName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = formatDate(post.createdAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Menú de opciones (editar/eliminar)
                if (showOptions) {
                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }

                    DropdownMenu(
                        expanded = showMenu.value,
                        onDismissRequest = { showMenu.value = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                onEditClick()
                                showMenu.value = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                onDeleteClick()
                                showMenu.value = false
                            }
                        )
                        // Opción adicional solo para admin
                        if (isAdmin) {
                            Divider()
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "🛡️ Admin: Ver detalles",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    // Aquí podríamos navegar a una vista especial de admin
                                    showMenu.value = false
                                }
                            )
                        }
                    }
                }
            }

            // Ubicación (si existe)
            if (post.location.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = post.location,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Imagen del post (si hay)
            if (post.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = post.imageUrls.first(),
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Contenido del post
            if (post.content.isNotEmpty()) {
                Text(
                    text = post.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(8.dp),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags (si hay)
            if (post.tags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    post.tags.take(3).forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text(tag, style = MaterialTheme.typography.bodySmall) },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }

                    if (post.tags.size > 3) {
                        Text(
                            text = "+${post.tags.size - 3}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                        )
                    }
                }
            }

            // Contador de likes y comentarios
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likes}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Comment,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Divider sutil
            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            // Botones de acción (like, comentar, favorito)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = onLikeClick,
                    modifier = Modifier.weight(1f),
                    colors = if (isLiked) {
                        ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.textButtonColors()
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Me gusta")
                }

                TextButton(
                    onClick = onCommentClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = "Comentar"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Comentar",
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Bookmark,
                        contentDescription = "Guardar"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Guardar")
                }
            }
        }
    }
}

@Composable
fun CreatePostDialog(
    state: FeedViewModel.CreatePostState,
    onContentChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.isEditing) "Editar publicación" else "Nueva publicación") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Consejos para tomar fotos (solo en modo creación)
                if (!state.isEditing) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp)
                        ) {
                            Text(
                                text = "Consejos para fotos",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "• Toma fotos con buena iluminación\n" +
                                        "• Asegúrate que tu mascota sea el centro de atención\n" +
                                        "• Usa fondos simples para destacar a tu mascota",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Texto de la publicación
                OutlinedTextField(
                    value = state.content,
                    onValueChange = onContentChange,
                    label = { Text("¿Qué estás pensando?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags
                OutlinedTextField(
                    value = state.tags,
                    onValueChange = onTagsChange,
                    label = { Text("Etiquetas (separadas por comas)") },
                    placeholder = { Text("perro, mascota, juegos...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Ubicación
                OutlinedTextField(
                    value = state.location,
                    onValueChange = onLocationChange,
                    label = { Text("Ubicación (opcional)") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = "Ubicación")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Botón para añadir imágenes (solo en modo creación)
                if (!state.isEditing) {
                    OutlinedButton(
                        onClick = onAddImage,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = "Añadir imagen")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir imagen")
                    }

                    // Mostrar imágenes seleccionadas
                    if (state.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${state.images.size} ${if (state.images.size == 1) "imagen seleccionada" else "imágenes seleccionadas"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Mostrar error si hay
                if (state.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Indicador de carga
                if (state.isLoading) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !state.isLoading && (state.content.isNotBlank() || state.images.isNotEmpty())
            ) {
                Text(if (state.isEditing) "Actualizar" else "Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

// Función para formatear la fecha
@Composable
fun formatDate(date: Date): String {
    val now = Date()
    val diffInMillis = now.time - date.time
    val diffInMinutes = diffInMillis / (60 * 1000)
    val diffInHours = diffInMillis / (60 * 60 * 1000)
    val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

    return when {
        diffInMinutes < 60 -> "$diffInMinutes min"
        diffInHours < 24 -> "$diffInHours h"
        diffInDays < 7 -> "$diffInDays d"
        else -> {
            val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            dateFormat.format(date)
        }
    }
}