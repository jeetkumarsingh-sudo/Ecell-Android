package dcrustm.ecell.mobile.ui.complaint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Complaint
import dcrustm.ecell.mobile.domain.repository.ComplaintRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun submit(title: String, body: String, onDone: () -> Unit) {
        val user = sessionManager.currentUser.value ?: return
        _isSubmitting.value = true
        viewModelScope.launch {
            complaintRepository.submitComplaint(
                Complaint(
                    title = title,
                    body = body,
                    filerUid = user.uid,
                    filerName = user.name,
                    filerCommittee = user.committee
                )
            )
            _isSubmitting.value = false
            onDone()
        }
    }
}
