import assert from "node:assert/strict";
import { readdir, realpathSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, isAbsolute, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const deepsecEntry = require.resolve("deepsec");
const deepsecRequire = createRequire(deepsecEntry);
const agentRequire = createRequire(
  realpathSync(resolve(dirname(deepsecEntry), "../../@earendil-works/pi-coding-agent/package.json")),
);

for (const [consumer, consumerRequire] of [["DeepSec", deepsecRequire], ["agent", agentRequire]]) {
  test(`${consumer} minimatch supports file exclusions and case-insensitive model patterns`, () => {
    const { minimatch } = consumerRequire("minimatch");
    assert.equal(minimatch("app/src/main/Clock.kt", "**/*.kt"), true);
    assert.equal(minimatch("app/src/main/Clock.kt", "**/*.java"), false);
    assert.equal(minimatch("provider/Model-Large", "provider/model-*", { nocase: true }), true);
  });
}

test("agent glob supports its absolute package discovery options", () => {
  const { globSync } = agentRequire("glob");
  const files = globSync("tests/*.test.mjs", {
    cwd: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
    absolute: true,
    dot: false,
    nodir: false,
  });
  assert.ok(files.length >= 3);
  assert.ok(files.every(isAbsolute));
  assert.ok(files.some((file) => file.endsWith("qs-comma.test.mjs")));
});

test("agent glob stops directory reads when cancelled during traversal", async () => {
  const { glob } = agentRequire("glob");
  const controller = new AbortController();
  const reason = new Error("test cancellation");
  let reads = 0;
  const traversal = glob("**/*", {
    cwd: resolve(dirname(fileURLToPath(import.meta.url)), ".."),
    signal: controller.signal,
    fs: {
      readdir(path, options, callback) {
        reads++;
        if (reads > 1) {
          // Bound a regression to the root's immediate children, never the installed dependency tree.
          queueMicrotask(() => callback(null, []));
          return;
        }
        readdir(path, options, (error, entries) => {
          controller.abort(reason);
          callback(error, entries);
        });
      },
    },
  });
  await assert.rejects(traversal, reason);
  const readsAtRejection = reads;
  await new Promise((done) => setImmediate(done));
  assert.equal(readsAtRejection, 1, "Cancellation must prevent traversal into child directories");
  assert.equal(reads, readsAtRejection, "Directory reads must stop after rejection");
});
