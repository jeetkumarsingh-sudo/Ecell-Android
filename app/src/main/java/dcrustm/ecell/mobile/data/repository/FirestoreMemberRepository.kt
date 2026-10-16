package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toMember
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.model.ROLE
import dcrustm.ecell.mobile.domain.repository.MemberRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirestoreMemberRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : MemberRepository {

    private val usersRef get() = firestore.collection("users")

    override fun observeApprovedMembers(): Flow<List<Member>> =
        usersRef.whereEqualTo("isApproved", true).snapshots().map { snap ->
            snap.documents.mapNotNull { it.toMember() }
        }

    override fun observePendingMembers(): Flow<List<Member>> =
        usersRef.whereEqualTo("isApproved", false).snapshots().map { snap ->
            snap.documents.mapNotNull { it.toMember() }
        }

    override fun observeMember(uid: String): Flow<Member?> =
        usersRef.document(uid).snapshots().map { it.toMember() }

    override suspend fun approveMember(uid: String, committee: Committee, role: ROLE) {
        usersRef.document(uid).update(
            mapOf(
                "isApproved" to true,
                "committee" to committee.name,
                "role" to role.name
            )
        ).awaitResult()
    }

    override suspend fun rejectMember(uid: String) {
        usersRef.document(uid).delete().awaitResult()
    }

    override suspend fun updateOwnProfile(uid: String, phone: String, bio: String) {
        usersRef.document(uid).update(
            mapOf("phone" to phone, "bio" to bio)
        ).awaitResult()
    }
}
