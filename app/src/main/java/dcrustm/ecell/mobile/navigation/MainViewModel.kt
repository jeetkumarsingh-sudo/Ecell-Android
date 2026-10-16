package dcrustm.ecell.mobile.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.session.UserSessionManager
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val sessionManager: UserSessionManager
) : ViewModel() {
    val currentUser = sessionManager.currentUser

    fun signOut() = sessionManager.signOut()
}
