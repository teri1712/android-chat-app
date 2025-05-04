package com.decade.practice.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.decade.practice.model.domain.Conversation
import com.decade.practice.model.domain.User
import com.decade.practice.model.presentation.Dialog
import com.decade.practice.session.repository.DialogRepository
import com.decade.practice.session.repository.OnlineRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = OnlineViewModelFactory::class)
class OnlineViewModel @AssistedInject constructor(
      @Assisted private val account: User,
      @Assisted private val onlineRepo: OnlineRepository,
      @Assisted private val dialogRepository: DialogRepository,
) : ViewModel() {
      private val _dialogFlow = MutableStateFlow<List<Dialog>>(emptyList())
      private var loading = false
      val dialogFlow = _dialogFlow.asStateFlow()

      fun refresh() {
            if (loading)
                  return
            loading = true
            viewModelScope.launch {
                  _dialogFlow.value = onlineRepo.list().map { online ->
                        dialogRepository.get(Conversation(account, online.user))
                  }
                  loading = false
            }
      }

}

@AssistedFactory
interface OnlineViewModelFactory {
      fun create(
            account: User,
            onlineRepository: OnlineRepository,
            dialogRepository: DialogRepository
      ): OnlineViewModel
}