# Ecell-Android
The official Ecell android app for everything related E-Cell @DCRUSTM

---

## Firebase Setup (required before the app will run)

The app uses Firebase Auth (Google Sign-In) + Firestore as its backend.
`app/google-services.json` is gitignored — each developer needs their own
copy from the Firebase console for project `ecell-dcrust`.

### One-time Firebase Console steps

1. **Add your SHA-1 fingerprint** (Google Sign-In won't work without this)
   - Get it: `./gradlew signingReport` → copy the `SHA1` value under the `debug` variant
   - Firebase Console → Project Settings → your Android app → **Add fingerprint** → paste it
   - Firebase then generates an Android-type OAuth client automatically
   - Re-download `google-services.json` afterwards and replace `app/google-services.json`

2. **Publish the Firestore security rules**
   - Firebase Console → Firestore Database → **Rules** tab
   - Paste the contents of [`firestore.rules`](firestore.rules) (repo root) → **Publish**
   - (Or via CLI: `firebase deploy --only firestore:rules`)

3. **Bootstrap the first coordinator account**
   - Build and run the app, sign in once with Google (your own account)
   - This creates a `users/{your-uid}` Firestore document with `role: PENDING`,
     `isApproved: false` — nobody can self-promote to coordinator, by design
   - Firebase Console → Firestore Database → **Data** → `users` collection →
     find your document (matched by name/email) → manually set `role` to
     `"COORDINATOR"` and `isApproved` to `true`
   - From then on, every further approval happens in-app: the coordinator
     approves pending members and assigns them to a committee

### Firestore data model

- `users/{uid}` — name, email, phone, year, branch, committee, role, bio, profilePhotoUrl, joinedAt, isApproved, skills, linkedinUrl, githubUrl, portfolioUrl
- `announcements/{id}` — title, body, authorUid, authorName, committee (`null` = org-wide), createdAt, isPinned, attachmentUrl
- `complaints/{id}` — title, body, filerUid, filerName, filerCommittee, status, response, createdAt
- `polls/{id}` — question, options (2–6), createdByUid, createdByName, committee (`null` = org-wide), createdAt, closesAt (`0` = no deadline), isClosed
- `pollVotes/{pollId}_{uid}` — pollId, voterUid, optionIndex, votedAt (the id pins one vote per member per poll)
- `tasks/{id}` — title, description, committee (the assignee's), assigneeUid, assigneeName, assignedByUid, assignedByName, status (`TODO`/`IN_PROGRESS`/`DONE`), priority, dueAt (`0` = none), createdAt, completedAt
- `meetings/{id}` — title, committee (`null` = org-wide), hostUid, hostName, startedAt, endedAt (`0` = live), checkInOpen
- `meetingCodes/{meetingId}` — code, hostUid (readable only by the host and the coordinator)
- `attendance/{meetingId}_{uid}` — meetingId, uid, name, committee, checkedInAt, markedByHost
- `checkins/{meetingId}_{uid}` — meetingId, uid, code, at (write-only proof the rules compare with `meetingCodes`, so the code never sits in a document members can read)
- `resources/{id}` — title, description, url, type (`DOC`/`SLIDES`/`SHEET`/`PDF`/`VIDEO`/`DESIGN`/`LINK`), committee (`null` = everyone), addedByUid, addedByName, createdAt

### Known trade-off

`firestore.rules` restricts reads mostly at the document level, not per field:
any *approved* member can read another approved member's full profile
document (including phone number), not just the subset the in-app directory
UI shows them. Splitting `users/{uid}` into a public profile doc plus a
`users/{uid}/private/contact` subcollection would let rules restrict
phone/email more tightly (self, coordinator, or committee head of the same
committee only) if that's needed later.

---

## Features

### Market News Dashboard
- **Curated Content:** Displays the latest market news and trends.
- **Offline Caching:** Pre-loads and caches news content for offline access, with updates when connectivity is available using Android's Workmanager API.
- **User-Friendly Interface:** Provides easy filtering and navigation through organized content.

### Meetings Module
- **Schedule Display:** Lists upcoming meetings with details such as time, venue, and host.
- **Meeting Summaries:** Offers neat summaries and key takeaways from past meetings.
- **Push Notifications:** Sends reminders for upcoming events.

### Quiz with Buzzer System
- **Interactive Sessions:** Hosts live quizzes where participants can join using a buzzer.
- **Latency Check:** Determines precisely for latency before starting the quiz.
- **Real-Time Feedback:** Delivers instant quiz responses and scoring updates.

### Local Scoreboard Website
- **Live Score Display:** Serves a local website that showcases real-time quiz scores on larger screens.
- **Responsive Design:** Optimized for projectors, monitors, and various screen sizes.
- **Seamless Access:** Accessible from any device connected to the local network.

---

## Architecture: Clean Architecture

ECell is built on Clean Architecture principles with usecases to streamline the multiple components, ensuring separation of concerns and scalability.

## Roadmap

### Phase 1: Core Features
- [ ] Setup a REST API server to gather and serve market related news in the app.
- [ ] **Market News Dashboard:** Implement offline caching and content display.
- [ ] **Meetings Module:** Develop scheduling, notifications, and summary functionalities which can be done by an Admin.
- [ ] **Quiz Feature:** Build interactive quizzes with a real-time buzzer system using server based approach as of now.
- [ ] **Local Scoreboard:** Create a locally hosted website for live score display using nano httpd library.

### Phase 2: Enhancements
- [ ] Refine UI/UX for better engagement.
- [ ] Consider an offline network based quiz featuers.

### Phase 3: Future Expansion
- [ ] Port to Kotlin Multi platform project for cross-platform(iOS Support as well).
- [ ] Integrate external APIs for dynamic market news.
- [ ] Expand scalability for multi-location and larger events.
- [ ] Connectivity features for E-Cell alumnus network.

---

## Contributing

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a feature or resolve an issue.
3. Commit your changes and push to your branch.
4. Open a pull request for review.

if you find any bugs simply file an issue in the repository as soon as possible. That would be really helpful.

