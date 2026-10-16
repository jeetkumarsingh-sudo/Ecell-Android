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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Which shelf of the library to show. */
sealed class ResourceAudience {
    object Everything : ResourceAudience()
    object OrgWide : ResourceAudience()
    data class OneCommittee(val committee: Committee) : ResourceAudience()
}

data class ResourceFilter(
    val query: String = "",
    val audience: ResourceAudience = ResourceAudience.Everything,
    val type: ResourceType? = null
)

@HiltViewModel
class ResourcesViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val resourceRepository: ResourceRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _filter = MutableStateFlow(ResourceFilter())
    val filter: StateFlow<ResourceFilter> = _filter.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val resources: StateFlow<List<SharedResource>> = combine(
        resourceRepository.observeResources(),
        sessionManager.currentUser,
        _filter
    ) { all, user, filter ->
        if (user == null) return@combine emptyList()
        all.filter { r ->
            val visible = user.role == ROLE.COORDINATOR || r.isOrgWide || r.committee == user.committee
            val audienceOk = when (val a = filter.audience) {
                ResourceAudience.Everything -> true
                ResourceAudience.OrgWide -> r.isOrgWide
                is ResourceAudience.OneCommittee -> r.committee == a.committee
            }
            val typeOk = filter.type == null || r.type == filter.type
            val q = filter.query.trim()
            val queryOk = q.isEmpty() ||
                r.title.contains(q, ignoreCase = true) ||
                r.description.contains(q, ignoreCase = true) ||
                r.addedByName.contains(q, ignoreCase = true) ||
                r.host.contains(q, ignoreCase = true)
            visible && audienceOk && typeOk && queryOk
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: ResourceFilter) {
        _filter.value = filter
    }

    fun canDelete(resource: SharedResource): Boolean {
        val user = sessionManager.currentUser.value ?: return false
        return user.role == ROLE.COORDINATOR ||
            resource.addedByUid == user.uid ||
            (user.role == ROLE.COMMITTEE_HEAD && resource.committee == user.committee)
    }

    fun delete(resource: SharedResource) {
        viewModelScope.launch {
            runCatching { resourceRepository.deleteResource(resource.id) }
                .onSuccess { _messages.tryEmit("Removed \"${resource.title}\"") }
                .onFailure { _messages.tryEmit("Couldn't remove it") }
        }
    }

    fun reportCannotOpen() {
        _messages.tryEmit("No app on this phone can open that link")
    }
}
