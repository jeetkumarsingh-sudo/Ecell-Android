package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toAnnouncement
import dcrustm.ecell.mobile.data.firebase.toFirestoreMap
import dcrustm.ecell.mobile.domain.model.Announcement
import dcrustm.ecell.mobile.domain.repository.AnnouncementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FirestoreAnnouncementRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : AnnouncementRepository {

    private val collectionRef get() = firestore.collection("announcements")

    override fun observeAnnouncements(): Flow<List<Announcement>> =
        collectionRef.orderBy("createdAt", Query.Direction.DESCENDING).snapshots().map { snap ->
            snap.documents.mapNotNull { it.toAnnouncement() }
        }

    override suspend fun postAnnouncement(announcement: Announcement) {
        collectionRef.add(announcement.toFirestoreMap()).awaitResult()
    }
}
