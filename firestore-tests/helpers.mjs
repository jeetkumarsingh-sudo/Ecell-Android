import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';

export { assertFails, assertSucceeds };

const rulesPath = fileURLToPath(new URL('../firestore.rules', import.meta.url));

/** Each test file uses its own project id so files can run in parallel against one emulator. */
export function setup(projectId) {
  return initializeTestEnvironment({
    projectId,
    firestore: {
      rules: readFileSync(rulesPath, 'utf8'),
      host: '127.0.0.1',
      port: 8085,
    },
  });
}

export const users = {
  coord:   { role: 'COORDINATOR',    committee: 'NONE',     isApproved: true,  name: 'Coord' },
  headWeb: { role: 'COMMITTEE_HEAD', committee: 'WEB_TECH', isApproved: true,  name: 'Head Web' },
  headDes: { role: 'COMMITTEE_HEAD', committee: 'DESIGN',   isApproved: true,  name: 'Head Design' },
  webMem:  { role: 'MEMBER',         committee: 'WEB_TECH', isApproved: true,  name: 'Web Member' },
  web2:    { role: 'MEMBER',         committee: 'WEB_TECH', isApproved: true,  name: 'Web Member 2' },
  desMem:  { role: 'MEMBER',         committee: 'DESIGN',   isApproved: true,  name: 'Design Member' },
  pending: { role: 'PENDING',        committee: 'NONE',     isApproved: false, name: 'Pending' },
};

/** Wipes the project and seeds one user per role, then runs [extra] with rules disabled. */
export async function seed(env, extra = async () => {}) {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    for (const [uid, u] of Object.entries(users)) {
      await db.collection('users').doc(uid).set({
        uid, email: `${uid}@example.com`, phone: '', year: 2, branch: 'CSE', bio: '',
        profilePhotoUrl: '', joinedAt: 0, ...u,
      });
    }
    await extra(db);
  });
}

export const as = (env, uid) => env.authenticatedContext(uid).firestore();
