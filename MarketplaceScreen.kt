package com.petter.application.ui.screens.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.petter.application.domain.models.PaymentMethod
import com.petter.application.domain.models.Product
import com.petter.application.domain.models.ProductStatus
import com.petter.application.ui.utils.Screen
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketplaceScreen(
    navController: NavController,
    viewModel: MarketplaceViewModel = hiltViewModel()
) {
    val approvedProducts by viewModel.approvedProducts.collectAsState()
    val pendingProducts by viewModel.pendingProducts.collectAsState()
    val favoriteProducts by viewModel.favoriteProducts.collectAsState()
    val myProducts by viewModel.myProducts.collectAsState()
    val isAdmin by viewModel.isAdmin.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val adminComment by viewModel.adminComment.collectAsState()
    val showRejectDialog by viewModel.showRejectDialog.collectAsState()
    val showApproveDialog by viewModel.showApproveDialog.collectAsState()
    val selectedTags by viewModel.selectedTags.collectAsState()
    val minPrice by viewModel.minPrice.collectAsState()
    val maxPrice by viewModel.maxPrice.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Mostrar error si existe
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Mostrar mensaje de éxito si existe
    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Diálogo de rechazo
    if (showRejectDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideRejectDialog() },
            title = { Text("Rechazar producto") },
            text = {
                Column {
                    Text("Indica el motivo del rechazo. Esta información será visible para el vendedor.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminComment,
                        onValueChange = { viewModel.updateAdminComment(it) },
                        label = { Text("Motivo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRejectDialog?.let { productId ->
                            viewModel.rejectProduct(productId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Rechazar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRejectDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de aprobación
    if (showApproveDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideApproveDialog() },
            title = { Text("Aprobar producto") },
            text = {
                Column {
                    Text("Puedes añadir un comentario opcional para el vendedor.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = adminComment,
                        onValueChange = { viewModel.updateAdminComment(it) },
                        label = { Text("Comentario (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showApproveDialog?.let { productId ->
                            viewModel.approveProduct(productId)
                        }
                    }
                ) {
                    Text("Aprobar")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideApproveDialog() }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de filtros
    if (showFilterDialog) {
        CategoryFilterDialog(
            currentTags = selectedTags,
            currentMinPrice = minPrice,
            currentMaxPrice = maxPrice,
            onApplyFilters = { tags, minPrice, maxPrice ->
                viewModel.setSelectedTags(tags)
                viewModel.setPriceRange(minPrice, maxPrice)
            },
            onDismiss = { showFilterDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateProduct.route) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Crear Producto",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header con título y botón de búsqueda
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Marketplace",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Row {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    }

                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filtrar"
                        )
                    }
                }
            }

            // Barra de búsqueda expandible
            if (showSearchBar) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    placeholder = { Text("Buscar productos...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Limpiar"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Mostrar filtros activos
            if (selectedTags.isNotEmpty() || minPrice != null || maxPrice != null) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip para borrar todos los filtros
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.clearFilters() },
                            label = { Text("Borrar todos") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Borrar filtros"
                                )
                            }
                        )
                    }

                    // Chips de categorías seleccionadas
                    items(selectedTags) { tag ->
                        FilterChip(
                            selected = true,
                            onClick = {
                                // Quitar solo esta categoría
                                val newTags = selectedTags.toMutableList()
                                newTags.remove(tag)
                                viewModel.setSelectedTags(newTags)
                            },
                            label = { Text(tag) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Quitar filtro"
                                )
                            }
                        )
                    }

                    // Chip para rango de precios
                    if (minPrice != null || maxPrice != null) {
                        item {
                            val priceLabel = when {
                                minPrice != null && maxPrice != null ->
                                    "Q${minPrice} - Q${maxPrice}"
                                minPrice != null ->
                                    "Desde Q${minPrice}"
                                else ->
                                    "Hasta Q${maxPrice}"
                            }

                            FilterChip(
                                selected = true,
                                onClick = {
                                    // Quitar filtro de precio
                                    viewModel.setPriceRange(null, null)
                                },
                                label = { Text(priceLabel) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Quitar filtro de precio"
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Pestañas (si es admin, mostrar pestaña de aprobaciones)
            val tabs = if (isAdmin) {
                listOf("All", "Pendent", "Favorites", "My Products")
            } else {
                listOf("Todos", "Favoritos", "Mis Products")
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(title)
                                if (title == "Pendientes" && pendingProducts.isNotEmpty()) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Badge { Text(pendingProducts.size.toString()) }
                                }
                            }
                        }
                    )
                }
            }

            // Contenido según la pestaña seleccionada
            when (selectedTabIndex) {
                0 -> ProductList(
                    products = approvedProducts,
                    currentUserId = currentUserId,
                    isAdmin = isAdmin,
                    isLoading = isLoading,
                    onProductClick = { product ->
                        navController.navigate(Screen.ProductDetail.createRoute(product.id))
                    }
                )
                1 -> {
                    if (isAdmin) {
                        PendingProductsList(
                            products = pendingProducts,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isLoading = isLoading,
                            onProductClick = { product ->
                                navController.navigate(Screen.ProductDetail.createRoute(product.id))
                            },
                            onApproveClick = { product ->
                                viewModel.showApproveDialog(product.id)
                            },
                            onRejectClick = { product ->
                                viewModel.showRejectDialog(product.id)
                            }
                        )
                    } else {
                        FavoriteProductsList(
                            products = favoriteProducts,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isLoading = isLoading,
                            onProductClick = { product ->
                                navController.navigate(Screen.ProductDetail.createRoute(product.id))
                            }
                        )
                    }
                }
                2 -> {
                    if (isAdmin) {
                        FavoriteProductsList(
                            products = favoriteProducts,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isLoading = isLoading,
                            onProductClick = { product ->
                                navController.navigate(Screen.ProductDetail.createRoute(product.id))
                            }
                        )
                    } else {
                        MyProductsList(
                            products = myProducts,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isLoading = isLoading,
                            onProductClick = { product ->
                                navController.navigate(Screen.ProductDetail.createRoute(product.id))
                            },
                            onReactivateClick = { product ->
                                viewModel.reactivateProduct(product.id)
                            }
                        )
                    }
                }
                3 -> {
                    if (isAdmin) {
                        MyProductsList(
                            products = myProducts,
                            currentUserId = currentUserId,
                            isAdmin = isAdmin,
                            isLoading = isLoading,
                            onProductClick = { product ->
                                navController.navigate(Screen.ProductDetail.createRoute(product.id))
                            },
                            onReactivateClick = { product ->
                                viewModel.reactivateProduct(product.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MyProductCard(
    product: Product,
    onClick: () -> Unit,
    onReactivate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagen principal del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrls.first(),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sin imagen",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Indicador de status
                val statusColor = when (product.status) {
                    ProductStatus.APPROVED -> MaterialTheme.colorScheme.primary
                    ProductStatus.SOLD -> MaterialTheme.colorScheme.tertiary
                    ProductStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                    ProductStatus.PENDING -> MaterialTheme.colorScheme.secondaryContainer
                    ProductStatus.REJECTED -> MaterialTheme.colorScheme.error
                }

                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = when (product.status) {
                            ProductStatus.APPROVED -> "Activo"
                            ProductStatus.SOLD -> "Vendido"
                            ProductStatus.PAUSED -> "Pausado"
                            ProductStatus.PENDING -> "Pendiente"
                            ProductStatus.REJECTED -> "Rechazado"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Información del producto
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Título
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Precio
                Text(
                    text = formatPrice(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botón para reactivar (solo para pausados o vendidos)
                if (product.status == ProductStatus.PAUSED || product.status == ProductStatus.SOLD) {
                    Button(
                        onClick = onReactivate,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Reactivar")
                    }
                }
            }
        }
    }
}

@Composable
fun MyProductsList(
    products: List<Product>,
    currentUserId: String?,
    isAdmin: Boolean,
    isLoading: Boolean,
    onProductClick: (Product) -> Unit,
    onReactivateClick: (Product) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tienes productos publicados",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            MyProductCard(
                product = product,
                onClick = { onProductClick(product) },
                onReactivate = {
                    if (product.status == ProductStatus.PAUSED || product.status == ProductStatus.SOLD) {
                        onReactivateClick(product)
                    }
                }
            )
        }
    }
}

@Composable
fun ProductList(
    products: List<Product>,
    currentUserId: String?,
    isAdmin: Boolean,
    isLoading: Boolean,
    onProductClick: (Product) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay productos disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                isSeller = product.sellerId == currentUserId,
                isAdmin = isAdmin,
                onClick = { onProductClick(product) }
            )
        }
    }
}

@Composable
fun PendingProductsList(
    products: List<Product>,
    currentUserId: String?,
    isAdmin: Boolean,
    isLoading: Boolean,
    onProductClick: (Product) -> Unit,
    onApproveClick: (Product) -> Unit,
    onRejectClick: (Product) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay productos pendientes de aprobación",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            PendingProductCard(
                product = product,
                isSeller = product.sellerId == currentUserId,
                isAdmin = isAdmin,
                onClick = { onProductClick(product) },
                onApproveClick = { onApproveClick(product) },
                onRejectClick = { onRejectClick(product) }
            )
        }
    }
}

@Composable
fun FavoriteProductsList(
    products: List<Product>,
    currentUserId: String?,
    isAdmin: Boolean,
    isLoading: Boolean,
    onProductClick: (Product) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No tienes productos favoritos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(products) { product ->
            ProductCard(
                product = product,
                isSeller = product.sellerId == currentUserId,
                isAdmin = isAdmin,
                onClick = { onProductClick(product) },
                isFavorite = true
            )
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isSeller: Boolean,
    isAdmin: Boolean,
    onClick: () -> Unit,
    isFavorite: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Imagen principal del producto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrls.first(),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sin imagen",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Indicador de favorito
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorito",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Indicador de vendedor o admin
                if (isSeller || isAdmin) {
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = if (isAdmin && !isSeller) "Admin" else "Tu producto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Indicador de status (si no es APPROVED)
                if (product.status != ProductStatus.APPROVED) {
                    val statusColor = when (product.status) {
                        ProductStatus.SOLD -> MaterialTheme.colorScheme.tertiary
                        ProductStatus.PAUSED -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.error
                    }

                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .align(Alignment.BottomStart)
                            .clip(RoundedCornerShape(4.dp))
                            .background(statusColor)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = when (product.status) {
                                ProductStatus.SOLD -> "Vendido"
                                ProductStatus.PAUSED -> "Pausado"
                                ProductStatus.PENDING -> "Pendiente"
                                ProductStatus.REJECTED -> "Rechazado"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }

            // Información del producto
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Título
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Precio
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPrice(product.price),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    if (product.hasLimitedStock && product.stockQuantity != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stock: ${product.stockQuantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción corta
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Etiquetas
                if (product.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(product.tags) { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Pie de tarjeta con métodos de pago e info del vendedor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Métodos de pago
                    Row {
                        if (product.paymentMethods.isNotEmpty()) {
                            product.paymentMethods.take(2).forEach { method ->
                                val icon = Icons.Default.Add // Usamos un ícono genérico en lugar de los específicos

                                Icon(
                                    imageVector = icon,
                                    contentDescription = method.name,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            if (product.paymentMethods.size > 2) {
                                Text(
                                    text = "+${product.paymentMethods.size - 2}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Info del vendedor
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.sellerProfileImage.isNotEmpty()) {
                            AsyncImage(
                                model = product.sellerProfileImage,
                                contentDescription = "Foto de ${product.sellerName}",
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        Text(
                            text = product.sellerName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PendingProductCard(
    product: Product,
    isSeller: Boolean,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Usamos el mismo diseño que ProductCard
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                if (product.imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = product.imageUrls.first(),
                        contentDescription = product.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Sin imagen",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Indicador de pendiente
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Información del producto
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Título
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Precio
                Text(
                    text = formatPrice(product.price),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Descripción corta
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Info del vendedor
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (product.sellerProfileImage.isNotEmpty()) {
                        AsyncImage(
                            model = product.sellerProfileImage,
                            contentDescription = "Foto de ${product.sellerName}",
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Text(
                        text = "Vendedor: ${product.sellerName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botones de acción para el administrador
                if (isAdmin) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = onRejectClick,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Cancel,
                                contentDescription = "Rechazar"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rechazar")
                        }

                        Button(
                            onClick = onApproveClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Aprobar"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Aprobar")
                        }
                    }
                }
            }
        }
    }
}

// Función para formatear el precio en quetzales
fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("es", "GT"))
    return format.format(price)
}