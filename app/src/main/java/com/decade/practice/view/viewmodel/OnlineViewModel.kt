package com.decade.practice.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.model.Online
import com.decade.practice.repository.OnlineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OnlineViewModelFactory::class)
class OnlineViewModel @AssistedInject constructor(
    @Assisted private val onlineRepo: OnlineRepository,
) : ViewModel() {
    private val _onlineFlow = MutableStateFlow<List<Online>>(emptyList())
    private var loading = false
    val onlineFlow = _onlineFlow.asStateFlow()

    fun refresh() {
        if (loading)
            return
        loading = true
        viewModelScope.launch {
            _onlineFlow.value = onlineRepo.list()
            loading = false
        }
    }

}

@AssistedFactory
interface OnlineViewModelFactory {
    fun create(onlineRepository: OnlineRepository): OnlineViewModel
}