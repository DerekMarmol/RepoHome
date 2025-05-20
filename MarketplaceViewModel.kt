
package com.petter.application.ui.screens.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petter.application.domain.models.Product
import com.petter.application.domain.models.ProductStatus
import com.petter.application.domain.repositories.AuthRepository
import com.petter.application.domain.usecases.marketplace.ApproveProductUseCase
import com.petter.application.domain.usecases.marketplace.GetAllProductsUseCase
import com.petter.application.domain.usecases.marketplace.GetPendingProductsUseCase
import com.petter.application.domain.usecases.marketplace.GetFavoriteProductsUseCase
import com.petter.application.domain.usecases.marketplace.GetProductByIdUseCase
import com.petter.application.domain.usecases.marketplace.GetUserProductsUseCase
import com.petter.application.domain.usecases.marketplace.RejectProductUseCase
import com.petter.application.domain.usecases.marketplace.SearchProductsUseCase
import com.petter.application.domain.usecases.marketplace.UpdateProductUseCase
import com.petter.application.domain.repositories.MarketplaceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MarketplaceViewModel @Inject constructor(
    private val getAllProductsUseCase: GetAllProductsUseCase,
    private val getPendingProductsUseCase: GetPendingProductsUseCase,
    private val getFavoriteProductsUseCase: GetFavoriteProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val getUserProductsUseCase: GetUserProductsUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val authRepository: AuthRepository,
    private val marketplaceRepository: MarketplaceRepository
) : ViewModel() {

    // Estado para la búsqueda y filtros
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedTags = MutableStateFlow<List<String>>(emptyList())
    val selectedTags: StateFlow<List<String>> = _selectedTags

    private val _minPrice = MutableStateFlow<Double?>(null)
    val minPrice: StateFlow<Double?> = _minPrice

    private val _maxPrice = MutableStateFlow<Double?>(null)
    val maxPrice: StateFlow<Double?> = _maxPrice

    // Estado para el usuario actual
    val currentUserId = MutableStateFlow<String?>(null)

    // Verificar si es administrador
    val isAdmin = currentUserId.map { uid ->
        uid == "T8xHRjRNJbYW6jjuaP35vDtvP9G3"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    // Productos aprobados
    val approvedProducts = combine(
        _searchQuery,
        _selectedTags,
        _minPrice,
        _maxPrice
    ) { query, tags, min, max ->
        SearchParams(query, tags, min, max)
    }.flatMapLatest { params ->
        if (params.query.isEmpty() && params.tags.isEmpty() && params.minPrice == null && params.maxPrice == null) {
            getAllProductsUseCase()
        } else {
            searchProductsUseCase(params.query, params.tags, params.minPrice, params.maxPrice)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Productos pendientes (solo para admin)
    val pendingProducts = isAdmin.flatMapLatest { admin ->
        if (admin) {
            getPendingProductsUseCase()
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Estado para el comentario de aprobación/rechazo
    private val _adminComment = MutableStateFlow("")
    val adminComment: StateFlow<String> = _adminComment

    private val _showRejectDialog = MutableStateFlow<String?>(null) // Almacenará productId si está mostrando diálogo
    val showRejectDialog: StateFlow<String?> = _showRejectDialog

    private val _showApproveDialog = MutableStateFlow<String?>(null) // Almacenará productId si está mostrando diálogo
    val showApproveDialog: StateFlow<String?> = _showApproveDialog

    // Productos favoritos
    val favoriteProducts = currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            getFavoriteProductsUseCase(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Productos propios del usuario (incluyendo pausados, vendidos, etc.)
    val myProducts = currentUserId.flatMapLatest { userId ->
        if (userId != null) {
            getUserProductsUseCase(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Estado de carga
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de error
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // Estado de mensaje de éxito
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage

    init {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            currentUserId.value = uid
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedTags(tags: List<String>) {
        _selectedTags.value = tags
    }

    fun setPriceRange(min: Double?, max: Double?) {
        _minPrice.value = min
        _maxPrice.value = max
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _selectedTags.value = emptyList()
        _minPrice.value = null
        _maxPrice.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // Métodos para manejar la aprobación/rechazo desde la lista
    fun updateAdminComment(comment: String) {
        _adminComment.value = comment
    }

    fun showRejectDialog(productId: String) {
        _showRejectDialog.value = productId
        _adminComment.value = ""
    }

    fun hideRejectDialog() {
        _showRejectDialog.value = null
    }

    fun showApproveDialog(productId: String) {
        _showApproveDialog.value = productId
        _adminComment.value = ""
    }

    fun hideApproveDialog() {
        _showApproveDialog.value = null
    }

    fun approveProduct(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val approveUseCase = ApproveProductUseCase(marketplaceRepository)
                approveUseCase(productId, _adminComment.value.ifBlank { null })
                    .onSuccess {
                        _successMessage.value = "Producto aprobado con éxito"
                    }
                    .onFailure {
                        _error.value = "Error al aprobar el producto: ${it.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                hideApproveDialog()
            }
        }
    }

    fun rejectProduct(productId: String) {
        viewModelScope.launch {
            if (_adminComment.value.isBlank()) {
                _error.value = "Por favor, indica un motivo de rechazo"
                return@launch
            }

            _isLoading.value = true

            try {
                val rejectUseCase = RejectProductUseCase(marketplaceRepository)
                rejectUseCase(productId, _adminComment.value)
                    .onSuccess {
                        _successMessage.value = "Producto rechazado"
                    }
                    .onFailure {
                        _error.value = "Error al rechazar el producto: ${it.message}"
                    }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
                hideRejectDialog()
            }
        }
    }

    // Nuevo método para reactivar un producto pausado o vendido
    fun reactivateProduct(productId: String) {
        viewModelScope.launch {
            _isLoading.value = true

            try {
                val product = getProductByIdUseCase(productId).first()
                if (product != null) {
                    // Actualizar el estado a APPROVED
                    val updatedProduct = product.copy(status = ProductStatus.APPROVED)
                    updateProductUseCase(updatedProduct)
                        .onSuccess {
                            _successMessage.value = "Producto reactivado con éxito"
                        }
                        .onFailure {
                            _error.value = "Error al reactivar el producto: ${it.message}"
                        }
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private data class SearchParams(
        val query: String,
        val tags: List<String>,
        val minPrice: Double?,
        val maxPrice: Double?
    )
}