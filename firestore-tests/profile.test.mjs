import { test, before, after, beforeEach } from 'node:test';
import { setup, seed, as, assertFails, assertSucceeds } from './helpers.mjs';

let env;
const update = { phone: '98765', bio: 'hi', year: 3, branch: 'ECE', skills: ['Figma', 'Kotlin'], linkedinUrl: 'https://linkedin.com/in/x', githubUrl: '', portfolioUrl: '' };

before(async () => { env = await setup('demo-ecell-profile'); });
after(async () => { await env.cleanup(); });
beforeEach(async () => { await seed(env); });

test('member edits own year/branch/skills/links (ProfileUpdate)', async () => {
  await assertSucceeds(as(env, 'webMem').collection('users').doc('webMem').update(update));
});
test('member still cannot promote themselves or switch committee', async () => {
  await assertFails(as(env, 'webMem').collection('users').doc('webMem').update({ ...update, role: 'COORDINATOR' }));
  await assertFails(as(env, 'webMem').collection('users').doc('webMem').update({ ...update, committee: 'DESIGN' }));
  await assertFails(as(env, 'webMem').collection('users').doc('webMem').update({ ...update, isApproved: false }));
});
test('member cannot edit someone else', async () => {
  await assertFails(as(env, 'webMem').collection('users').doc('web2').update(update));
});
test('new sign-up doc with the new fields is accepted', async () => {
  await assertSucceeds(as(env, 'newbie').collection('users').doc('newbie').set({ uid: 'newbie', name: 'N', email: 'n@x', phone: '', year: 1, branch: '', committee: 'NONE', role: 'PENDING', bio: '', profilePhotoUrl: '', joinedAt: 1, isApproved: false, skills: [], linkedinUrl: '', githubUrl: '', portfolioUrl: '' }));
});
