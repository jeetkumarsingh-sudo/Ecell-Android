package dcrustm.ecell.mobile.domain.auth

interface FirebaseAuthRepository {

    /**
     * Signs in to Firebase with a Google ID token and ensures a `users/{uid}` Firestore
     * document exists. New accounts are bootstrapped as ROLE.PENDING / isApproved=false;
     * the coordinator must approve them (or, for the very first account, be promoted
     * manually in the Firebase console).
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult

    fun signOut()
}
