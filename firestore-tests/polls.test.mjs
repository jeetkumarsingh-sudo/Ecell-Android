import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';

let env;
const now = Date.now();
const poll = (o = {}) => ({ question: 'Q?', options: ['A', 'B'], createdByUid: 'coord', createdByName: 'Coord', committee: null, createdAt: now, closesAt: 0, isClosed: false, ...o });
const vote = (pollId, uid, i) => ({ pollId, voterUid: uid, optionIndex: i, votedAt: now });

before(async () => { env = await setup('demo-ecell-polls'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await seed(env, async (db) => {
    await db.collection('polls').doc('org').set(poll());
    await db.collection('polls').doc('web').set(poll({ committee: 'WEB_TECH', createdByUid: 'headWeb' }));
    await db.collection('polls').doc('closed').set(poll({ isClosed: true }));
    await db.collection('polls').doc('expired').set(poll({ closesAt: now - 1000 }));
    await db.collection('pollVotes').doc('org_web2').set(vote('org', 'web2', 1));
  });
});

test('coordinator creates org-wide poll', async () => {
  await assertSucceeds(as(env, 'coord').collection('polls').add(poll()));
});
test('head creates poll for own committee only', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('polls').add(poll({ createdByUid: 'headWeb', committee: 'WEB_TECH' })));
  await assertFails(as(env, 'headWeb').collection('polls').add(poll({ createdByUid: 'headWeb', committee: 'DESIGN' })));
  await assertFails(as(env, 'headWeb').collection('polls').add(poll({ createdByUid: 'headWeb', committee: null })));
});
test('member cannot create poll', async () => {
  await assertFails(as(env, 'webMem').collection('polls').add(poll({ createdByUid: 'webMem' })));
});
test('poll needs 2-6 options and must be created open, by self', async () => {
  await assertFails(as(env, 'coord').collection('polls').add(poll({ options: ['A'] })));
  await assertFails(as(env, 'coord').collection('polls').add(poll({ options: ['1','2','3','4','5','6','7'] })));
  await assertFails(as(env, 'coord').collection('polls').add(poll({ isClosed: true })));
  await assertFails(as(env, 'coord').collection('polls').add(poll({ createdByUid: 'headWeb' })));
});
test('approved members read polls, pending cannot', async () => {
  await assertSucceeds(as(env, 'desMem').collection('polls').get());
  await assertFails(as(env, 'pending').collection('polls').get());
});
test('member votes on org poll and can change vote', async () => {
  const db = as(env, 'webMem');
  await assertSucceeds(db.collection('pollVotes').doc('org_webMem').set(vote('org', 'webMem', 0)));
  await assertSucceeds(db.collection('pollVotes').doc('org_webMem').set(vote('org', 'webMem', 1)));
});
test('cannot vote as someone else or with mismatched id', async () => {
  const db = as(env, 'webMem');
  await assertFails(db.collection('pollVotes').doc('org_desMem').set(vote('org', 'desMem', 0)));
  await assertFails(db.collection('pollVotes').doc('org_desMem').set(vote('org', 'webMem', 0)));
  await assertFails(db.collection('pollVotes').doc('web_webMem').set(vote('org', 'webMem', 0)));
});
test('option index must be in range', async () => {
  const db = as(env, 'webMem');
  await assertFails(db.collection('pollVotes').doc('org_webMem').set(vote('org', 'webMem', 2)));
  await assertFails(db.collection('pollVotes').doc('org_webMem').set(vote('org', 'webMem', -1)));
});
test('committee poll: own committee and coordinator vote, others cannot', async () => {
  await assertSucceeds(as(env, 'webMem').collection('pollVotes').doc('web_webMem').set(vote('web', 'webMem', 0)));
  await assertSucceeds(as(env, 'coord').collection('pollVotes').doc('web_coord').set(vote('web', 'coord', 0)));
  await assertFails(as(env, 'desMem').collection('pollVotes').doc('web_desMem').set(vote('web', 'desMem', 0)));
});
test('no votes on closed or expired polls', async () => {
  await assertFails(as(env, 'webMem').collection('pollVotes').doc('closed_webMem').set(vote('closed', 'webMem', 0)));
  await assertFails(as(env, 'webMem').collection('pollVotes').doc('expired_webMem').set(vote('expired', 'webMem', 0)));
});
test('pending user cannot vote', async () => {
  await assertFails(as(env, 'pending').collection('pollVotes').doc('org_pending').set(vote('org', 'pending', 0)));
});
test('votes are readable by approved members (for live results)', async () => {
  await assertSucceeds(as(env, 'desMem').collection('pollVotes').where('pollId', '==', 'org').get());
  await assertSucceeds(as(env, 'webMem').collection('pollVotes').where('voterUid', '==', 'webMem').get());
});
test('creator head can close own poll; other head and members cannot', async () => {
  await assertSucceeds(as(env, 'headWeb').collection('polls').doc('web').update({ isClosed: true }));
  await assertFails(as(env, 'headDes').collection('polls').doc('org').update({ isClosed: true }));
  await assertFails(as(env, 'webMem').collection('polls').doc('org').update({ isClosed: true }));
  await assertSucceeds(as(env, 'coord').collection('polls').doc('org').update({ isClosed: true }));
});
test('deleting votes: coordinator or poll creator only', async () => {
  await assertFails(as(env, 'webMem').collection('pollVotes').doc('org_web2').delete());
  await assertFails(as(env, 'headWeb').collection('pollVotes').doc('org_web2').delete());
  await assertSucceeds(as(env, 'coord').collection('pollVotes').doc('org_web2').delete());
});
