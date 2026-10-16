package dcrustm.ecell.mobile.domain.usecase.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import dcrustm.ecell.mobile.domain.auth.AuthResult
import dcrustm.ecell.mobile.domain.auth.FirebaseAuthRepository
import javax.inject.Inject

class GoogleSignUpUseCase @Inject constructor(
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val getGoogleIdUseCase: GetGoogleIdUseCase
) {

    // Firebase's signInWithCredential creates the account on first use, so
    // sign-up and login are the same operation underneath.
    suspend operator fun invoke(credentialManager: CredentialManager, context: Activity): AuthResult {
        val googleUser = getGoogleIdUseCase(credentialManager, context)
        return firebaseAuthRepository.signInWithGoogle(googleUser.idToken)
    }

}
