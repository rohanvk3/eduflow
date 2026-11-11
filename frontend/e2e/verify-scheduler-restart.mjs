// Real process restarts and @Scheduled execution; no direct ticks or mocked clocks.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import { spawn } from 'node:child_process';
import { setTimeout as delay } from 'node:timers/promises';
import assert from 'node:assert/strict';
const root = path.resolve('..');
const read = file => JSON.parse(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
const state = read(read(path.join(root, '.verification/current.json')).stateFile);
const fixture = read(path.join(state.directory, 'fixtures.json'));
const jdk = read(path.join(root, '.verification/jdk21.json'));
assert.match(state.browserSchema, /^eduflow_test_browser_[a-z0-9]+$/);
assert.ok(path.resolve(state.directory).startsWith(path.join(root, '.verification') + path.sep));
const base = 'http://127.0.0.1:18083/api';
const secret = crypto.randomBytes(32).toString('base64');
let child, output;
async function stop() {
  if (child && child.exitCode === null) {
    const closed = new Promise(resolve => child.once('close', resolve));
    child.kill(); await closed;
  }
  output?.end(); child = null;
}
async function call(token, method, route, data, expected = 200) {
  const response = await fetch(base + route, { method, headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...(data ? { 'Content-Type': 'application/json' } : {}) }, body: data ? JSON.stringify(data) : undefined });
  assert.equal(response.status, expected, `${method} ${route}: ${response.status}`);
  return response.status === 204 ? null : response.json();
}
async function until(probe, description, timeout = 90000) {
  const end = Date.now() + timeout;
  while (Date.now() < end) { const result = await probe(); if (result) return result; await delay(60); }
  throw new Error(`Timed out: ${description}`);
}
async function start(name, enabled) {
  output = fs.createWriteStream(path.join(state.directory, `scheduler-${name}.log`));
  child = spawn(path.join(jdk.jdkHome, 'bin/java.exe'), ['-jar', path.join(root, 'backend/target/backend-0.0.1-SNAPSHOT.jar'), '--spring.profiles.active=prod', `--app.notifications.scheduler-enabled=${enabled}`, '--app.notifications.initial-delay-ms=500', '--app.notifications.poll-ms=250', '--app.notifications.batch-size=1', '--app.notifications.delivery-batches-per-tick=1', '--app.exams.expiry-enabled=false'], {
    windowsHide: true, env: { ...process.env, DB_URL: `jdbc:mysql://127.0.0.1:${state.port}/${state.browserSchema}`, DB_USERNAME: 'eduflow_test', DB_PASSWORD: state.testPassword, JWT_SECRET: secret, FILE_STORAGE_DIR: path.join(state.directory, 'files'), FRONTEND_ORIGINS: 'http://127.0.0.1:15173', SERVER_ADDRESS: '127.0.0.1', SERVER_PORT: '18083' },
  });
  child.stdout.pipe(output, { end: false }); child.stderr.pipe(output, { end: false });
  await until(async () => {
    if (child.exitCode !== null) throw new Error(`JAR failed at phase ${name}; inspect private log`);
    try { return (await fetch(base + '/health')).ok; } catch { return false; }
  }, `phase ${name} startup`, 60000);
}
try {
  await start('before-restart', false);
  const admin = (await call(null, 'POST', '/auth/login', { email: 'admin@e2e.invalid', password: fixture.password })).token;
  const clients = [];
  for (let i = 0; i < 6; i++) {
    const email = `restart-${crypto.randomUUID()}@e2e.invalid`;
    await call(null, 'POST', '/auth/register', { fullName: `Synthetic restart ${i}`, email, password: fixture.password }, 201);
    clients.push((await call(null, 'POST', '/auth/login', { email, password: fixture.password })).token);
  }
  await call(clients[1], 'PUT', '/notifications/preferences', { announcementsEnabled: false, examNotificationsEnabled: true, assignmentNotificationsEnabled: true, feeNotificationsEnabled: true, materialNotificationsEnabled: true, attendanceNotificationsEnabled: true });
  const make = async publishAt => {
    const created = await call(admin, 'POST', '/announcements', { title: `Restart ${crypto.randomUUID()}`, content: 'Synthetic durable scheduler acceptance', audienceType: 'ROLE', targetRole: 'STUDENT', publishAt }, 201);
    return call(admin, 'POST', `/announcements/${created.id}/publish`, { version: created.version });
  };
  const queued = await make(null);
  const scheduled = await make(new Date(Date.now() + 3000).toISOString());
  assert.equal(queued.deliveryComplete, false); assert.equal(queued.delivered, 0);
  assert.equal(scheduled.status, 'SCHEDULED');
  await stop();
  await start('partial-delivery', true);
  const checkpoint = await until(async () => {
    const current = await call(admin, 'GET', `/announcements/${queued.id}`);
    return current.delivered > 0 && !current.deliveryComplete ? current : false;
  }, 'committed partial delivery');
  await stop(); // Terminate the real JVM after an observed committed batch.
  await start('resume-delivery', true);
  for (const notice of [queued, scheduled]) await until(async () => (await call(admin, 'GET', `/announcements/${notice.id}`)).deliveryComplete, 'resumed/scheduled completion');
  for (let tick = 0; tick < 5; tick++) {
    for (const [index, token] of clients.entries()) {
      const list = await call(token, 'GET', '/notifications?type=ANNOUNCEMENT&size=100');
      for (const notice of [queued, scheduled]) assert.equal(list.content.filter(n => n.relatedEntityId === notice.id).length, index === 1 ? 0 : 1, 'exactly once per enabled recipient after restarts');
    }
    await delay(300);
  }
  const notifications = await call(clients[0], 'GET', '/notifications?type=ANNOUNCEMENT&size=100');
  const notification = notifications.content.find(n => n.relatedEntityId === queued.id);
  await call(clients[2], 'PATCH', `/notifications/${notification.id}/read`, undefined, 403);
  const before = (await call(clients[0], 'GET', '/notifications/unread-count')).count;
  const marked = await call(clients[0], 'PATCH', `/notifications/${notification.id}/read`);
  assert.equal(marked.isRead, true);
  assert.equal((await call(clients[0], 'GET', '/notifications/unread-count')).count, before - 1);
  await stop(); await start('read-state-restart', true);
  const restored = await call(clients[0], 'GET', '/notifications?type=ANNOUNCEMENT&size=100');
  assert.equal(restored.content.find(n => n.id === notification.id).isRead, true);
  const proof = { java: jdk.version, profile: 'prod', realJvmStarts: 4, scheduler: '@Scheduled', queuedRecovery: 'passed', partialCommittedRecipients: checkpoint.delivered, scheduledAfterDowntime: 'passed', deduplication: 'passed over repeated ticks/restarts', preferenceOptOut: 'passed', ownership: '403', unreadAndReadStatePersistence: 'passed', controlledSyntheticRecipients: 6 };
  fs.writeFileSync(path.join(state.directory, 'scheduler-restart-result.json'), JSON.stringify(proof, null, 2));
  console.log(JSON.stringify(proof));
} finally { await stop(); }
