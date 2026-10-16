package dcrustm.ecell.mobile.navigation

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dcrustm.ecell.mobile.session.UserSessionManager
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RootAppViewModel @Inject constructor(
    sessionManager: UserSessionManager
) : ViewModel() {
    val isSignedIn: StateFlow<Boolean> = sessionManager.isSignedIn
}
