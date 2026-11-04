import assert from "node:assert/strict";
import { createRequire } from "node:module";
import { dirname, isAbsolute, resolve } from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";

const require = createRequire(import.meta.url);
const deepsecEntry = require.resolve("deepsec");
const deepsecRequire = createRequire(deepsecEntry);
const agentRequire = createRequire(
  resolve(dirname(deepsecEntry), "../../@earendil-works/pi-coding-agent/package.json"),
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

test("agent glob rejects an already cancelled traversal", async () => {
  const { glob } = agentRequire("glob");
  const reason = new Error("test cancellation");
  await assert.rejects(glob("**/*", { signal: AbortSignal.abort(reason) }), reason);
});
