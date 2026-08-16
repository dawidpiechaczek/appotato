package com.appotato.features.pantry.implementation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appotato.shared.billing.api.Billing
import com.appotato.shared.billing.api.Entitlement
import com.appotato.features.pantry.implementation.data.toScannedProduct
import com.appotato.shared.billing.api.hasAccessTo
import com.appotato.shared.dispatchers.CoroutineDispatchers
import com.appotato.shared.ingredients.ingredientFromName
import com.appotato.shared.product.lookup.api.ProductLookup
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class PantryViewModel(
    private val repository: PantryRepository,
    private val billing: Billing,
    private val pendingScan: PendingScan,
    private val productLookup: ProductLookup,
    private val today: Today,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _state = MutableStateFlow(PantryState())
    val state: StateFlow<PantryState> = _state.asStateFlow()

    private val _effects = Channel<PantryEffect>(Channel.BUFFERED)
    val effects: Flow<PantryEffect> = _effects.receiveAsFlow()

    init {
        observeItems()
        observeScans()
    }

    fun onIntent(intent: PantryIntent) {
        when (intent) {
            is PantryIntent.NameChanged -> _state.update { it.copy(newItemName = intent.name) }
            is PantryIntent.DaysChanged -> _state.update { it.copy(newItemDays = intent.days) }
            is PantryIntent.QuantityChanged -> _state.update { it.copy(newItemQuantity = intent.quantity) }
            is PantryIntent.CaloriesChanged -> _state.update { it.copy(newItemCalories = intent.calories) }
            is PantryIntent.CategorySelected -> _state.update { it.copy(newItemCategory = intent.category) }
            is PantryIntent.CategoryFilterSelected -> _state.update { it.copy(categoryFilter = intent.category) }
            PantryIntent.AddSheetOpened -> _state.update { it.copy(isAddSheetOpen = true) }
            PantryIntent.AddSheetDismissed ->
                _state.update { it.copy(isAddSheetOpen = false).withScanCleared() }
            PantryIntent.AddClicked -> addItem()
            is PantryIntent.DeleteClicked -> launchOnIo { repository.remove(intent.id) }
            PantryIntent.UpgradeClicked -> launchOnIo { _effects.send(PantryEffect.PaywallRequested) }
        }
    }

    /**
     * The list and the entitlement are one stream: losing Pro has to shrink the free-slot counter
     * on the same frame the list redraws, not one recomposition later.
     */
    private fun observeItems() = launchOnIo {
        combine(repository.observeItems(), billing.status) { items, status ->
            items to (Entitlement.Pro in status.entitlements)
        }.collect { (items, isPro) -> onData(items, isPro) }
    }

    /**
     * A code from the scanner tab opens the add sheet straight away, before anything is known about
     * the product: the form is usable during the lookup, and typing the name by hand always beats
     * waiting for a request that may not come back.
     *
     * Collected one at a time on purpose — the lookup suspends inside `collect`, so a second scan
     * waits rather than racing the first one into the same form.
     *
     * **A code already in the form is dropped, and that is the only thing standing between one scan
     * and a burst of requests.** The camera reports the same label on every frame it can read it in,
     * tens of times a second, and it goes on doing so until the tab switch tears the preview down —
     * which happens a frame or more later, on another thread. [PendingScan]'s latch does not cover
     * that window: it re-opens the moment this collector takes the code, long before the lookup
     * comes back. Without the check below, one steady hand on one barcode is enough to spend a
     * public API's whole per-minute budget in a couple of seconds.
     *
     * It is deliberately keyed on the form rather than on a timer: clearing the form — adding the
     * item, or dismissing the sheet — is what makes the same product scannable again, so a second
     * jar of the same thing works immediately, and a different product is never delayed at all.
     */
    private fun observeScans() = launchOnIo {
        pendingScan.barcode.filterNotNull().collect { barcode ->
            pendingScan.consume()
            if (_state.value.newItemBarcode == barcode) return@collect

            _state.update {
                it.copy(isAddSheetOpen = true, newItemBarcode = barcode, lookup = LookupStatus.InProgress)
            }
            lookUp(barcode)
        }
    }

    private suspend fun lookUp(barcode: String) {
        val result = productLookup.byBarcode(barcode)
        _state.update { state ->
            result.fold(
                onSuccess = { product ->
                    product?.let { state.prefilledWith(it.toScannedProduct()) }
                        ?: state.copy(lookup = LookupStatus.NotFound)
                },
                onFailure = { state.copy(lookup = LookupStatus.Failed) }
            )
        }
    }

    private fun onData(items: List<PantryItem>, isPro: Boolean) {
        val currentDay = today()
        _state.update { state ->
            state.copy(
                isLoading = false,
                isPro = isPro,
                entries = items.map { item ->
                    PantryEntry(
                        item = item,
                        status = item.statusOn(currentDay),
                        daysUntilExpiry = currentDay.daysUntil(item.expiresOn)
                    )
                }
            )
        }
    }

    private fun addItem() {
        val name = _state.value.newItemName.trim()
        val days = _state.value.newItemDaysOrNull
        if (name.isEmpty() || days == null) return

        launchOnIo {
            if (isOverFreeLimit()) {
                _effects.send(PantryEffect.PaywallRequested)
            } else {
                repository.add(newItem(name, days))
                // The category and shelf life stay: shopping comes in runs of similar things.
                _state.update {
                    it.copy(isAddSheetOpen = false, newItemName = "", newItemQuantity = "").withScanCleared()
                }
            }
        }
    }

    /**
     * Counted through the repository rather than off the rendered list — the state may not have
     * caught up with the last insert, and the limit has to hold against a fast double tap.
     */
    private suspend fun isOverFreeLimit(): Boolean =
        !billing.hasAccessTo(Entitlement.Pro) && repository.count() >= FREE_TIER_ITEM_LIMIT

    @OptIn(ExperimentalUuidApi::class)
    private fun newItem(name: String, days: Int) = PantryItem(
        id = Uuid.random().toString(),
        name = name,
        expiresOn = today().plus(days, DateTimeUnit.DAY),
        category = _state.value.newItemCategory,
        quantity = _state.value.newItemQuantity.trim(),
        barcode = _state.value.newItemBarcode,
        caloriesPer100g = _state.value.newItemCaloriesOrNull,
        imageUrl = _state.value.newItemImageUrl,
        // The scan's tags first, the name only as a fallback: tags are a machine vocabulary, while
        // the name is whatever the user settled on. Resolved here rather than as the user types, so
        // an edit made after the lookup landed is still the one that counts.
        ingredientCode = _state.value.newItemIngredientCode ?: ingredientFromName(name)
    )

    private fun launchOnIo(block: suspend () -> Unit) = viewModelScope.launch(dispatchers.io) { block() }
}
