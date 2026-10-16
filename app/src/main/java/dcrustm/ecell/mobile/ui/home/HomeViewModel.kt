package dcrustm.ecell.mobile.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.domain.model.Announcement
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.repository.AnnouncementRepository
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    announcementRepository: AnnouncementRepository
) : ViewModel() {

    val currentUser: StateFlow<Member?> = sessionManager.currentUser

    val announcements: StateFlow<List<Announcement>> = combine(
        announcementRepository.observeAnnouncements(),
        sessionManager.currentUser
    ) { all, user ->
        val sorted = all.sortedWith(
            compareByDescending<Announcement> { it.isPinned }.thenByDescending { it.createdAt }
        )
        when {
            user == null -> emptyList()
            sessionManager.isCoordinator -> sorted
            else -> sorted.filter { it.isOrgWide || it.committee == user.committee }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
