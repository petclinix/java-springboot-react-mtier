#!/usr/bin/env node
/**
 * Generates TypeScript types from the backend's live OpenAPI spec.
 *
 * Starts the `database` and `backend` docker compose services, waits for
 * the OpenAPI spec endpoint to become available, generates
 * `src/client/generated/schema.d.ts` from it via `openapi-typescript`'s
 * programmatic API, then stops the containers again.
 *
 * Usage: npm run generate-client
 */

import { spawnSync } from "node:child_process";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import openapiTS, { astToString } from "openapi-typescript";

const REPO_ROOT = path.resolve(import.meta.dirname, "..", "..");
const SPEC_URL = "http://localhost:8081/api/v3/api-docs";
const OUTPUT_DIR = path.resolve(import.meta.dirname, "..", "src", "client", "generated");
const OUTPUT_FILE = path.join(OUTPUT_DIR, "schema.d.ts");

const POLL_INTERVAL_MS = 2000;
const POLL_TIMEOUT_MS = 90_000;

function log(message) {
  console.log(`[generate-client] ${message}`);
}

function startContainers() {
  log("Starting docker compose services (database, backend)...");
  const result = spawnSync("docker", ["compose", "up", "-d", "database", "backend"], {
    cwd: REPO_ROOT,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    throw new Error(`docker compose up failed with exit code ${result.status}`);
  }
}

function stopContainers() {
  log("Stopping docker compose services (database, backend)...");
  const result = spawnSync("docker", ["compose", "stop", "database", "backend"], {
    cwd: REPO_ROOT,
    stdio: "inherit",
  });
  if (result.status !== 0) {
    log(`Warning: docker compose stop exited with code ${result.status}`);
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function waitForSpec() {
  log(`Waiting for backend OpenAPI spec at ${SPEC_URL} (timeout ${POLL_TIMEOUT_MS / 1000}s)...`);
  const start = Date.now();
  let lastError = null;

  while (Date.now() - start < POLL_TIMEOUT_MS) {
    const elapsedSeconds = Math.round((Date.now() - start) / 1000);
    try {
      const response = await fetch(SPEC_URL);
      if (response.status === 200) {
        log(`Backend ready after ${elapsedSeconds}s.`);
        return;
      }
      lastError = `HTTP status ${response.status}`;
      log(`Waiting for backend... (${elapsedSeconds}s elapsed, last status: ${response.status})`);
    } catch (err) {
      lastError = err instanceof Error ? err.message : String(err);
      log(`Waiting for backend... (${elapsedSeconds}s elapsed, not reachable yet)`);
    }
    await sleep(POLL_INTERVAL_MS);
  }

  throw new Error(`Timed out waiting for ${SPEC_URL} to become ready. Last error: ${lastError}`);
}

async function generateTypes() {
  log(`Generating TypeScript types from ${SPEC_URL}...`);
  const ast = await openapiTS(new URL(SPEC_URL));
  const output = astToString(ast);

  mkdirSync(OUTPUT_DIR, { recursive: true });
  writeFileSync(OUTPUT_FILE, output, "utf-8");

  const lineCount = output.split("\n").length;
  const byteSize = Buffer.byteLength(output, "utf-8");
  log(`Wrote ${OUTPUT_FILE} (${byteSize} bytes, ${lineCount} lines).`);
}

async function main() {
  startContainers();
  try {
    await waitForSpec();
    await generateTypes();
  } finally {
    stopContainers();
  }
}

main()
  .then(() => {
    log("Done.");
    process.exit(0);
  })
  .catch((err) => {
    console.error(`[generate-client] Failed: ${err instanceof Error ? err.message : String(err)}`);
    process.exit(1);
  });
