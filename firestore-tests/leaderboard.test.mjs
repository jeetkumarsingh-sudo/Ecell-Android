import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';
import { getCountFromServer, collection, query, where } from 'firebase/firestore';

let env;
const now = Date.now();
before(async () => { env = await setup('demo-ecell-leaderboard'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await seed(env, async (db) => {
    for (const m of ['m1', 'm2', 'm3']) {
      await db.collection('attendance').doc(`${m}_web2`).set({ meetingId: m, uid: 'web2', name: 'w', committee: 'WEB_TECH', checkedInAt: now, markedByHost: true });
    }
    await db.collection('tasks').doc('d1').set({ title: 't', committee: 'WEB_TECH', assigneeUid: 'web2', assignedByUid: 'headWeb', status: 'DONE', completedAt: now });
    await db.collection('tasks').doc('d2').set({ title: 't', committee: 'WEB_TECH', assigneeUid: 'web2', assignedByUid: 'headWeb', status: 'TODO', completedAt: 0 });
    await db.collection('pollVotes').doc('p1_web2').set({ pollId: 'p1', voterUid: 'web2', optionIndex: 0, votedAt: now });
  });
});

const mod = (db) => db._delegate;
async function count(q) {
  const snap = await getCountFromServer(q);
  return snap.data().count;
}

test('another member can count attendance, finished tasks and votes', async () => {
  const db = mod(as(env, 'desMem'));
  const a = await count(query(collection(db, 'attendance'), where('uid', '==', 'web2')));
  const t = await count(query(collection(db, 'tasks'), where('assigneeUid', '==', 'web2'), where('status', '==', 'DONE')));
  const v = await count(query(collection(db, 'pollVotes'), where('voterUid', '==', 'web2')));
  if (a !== 3 || t !== 1 || v !== 1) throw new Error(`got ${a}/${t}/${v}`);
});
test('counting unfinished tasks of someone else is denied', async () => {
  await assertFails(getCountFromServer(query(collection(mod(as(env, 'desMem')), 'tasks'), where('assigneeUid', '==', 'web2'))));
});
test('pending users cannot count', async () => {
  await assertFails(getCountFromServer(query(collection(mod(as(env, 'pending')), 'attendance'), where('uid', '==', 'web2'))));
});
