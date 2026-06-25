import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './specs',
  timeout: 30000,
  expect: { timeout: 5000 },
  fullyParallel: false,
  retries: 0,
  workers: 1,
  use: {
    baseURL: 'http://localhost:3000',
    headless: true,
    viewport: { width: 1280, height: 720 },
    trace: 'on-first-retry',
  },
  webServer: [
    {
      command: 'cd ../backend && gradle bootRun',
      url: 'http://localhost:8080/actuator/health',
      timeout: 120000,
      reuseExistingServer: true,
      cwd: '/Users/dmitry/Documents/Projects/clicker/backend',
    },
    {
      command: 'cd ../frontend && npx vite --port 3000 --strictPort',
      url: 'http://localhost:3000',
      timeout: 30000,
      reuseExistingServer: true,
      cwd: '/Users/dmitry/Documents/Projects/clicker/frontend',
    },
  ],
});
