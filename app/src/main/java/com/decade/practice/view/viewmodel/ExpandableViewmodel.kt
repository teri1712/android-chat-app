package com.decade.practice.view.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.components.JobDelay
import com.decade.practice.repository.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.ConnectException

abstract class ExpandableViewmodel<Index, T : ListIndex<Index>>
    (private val jobDelay: JobDelay) : ViewModel() {

    protected open var itemList: List<T> = emptyList()
        set(value) {
            field = value
            _itemFlow.value = value
        }
    private val _itemFlow: MutableStateFlow<List<T>> = MutableStateFlow(emptyList())
    val itemFlow: StateFlow<List<T>> = _itemFlow.asStateFlow()

    private var end = false
    private val _loadingFlow = MutableStateFlow(false)
    val loadingFlow = _loadingFlow.asStateFlow()

    abstract val repository: Repository<T, Index>
    protected open fun append(list: List<T>) {
        itemList += list
    }

    private fun expandInternal() {
        val index = itemList.lastOrNull()?.index
        viewModelScope.launch {
            val list =
                if (index == null) repository.list()
                else repository.list(index).drop(1)

            if (list.isEmpty()) {
                end = true
                return@launch
            }
            append(list)
        }.invokeOnCompletion { throwable ->
            if (throwable is ConnectException) {
                jobDelay.save(viewModelScope) {
                    expandInternal()
                }
            } else {
                _loadingFlow.value = false
            }
        }
    }

    fun expand() {
        if (_loadingFlow.value || end)
            return

        _loadingFlow.value = true
        expandInternal()
    }
}