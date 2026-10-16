import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';

let env;
const now = Date.now();
const task = (o = {}) => ({ title: 'T', description: '', committee: 'WEB_TECH', assigneeUid: 'webMem', assigneeName: 'Web Member', assignedByUid: 'headWeb', assignedByName: 'Head Web', status: 'TODO', priority: 'MEDIUM', dueAt: 0, createdAt: now, completedAt: 0, ...o });

before(async () => { env = await setup('demo-ecell-tasks'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await seed(env, async (db) => {
    await db.collection('tasks').doc('t1').set(task());
    await db.collection('tasks').doc('t2').set(task({ assigneeUid: 'web2', assigneeName: 'Web Member 2' }));
    await db.collection('tasks').doc('d1').set(task({ committee: 'DESIGN', assigneeUid: 'desMem', assignedByUid: 'headDes' }));
    await db.collection('tasks').doc('done').set(task({ assigneeUid: 'web2', status: 'DONE', completedAt: now }));
  });
});

test('head assigns within own committee only', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('tasks').add(task()));
  await assertFails(as(env, 'headWeb').collection('tasks').add(task({ committee: 'DESIGN', assigneeUid: 'desMem' })));
  // lying about committee
  await assertFails(as(env, 'headWeb').collection('tasks').add(task({ committee: 'WEB_TECH', assigneeUid: 'desMem' })));
});
test('coordinator assigns to anyone; committee must match assignee', async () => {
  await assertSucceeds(as(env, 'coord').collection('tasks').add(task({ committee: 'DESIGN', assigneeUid: 'desMem', assignedByUid: 'coord' })));
  await assertFails(as(env, 'coord').collection('tasks').add(task({ committee: 'WEB_TECH', assigneeUid: 'desMem', assignedByUid: 'coord' })));
});
test('member cannot assign tasks', async () => {
  await assertFails(as(env, 'webMem').collection('tasks').add(task({ assignedByUid: 'webMem', assigneeUid: 'web2' })));
});
test('member reads own tasks via assignee query, not others', async () => {
  await assertSucceeds(as(env, 'webMem').collection('tasks').where('assigneeUid', '==', 'webMem').get());
  await assertFails(as(env, 'webMem').collection('tasks').doc('t2').get());
  await assertFails(as(env, 'webMem').collection('tasks').where('committee', '==', 'WEB_TECH').get());
});
test('head reads committee board, not other committees', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('tasks').where('committee', '==', 'WEB_TECH').get());
  await assertFails(as(env, 'headWeb').collection('tasks').where('committee', '==', 'DESIGN').get());
});
test('coordinator reads everything', async () => {
  await assertSucceeds(as(env, 'coord').collection('tasks').get());
});
test('anyone approved can count DONE tasks (leaderboard)', async () => {
  const q = as(env, 'desMem').collection('tasks').where('assigneeUid', '==', 'web2').where('status', '==', 'DONE');
  await assertSucceeds(q.get());
  await assertFails(as(env, 'pending').collection('tasks').where('status', '==', 'DONE').get());
});
test('assignee can change status only', async () => {
  const db = as(env, 'webMem');
  await assertSucceeds(db.collection('tasks').doc('t1').update({ status: 'IN_PROGRESS', completedAt: 0 }));
  await assertSucceeds(db.collection('tasks').doc('t1').update({ status: 'DONE', completedAt: now }));
  await assertFails(db.collection('tasks').doc('t1').update({ title: 'hacked' }));
  await assertFails(db.collection('tasks').doc('t1').update({ status: 'DONE', assigneeUid: 'web2' }));
  await assertFails(db.collection('tasks').doc('t2').update({ status: 'DONE', completedAt: now }));
});
test('head manages own committee tasks, cannot move them out', async () => {
  const db = as(env, 'headWeb');
  await assertSucceeds(db.collection('tasks').doc('t1').update({ title: 'Edited', priority: 'HIGH' }));
  await assertFails(db.collection('tasks').doc('t1').update({ committee: 'DESIGN' }));
  await assertFails(db.collection('tasks').doc('d1').update({ status: 'DONE' }));
  await assertSucceeds(db.collection('tasks').doc('t2').delete());
  await assertFails(db.collection('tasks').doc('d1').delete());
});
test('member cannot delete own task', async () => {
  await assertFails(as(env, 'webMem').collection('tasks').doc('t1').delete());
});
