import { defineConfig, devices } from '@playwright/test';

/**
 * E2E tests run against the full Docker Compose stack (Nginx on port 8080).
 * Start the stack before running tests:
 *
 *   docker compose up --build -d
 *   cd e2e && npm test
 *   docker compose down
 *
 * Tests run sequentially (workers: 1) because they share a single database instance.
 * Parallel execution would cause test data collisions.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  retries: 0,
  workers: 1,
  reporter: [['html', { open: 'never' }], ['list']],

  use: {
    baseURL: 'http://localhost:8080',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});