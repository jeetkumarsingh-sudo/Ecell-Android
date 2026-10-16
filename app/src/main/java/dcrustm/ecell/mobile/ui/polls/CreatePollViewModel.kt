package dcrustm.ecell.mobile.ui.polls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.Poll
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.PollRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreatePollViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val pollRepository: PollRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun create(
        question: String,
        options: List<String>,
        committee: Committee?,
        durationMillis: Long,
        onDone: () -> Unit
    ) {
        val user = sessionManager.currentUser.value ?: return
        val cleanOptions = options.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            .take(Poll.MAX_OPTIONS)
        if (question.isBlank() || cleanOptions.size < Poll.MIN_OPTIONS) return

        // Heads can only poll their own committee — the security rules enforce the same thing.
        val audience = if (user.role == ROLE.COORDINATOR) committee else user.committee
        val now = System.currentTimeMillis()

        _isPosting.value = true
        _error.value = null
        viewModelScope.launch {
            runCatching {
                pollRepository.createPoll(
                    Poll(
                        question = question.trim(),
                        options = cleanOptions,
                        createdByUid = user.uid,
                        createdByName = user.name,
                        committee = audience,
                        createdAt = now,
                        closesAt = if (durationMillis > 0) now + durationMillis else 0L
                    )
                )
            }.onSuccess {
                _isPosting.value = false
                onDone()
            }.onFailure {
                _isPosting.value = false
                _error.value = "Couldn't create the poll. Check your connection and try again."
            }
        }
    }
}
