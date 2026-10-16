package dcrustm.ecell.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Announcement
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.repository.AnnouncementRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostAnnouncementViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val announcementRepository: AnnouncementRepository
) : ViewModel() {

    private val _isPosting = MutableStateFlow(false)
    val isPosting: StateFlow<Boolean> = _isPosting.asStateFlow()

    fun post(
        title: String,
        body: String,
        committee: Committee?,
        isPinned: Boolean,
        onDone: () -> Unit
    ) {
        val user = sessionManager.currentUser.value ?: return
        _isPosting.value = true
        viewModelScope.launch {
            announcementRepository.postAnnouncement(
                Announcement(
                    title = title,
                    body = body,
                    authorUid = user.uid,
                    authorName = user.name,
                    committee = committee,
                    isPinned = isPinned
                )
            )
            _isPosting.value = false
            onDone()
        }
    }
}
