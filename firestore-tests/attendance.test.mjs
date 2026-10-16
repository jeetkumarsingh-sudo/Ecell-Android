import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';

let env;
const now = Date.now();
const meeting = (o = {}) => ({ title: 'M', committee: null, hostUid: 'headWeb', hostName: 'Head Web', startedAt: now, endedAt: 0, checkInOpen: true, ...o });
const rec = (mid, uid, o = {}) => ({ meetingId: mid, uid, name: uid, committee: 'WEB_TECH', checkedInAt: now, markedByHost: false, ...o });
const checkin = (mid, uid, code) => ({ meetingId: mid, uid, code, at: now });

async function selfCheckIn(db, mid, uid, code) {
  const id = `${mid}_${uid}`;
  const b = db.batch();
  b.set(db.collection('checkins').doc(id), checkin(mid, uid, code));
  b.set(db.collection('attendance').doc(id), rec(mid, uid));
  return b.commit();
}

before(async () => { env = await setup('demo-ecell-attendance'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await seed(env, async (db) => {
    await db.collection('meetings').doc('web').set(meeting({ committee: 'WEB_TECH' }));
    await db.collection('meetingCodes').doc('web').set({ code: '1234', hostUid: 'headWeb' });
    await db.collection('meetings').doc('org').set(meeting({ hostUid: 'coord' }));
    await db.collection('meetingCodes').doc('org').set({ code: '5555', hostUid: 'coord' });
    await db.collection('meetings').doc('closed').set(meeting({ checkInOpen: false }));
    await db.collection('meetingCodes').doc('closed').set({ code: '1111', hostUid: 'headWeb' });
    await db.collection('meetings').doc('ended').set(meeting({ endedAt: now, checkInOpen: true }));
    await db.collection('meetingCodes').doc('ended').set({ code: '2222', hostUid: 'headWeb' });
    await db.collection('attendance').doc('org_web2').set(rec('org', 'web2'));
    await db.collection('checkins').doc('org_web2').set(checkin('org', 'web2', '5555'));
  });
});

test('head starts own-committee meeting in one batch (meeting + code + own attendance)', async () => {
  const db = as(env, 'headWeb');
  const ref = db.collection('meetings').doc('new1');
  const b = db.batch();
  b.set(ref, meeting({ committee: 'WEB_TECH' }));
  b.set(db.collection('meetingCodes').doc('new1'), { code: '0420', hostUid: 'headWeb' });
  b.set(db.collection('attendance').doc('new1_headWeb'), rec('new1', 'headWeb', { markedByHost: true }));
  await assertSucceeds(b.commit());
});
test('head cannot host another committee or org-wide; member cannot host', async () => {
  await assertFails(as(env, 'headWeb').collection('meetings').doc('x').set(meeting({ committee: 'DESIGN' })));
  await assertFails(as(env, 'headWeb').collection('meetings').doc('x').set(meeting({ committee: null })));
  await assertFails(as(env, 'webMem').collection('meetings').doc('x').set(meeting({ hostUid: 'webMem', committee: 'WEB_TECH' })));
});
test('coordinator hosts org-wide', async () => {
  await assertSucceeds(as(env, 'coord').collection('meetings').doc('x').set(meeting({ hostUid: 'coord' })));
});
test('only host/coordinator can read the code', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('meetingCodes').doc('web').get());
  await assertSucceeds(as(env, 'coord').collection('meetingCodes').doc('web').get());
  await assertFails(as(env, 'webMem').collection('meetingCodes').doc('web').get());
  await assertFails(as(env, 'headDes').collection('meetingCodes').doc('web').get());
});
test('members cannot read other people\'s checkins (code never leaks)', async () => {
  await assertFails(as(env, 'webMem').collection('checkins').doc('org_web2').get());
  await assertFails(as(env, 'web2').collection('checkins').doc('org_web2').get());
  await assertSucceeds(as(env, 'coord').collection('checkins').doc('org_web2').get());
});
test('attendance docs are readable but do not contain the code', async () => {
  const snap = await assertSucceeds(as(env, 'desMem').collection('attendance').doc('org_web2').get());
  if ('code' in snap.data()) throw new Error('code leaked into attendance');
});
test('self check-in with the right code succeeds', async () => {
  await assertSucceeds(selfCheckIn(as(env, 'webMem'), 'web', 'webMem', '1234'));
});
test('wrong code is rejected and leaves nothing behind', async () => {
  await assertFails(selfCheckIn(as(env, 'webMem'), 'web', 'webMem', '9999'));
  await env.withSecurityRulesDisabled(async (ctx) => {
    const d = ctx.firestore();
    if ((await d.collection('attendance').doc('web_webMem').get()).exists) throw new Error('attendance written');
    if ((await d.collection('checkins').doc('web_webMem').get()).exists) throw new Error('checkin written');
  });
});
test('cannot write attendance without the checkin proof, or a checkin alone', async () => {
  const db = as(env, 'webMem');
  await assertFails(db.collection('attendance').doc('web_webMem').set(rec('web', 'webMem')));
  await assertFails(db.collection('checkins').doc('web_webMem').set(checkin('web', 'webMem', '1234')));
});
test('cannot check in to another committee meeting, a closed or an ended one', async () => {
  await assertFails(selfCheckIn(as(env, 'desMem'), 'web', 'desMem', '1234'));
  await assertFails(selfCheckIn(as(env, 'webMem'), 'closed', 'webMem', '1111'));
  await assertFails(selfCheckIn(as(env, 'webMem'), 'ended', 'webMem', '2222'));
});
test('cannot check in someone else or fake markedByHost', async () => {
  const db = as(env, 'webMem');
  const b = db.batch();
  b.set(db.collection('checkins').doc('web_web2'), checkin('web', 'web2', '1234'));
  b.set(db.collection('attendance').doc('web_web2'), rec('web', 'web2'));
  await assertFails(b.commit());
  await assertFails(db.collection('attendance').doc('web_webMem').set(rec('web', 'webMem', { markedByHost: true })));
});
test('host marks a member present and can undo it', async () => {
  const db = as(env, 'headWeb');
  await assertSucceeds(db.collection('attendance').doc('web_web2').set(rec('web', 'web2', { markedByHost: true })));
  await assertSucceeds(db.collection('attendance').doc('web_web2').delete());
  await assertFails(as(env, 'headDes').collection('attendance').doc('org_web2').delete());
});
test('host can regenerate code, close check-in and end meeting; others cannot', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('meetingCodes').doc('web').update({ code: '7777' }));
  await assertSucceeds(as(env, 'headWeb').collection('meetings').doc('web').update({ checkInOpen: false }));
  await assertSucceeds(as(env, 'headWeb').collection('meetings').doc('web').update({ endedAt: now, checkInOpen: false }));
  await assertFails(as(env, 'webMem').collection('meetings').doc('web').update({ checkInOpen: true }));
  await assertFails(as(env, 'webMem').collection('meetingCodes').doc('web').update({ code: '0000' }));
  await assertFails(as(env, 'headWeb').collection('meetings').doc('web').update({ hostUid: 'webMem' }));
});
test('re-check-in after host removed the record', async () => {
  const db = as(env, 'coord');
  await assertSucceeds(db.collection('attendance').doc('org_web2').delete());
  await assertSucceeds(selfCheckIn(as(env, 'web2'), 'org', 'web2', '5555'));
});
test('member history and leaderboard queries are allowed', async () => {
  await assertSucceeds(as(env, 'webMem').collection('attendance').where('uid', '==', 'webMem').get());
  await assertSucceeds(as(env, 'webMem').collection('attendance').where('uid', '==', 'web2').get());
  await assertFails(as(env, 'pending').collection('attendance').where('uid', '==', 'web2').get());
});
