package dcrustm.ecell.mobile.ui.complaint

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Complaint
import dcrustm.ecell.mobile.domain.model.ComplaintStatus
import dcrustm.ecell.mobile.domain.repository.ComplaintRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ComplaintInboxViewModel @Inject constructor(
    private val complaintRepository: ComplaintRepository
) : ViewModel() {

    val complaints: StateFlow<List<Complaint>> = complaintRepository.observeAllComplaints()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun respond(id: String, status: ComplaintStatus, response: String) {
        viewModelScope.launch {
            complaintRepository.updateComplaint(id, status, response)
        }
    }
}
