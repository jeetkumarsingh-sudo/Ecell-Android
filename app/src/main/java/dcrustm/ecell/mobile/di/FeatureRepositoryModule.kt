package dcrustm.ecell.mobile.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dcrustm.ecell.mobile.data.repository.FirestoreAttendanceRepository
import dcrustm.ecell.mobile.data.repository.FirestorePollRepository
import dcrustm.ecell.mobile.data.repository.FirestoreTaskRepository
import dcrustm.ecell.mobile.domain.repository.AttendanceRepository
import dcrustm.ecell.mobile.domain.repository.PollRepository
import dcrustm.ecell.mobile.domain.repository.TaskRepository
import javax.inject.Singleton

/** Repositories for the Hub features (polls, tasks, attendance, resources, leaderboard). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FeatureRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPollRepository(impl: FirestorePollRepository): PollRepository

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: FirestoreTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindAttendanceRepository(impl: FirestoreAttendanceRepository): AttendanceRepository

}
