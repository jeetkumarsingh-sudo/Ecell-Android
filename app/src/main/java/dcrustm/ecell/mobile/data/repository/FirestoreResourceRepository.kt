package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toFirestoreMap
import dcrustm.ecell.mobile.data.firebase.toSharedResource
import dcrustm.ecell.mobile.domain.model.SharedResource
import dcrustm.ecell.mobile.domain.repository.ResourceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirestoreResourceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : ResourceRepository {

    private val resourcesRef get() = firestore.collection("resources")

    override fun observeResources(): Flow<List<SharedResource>> =
        resourcesRef.snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toSharedResource() }.sortedByDescending { it.createdAt } }
            .catch { emit(emptyList()) }

    override suspend fun addResource(resource: SharedResource) {
        resourcesRef.add(resource.toFirestoreMap()).awaitResult()
    }

    override suspend fun deleteResource(resourceId: String) {
        resourcesRef.document(resourceId).delete().awaitResult()
    }
}
