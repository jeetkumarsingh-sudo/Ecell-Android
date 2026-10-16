package dcrustm.ecell.mobile.domain.repository

import dcrustm.ecell.mobile.domain.model.SharedResource
import kotlinx.coroutines.flow.Flow

interface ResourceRepository {

    fun observeResources(): Flow<List<SharedResource>>

    suspend fun addResource(resource: SharedResource)

    suspend fun deleteResource(resourceId: String)
}
