package dcrustm.ecell.mobile.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import dcrustm.ecell.mobile.data.firebase.awaitResult
import dcrustm.ecell.mobile.data.firebase.snapshots
import dcrustm.ecell.mobile.data.firebase.toAttendanceRecord
import dcrustm.ecell.mobile.data.firebase.toFirestoreMap
import dcrustm.ecell.mobile.data.firebase.toMeeting
import dcrustm.ecell.mobile.domain.model.AttendanceRecord
import dcrustm.ecell.mobile.domain.model.CheckInResult
import dcrustm.ecell.mobile.domain.model.Committee
import dcrustm.ecell.mobile.domain.model.Meeting
import dcrustm.ecell.mobile.domain.model.Member
import dcrustm.ecell.mobile.domain.repository.AttendanceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.security.SecureRandom
import javax.inject.Inject

/**
 * Collections:
 * - meetings/{id}                    the session itself
 * - meetingCodes/{meetingId}         live 4-digit code, readable by the host only
 * - attendance/{meetingId}_{uid}     who was there
 * - checkins/{meetingId}_{uid}       the code a member typed; write-only proof the rules
 *                                    compare against meetingCodes, so the code never ends
 *                                    up in a document other members can read
 */
class FirestoreAttendanceRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : AttendanceRepository {

    private val meetingsRef get() = firestore.collection("meetings")
    private val codesRef get() = firestore.collection("meetingCodes")
    private val attendanceRef get() = firestore.collection("attendance")
    private val checkinsRef get() = firestore.collection("checkins")

    private val random = SecureRandom()

    private fun newCode(): String = random.nextInt(10_000).toString().padStart(4, '0')

    private fun recordId(meetingId: String, uid: String) = "${meetingId}_$uid"

    override fun observeMeetings(): Flow<List<Meeting>> =
        meetingsRef.snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toMeeting() }.sortedByDescending { it.startedAt } }
            .catch { emit(emptyList()) }

    override fun observeMeeting(meetingId: String): Flow<Meeting?> =
        meetingsRef.document(meetingId).snapshots()
            .map { it.toMeeting() }
            .catch { emit(null) }

    override fun observeAttendance(meetingId: String): Flow<List<AttendanceRecord>> =
        attendanceRef.whereEqualTo("meetingId", meetingId).snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toAttendanceRecord() }.sortedBy { it.checkedInAt } }
            .catch { emit(emptyList()) }

    override fun observeAttendanceOf(uid: String): Flow<List<AttendanceRecord>> =
        attendanceRef.whereEqualTo("uid", uid).snapshots()
            .map { snap -> snap.documents.mapNotNull { it.toAttendanceRecord() } }
            .catch { emit(emptyList()) }

    override fun observeCheckInCode(meetingId: String): Flow<String?> =
        codesRef.document(meetingId).snapshots()
            .map { it.getString("code") }
            .catch { emit(null) }

    override suspend fun startMeeting(title: String, committee: Committee?, host: Member): String {
        val meetingRef = meetingsRef.document()
        val now = System.currentTimeMillis()
        val meeting = Meeting(
            title = title,
            committee = committee,
            hostUid = host.uid,
            hostName = host.name,
            startedAt = now
        )
        val hostRecord = AttendanceRecord(
            meetingId = meetingRef.id,
            uid = host.uid,
            name = host.name,
            committee = host.committee,
            checkedInAt = now,
            markedByHost = true
        )
        firestore.batch()
            .set(meetingRef, meeting.toFirestoreMap())
            .set(codesRef.document(meetingRef.id), mapOf("code" to newCode(), "hostUid" to host.uid))
            .set(attendanceRef.document(recordId(meetingRef.id, host.uid)), hostRecord.toFirestoreMap())
            .commit()
            .awaitResult()
        return meetingRef.id
    }

    override suspend fun regenerateCode(meetingId: String) {
        codesRef.document(meetingId).update("code", newCode()).awaitResult()
    }

    override suspend fun setCheckInOpen(meetingId: String, open: Boolean) {
        meetingsRef.document(meetingId).update("checkInOpen", open).awaitResult()
    }

    override suspend fun endMeeting(meetingId: String) {
        meetingsRef.document(meetingId).update(
            mapOf("endedAt" to System.currentTimeMillis(), "checkInOpen" to false)
        ).awaitResult()
    }

    override suspend fun checkIn(meeting: Meeting, member: Member, code: String): CheckInResult {
        if (!meeting.isLive || !meeting.checkInOpen) return CheckInResult.Closed
        val id = recordId(meeting.id, member.uid)

        val existing = runCatching { attendanceRef.document(id).get().awaitResult() }.getOrNull()
        if (existing?.exists() == true) return CheckInResult.AlreadyCheckedIn

        val now = System.currentTimeMillis()
        val record = AttendanceRecord(
            meetingId = meeting.id,
            uid = member.uid,
            name = member.name,
            committee = member.committee,
            checkedInAt = now,
            markedByHost = false
        )
        return try {
            firestore.batch()
                .set(
                    checkinsRef.document(id),
                    mapOf("meetingId" to meeting.id, "uid" to member.uid, "code" to code.trim(), "at" to now)
                )
                .set(attendanceRef.document(id), record.toFirestoreMap())
                .commit()
                .awaitResult()
            CheckInResult.Success
        } catch (e: FirebaseFirestoreException) {
            // The rules reject the whole batch when the code doesn't match (or check-in just closed).
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) CheckInResult.WrongCode
            else CheckInResult.Failed(e.message ?: "Check-in failed")
        } catch (e: Exception) {
            CheckInResult.Failed(e.message ?: "Check-in failed")
        }
    }

    override suspend fun markPresent(meetingId: String, member: Member) {
        val record = AttendanceRecord(
            meetingId = meetingId,
            uid = member.uid,
            name = member.name,
            committee = member.committee,
            markedByHost = true
        )
        attendanceRef.document(recordId(meetingId, member.uid)).set(record.toFirestoreMap()).awaitResult()
    }

    override suspend fun removeAttendance(meetingId: String, uid: String) {
        attendanceRef.document(recordId(meetingId, uid)).delete().awaitResult()
    }
}
