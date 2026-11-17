package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BattleMapId
import com.example.data.model.CatalogItem
import com.example.data.model.ItemCategory
import com.example.data.model.MatchRecord
import com.example.data.model.OwnedItem
import com.example.data.model.RoyaleBannerId
import com.example.data.model.RoyaleSpinRecord
import com.example.data.model.SpinOutcome
import com.example.data.model.UserProfile
import com.example.data.repository.GameDataSource
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class GameActionState(
    val selectedMode: String = "battle_royale",
    val selectedMap: BattleMapId = BattleMapId.ERANGEL,
    val selectedStoreCategory: ItemCategory? = null,
    val storeSearchQuery: String = "",
    val selectedRoyaleBanner: RoyaleBannerId = RoyaleBannerId.DIAMOND_ROYALE,
    val isSpinning: Boolean = false,
    val lastSpinOutcome: SpinOutcome? = null,
    val lastMatchRecord: MatchRecord? = null,
    val statusMessage: String? = null,
    val isBusy: Boolean = false
)

class GameViewModel(
    private val repository: GameDataSource,
    private val currentUserId: String,
    initialDisplayName: String?
) : ViewModel() {

    private val _actionState = MutableStateFlow(GameActionState())
    val actionState: StateFlow<GameActionState> = _actionState.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile>> = repository.observeProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile>> { profile ->
            if (profile != null) UiState.Success(profile) else UiState.Loading
        }
        .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Error observing profile", root)
            emit(UiState.Error(root.message ?: "Failed to load survivor profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val inventoryMap: StateFlow<Map<String, OwnedItem>> = repository.observeInventory(currentUserId)
        .map { list -> list.associateBy { it.itemId } }
        .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing inventory", fsError ?: error)
            emit(emptyMap())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val matchHistory: StateFlow<List<MatchRecord>> = repository.observeMatchHistory(currentUserId)
        .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing match history", fsError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val royaleHistory: StateFlow<List<RoyaleSpinRecord>> = repository.observeRoyaleHistory(currentUserId)
        .catch { error ->
            val fsError = unwrapFirestoreException(error)
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing royale history", fsError ?: error)
            emit(emptyList())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    init {
        viewModelScope.launch {
            try {
                repository.ensureUserProfile(initialDisplayName)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize profile", e)
                _actionState.update { it.copy(statusMessage = e.localizedMessage ?: "Failed to sync profile") }
            }
        }
    }

    fun selectMode(mode: String) {
        _actionState.update { it.copy(selectedMode = mode) }
    }

    fun selectMap(map: BattleMapId) {
        _actionState.update { it.copy(selectedMap = map) }
    }

    fun selectStoreCategory(category: ItemCategory?) {
        _actionState.update { it.copy(selectedStoreCategory = category) }
    }

    fun updateStoreSearch(query: String) {
        _actionState.update { it.copy(storeSearchQuery = query) }
    }

    fun selectRoyaleBanner(banner: RoyaleBannerId) {
        _actionState.update { it.copy(selectedRoyaleBanner = banner) }
    }

    fun clearStatusMessage() {
        _actionState.update { it.copy(statusMessage = null) }
    }

    fun dismissSpinOutcome() {
        _actionState.update { it.copy(lastSpinOutcome = null) }
    }

    fun dismissMatchSummary() {
        _actionState.update { it.copy(lastMatchRecord = null) }
    }

    fun refillUnlimitedCurrency() {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                repository.refillUnlimitedCurrency(profile)
                _actionState.update {
                    it.copy(statusMessage = "Unlimited Vault boosted: +5,000,000 Diamonds & Gold!")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update { it.copy(statusMessage = e.localizedMessage ?: "Refill failed") }
            }
        }
    }

    fun purchaseItem(item: CatalogItem) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val inv = inventoryMap.value
        if (_actionState.value.isBusy) return
        viewModelScope.launch {
            _actionState.update { it.copy(isBusy = true) }
            try {
                repository.purchaseStoreItem(item, profile, inv)
                repository.equipItem(item)
                val msg = if (item.category == ItemCategory.BUNDLE && item.bundleItemIds.isNotEmpty()) {
                    "Unlocked & Equipped ${item.name} (${item.bundleItemIds.size} items)!"
                } else {
                    "Unlocked & Equipped ${item.name}!"
                }
                _actionState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = msg
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = e.localizedMessage ?: "Purchase failed"
                    )
                }
            }
        }
    }

    fun equipItem(item: CatalogItem) {
        viewModelScope.launch {
            try {
                repository.equipItem(item)
                _actionState.update { it.copy(statusMessage = "Equipped ${item.name}") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update { it.copy(statusMessage = e.localizedMessage ?: "Equip failed") }
            }
        }
    }

    fun updateCallsign(newCallsign: String) {
        if (newCallsign.trim().length < 2) {
            _actionState.update { it.copy(statusMessage = "Callsign must be at least 2 characters.") }
            return
        }
        viewModelScope.launch {
            try {
                repository.updateCallsign(newCallsign)
                _actionState.update { it.copy(statusMessage = "Callsign updated!") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update { it.copy(statusMessage = e.localizedMessage ?: "Update failed") }
            }
        }
    }

    fun spinLuckRoyale(isMulti: Boolean) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        val banner = _actionState.value.selectedRoyaleBanner
        if (_actionState.value.isSpinning) return

        viewModelScope.launch {
            _actionState.update { it.copy(isSpinning = true, lastSpinOutcome = null) }
            try {
                delay(900L)
                val outcome = repository.performLuckRoyaleSpin(
                    banner = banner,
                    isMulti = isMulti,
                    profile = profile,
                    existingInventory = inventoryMap.value
                )
                _actionState.update {
                    it.copy(
                        isSpinning = false,
                        lastSpinOutcome = outcome
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update {
                    it.copy(
                        isSpinning = false,
                        statusMessage = e.localizedMessage ?: "Luck Royale spin failed"
                    )
                }
            }
        }
    }

    fun recordMatchResult(
        mode: String,
        placement: Int,
        kills: Int,
        headshots: Int,
        damageDealt: Int,
        survivalSeconds: Int
    ) {
        val profile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            try {
                val record = repository.recordMatchResult(
                    mode = mode,
                    placement = placement,
                    kills = kills,
                    headshots = headshots,
                    damageDealt = damageDealt,
                    survivalSeconds = survivalSeconds,
                    profile = profile
                )
                _actionState.update { it.copy(lastMatchRecord = record) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionState.update {
                    it.copy(statusMessage = e.localizedMessage ?: "Failed to save match stats")
                }
            }
        }
    }

    private fun unwrapFirestoreException(error: Throwable): FirebaseFirestoreException? {
        return generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
    }

    private companion object {
        const val TAG = "GameViewModel"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
