package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.Announcement
import kotlinx.coroutines.flow.Flow

interface AnnouncementRepository {

    fun observeAnnouncements(): Flow<List<Announcement>>

    suspend fun postAnnouncement(announcement: Announcement)
}
