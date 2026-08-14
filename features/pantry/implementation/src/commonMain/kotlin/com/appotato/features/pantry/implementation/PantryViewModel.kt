package com.appotato.features.pantry.implementation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appotato.shared.billing.api.Billing
import com.appotato.shared.billing.api.Entitlement
import com.appotato.shared.billing.api.hasAccessTo
import com.appotato.shared.dispatchers.CoroutineDispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val today: Today,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val _state = MutableStateFlow(PantryState())
    val state: StateFlow<PantryState> = _state.asStateFlow()

    private val _effects = Channel<PantryEffect>(Channel.BUFFERED)
    val effects: Flow<PantryEffect> = _effects.receiveAsFlow()

    init {
        observe()
    }

    fun onIntent(intent: PantryIntent) {
        when (intent) {
            is PantryIntent.NameChanged -> _state.update { it.copy(newItemName = intent.name) }
            is PantryIntent.DaysChanged -> _state.update { it.copy(newItemDays = intent.days) }
            PantryIntent.AddClicked -> addItem()
            is PantryIntent.DeleteClicked -> launchOnIo { repository.remove(intent.id) }
            PantryIntent.UpgradeClicked -> launchOnIo { _effects.send(PantryEffect.PaywallRequested) }
        }
    }

    /**
     * The list and the entitlement are one stream: losing Pro has to shrink the free-slot counter
     * on the same frame the list redraws, not one recomposition later.
     */
    private fun observe() = launchOnIo {
        combine(repository.observeItems(), billing.status) { items, status ->
            items to (Entitlement.Pro in status.entitlements)
        }.collect { (items, isPro) -> onData(items, isPro) }
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
                // Only the name clears: the next item usually keeps the same shelf life.
                _state.update { it.copy(newItemName = "") }
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
        expiresOn = today().plus(days, DateTimeUnit.DAY)
    )

    private fun launchOnIo(block: suspend () -> Unit) = viewModelScope.launch(dispatchers.io) { block() }
}
