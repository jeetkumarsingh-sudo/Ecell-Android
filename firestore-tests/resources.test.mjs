import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';

let env;
const res = (o = {}) => ({ title: 'Deck', description: '', url: 'https://docs.google.com/presentation/d/1', type: 'SLIDES', committee: null, addedByUid: 'webMem', addedByName: 'Web Member', createdAt: Date.now(), ...o });

before(async () => { env = await setup('demo-ecell-resources'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => {
  await seed(env, async (db) => {
    await db.collection('resources').doc('r1').set(res({ committee: 'WEB_TECH' }));
    await db.collection('resources').doc('r2').set(res({ addedByUid: 'desMem', committee: 'DESIGN' }));
  });
});

test('member shares org-wide or to own committee', async () => {
  await assertSucceeds(as(env, 'webMem').collection('resources').add(res()));
  await assertSucceeds(as(env, 'webMem').collection('resources').add(res({ committee: 'WEB_TECH' })));
  await assertFails(as(env, 'webMem').collection('resources').add(res({ committee: 'DESIGN' })));
});
test('coordinator shares to any committee', async () => {
  await assertSucceeds(as(env, 'coord').collection('resources').add(res({ addedByUid: 'coord', committee: 'DESIGN' })));
});
test('must be own name and an http(s) link', async () => {
  await assertFails(as(env, 'webMem').collection('resources').add(res({ addedByUid: 'web2' })));
  await assertFails(as(env, 'webMem').collection('resources').add(res({ url: 'javascript:alert(1)' })));
  await assertFails(as(env, 'webMem').collection('resources').add(res({ url: 'intent://x' })));
});
test('pending users cannot read or share', async () => {
  await assertFails(as(env, 'pending').collection('resources').get());
  await assertFails(as(env, 'pending').collection('resources').add(res({ addedByUid: 'pending' })));
});
test('remove: author, head of that committee, coordinator', async () => {
  await assertFails(as(env, 'web2').collection('resources').doc('r1').delete());
  await assertFails(as(env, 'headWeb').collection('resources').doc('r2').delete());
  await assertSucceeds(as(env, 'headWeb').collection('resources').doc('r1').delete());
  await assertSucceeds(as(env, 'desMem').collection('resources').doc('r2').delete());
});
test('no edits after sharing', async () => {
  await assertFails(as(env, 'webMem').collection('resources').doc('r1').update({ url: 'https://evil.example' }));
});
