package dcrustm.ecell.mobile.ui.resources

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.model.ResourceType
import dcrustm.ecell.mobile.domain.model.SharedResource
import dcrustm.ecell.mobile.domain.repository.ResourceRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddResourceViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun save(
        url: String,
        title: String,
        description: String,
        type: ResourceType,
        committee: Committee?,
        onDone: () -> Unit
    ) {
        val user = sessionManager.currentUser.value ?: return
        val normalized = ResourceType.normalizeUrl(url)
        if (!ResourceType.looksLikeUrl(normalized) || title.isBlank()) return

        // Members and heads can share org-wide or with their own committee; the coordinator anywhere.
        val audience = when {
            committee == null -> null
            user.role == ROLE.COORDINATOR -> committee
            else -> user.committee
        }

        _isSaving.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                resourceRepository.addResource(
                    SharedResource(
                        title = title.trim(),
                        description = description.trim(),
                        url = normalized,
                        type = type,
                        committee = audience,
                        addedByUid = user.uid,
                        addedByName = user.name
                    )
                )
            }.onSuccess {
                _isSaving.value = false
                onDone()
            }.onFailure {
                _isSaving.value = false
                _error.value = "Couldn't share it. Check your connection and try again."
            }
        }
    }
}
