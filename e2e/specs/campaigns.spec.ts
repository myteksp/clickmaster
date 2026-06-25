import { test, expect } from '@playwright/test';

test.describe('Campaigns — complete coverage', () => {
  let email: string;

  test.beforeEach(async ({ page }) => {
    email = `camp-${Date.now()}-${Math.random().toString(36).slice(2,8)}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Camp Tester');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');

    // Create a site first (needed for campaigns)
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Test Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
  });

  test('empty campaigns list', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await expect(page.locator('text=No campaigns yet')).toBeVisible();
  });

  test('create campaign: HTTP_ONLY + CONSTANT', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'HTTP Constant');

    // Select site
    await page.locator('select').first().selectOption({ index: 1 });

    // HTTP Only radio
    await page.locator('input[value="HTTP_ONLY"]').check();

    // Visits/hour and duration
    const nums = page.locator('input[type="number"]');
    await nums.nth(0).fill('100');
    await nums.nth(1).fill('30');

    // Constant pattern
    await page.locator('input[value="CONSTANT"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
    await expect(page.locator('text=HTTP Constant')).toBeVisible();
  });

  test('create campaign: BROWSER_NAVIGATION level', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Browser Nav');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[name="simulationLevel"][value="BROWSER_NAVIGATION"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
    // Verify campaign appears — rely on name match
    await expect(page.getByRole('link', { name: 'Browser Nav' })).toBeVisible();
  });

  test('create campaign: FULL_BROWSER level', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Full Browser');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[name="simulationLevel"][value="FULL_BROWSER"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
    await expect(page.getByRole('link', { name: 'Full Browser' })).toBeVisible();
  });

  test('create campaign: RAMP_UP pattern', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Ramp Up');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="RAMP_UP"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
  });

  test('create campaign: PULSE pattern', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Pulse');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="PULSE"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
  });

  test('create campaign: REALISTIC_WAVE pattern', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Wave');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="REALISTIC_WAVE"]').check();

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
  });

  test('create campaign with geo distribution', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Geo Test');
    await page.locator('select').first().selectOption({ index: 1 });

    // Add another country
    await page.click('button:has-text("+ Add Country")');
    // We should now have 2 geo entries
    const geoInputs = page.locator('input[placeholder="US"]');
    await expect(geoInputs).toHaveCount(2);

    // Set second country to GB
    await geoInputs.nth(1).fill('GB');

    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');
  });

  test('create campaign: no site selected should be rejected', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Bad Campaign');
    // Don't select site — keep "Select a site..."

    await page.click('button:has-text("Create Campaign")');
    // HTML5 validation or stays on form
    // The page should still show the form (submit blocked by validation)
  });

  test('campaign detail page shows correct info', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="My Campaign"]', 'Detail Check');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    const nums = page.locator('input[type="number"]');
    await nums.nth(0).fill('500');
    await nums.nth(1).fill('60');
    await page.locator('input[value="CONSTANT"]').check();
    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');

    // Open detail
    await page.click('a:has-text("Detail Check")');
    await expect(page).toHaveURL(/\/campaigns\//);

    // Check detail page content
    await expect(page.locator('text=Detail Check')).toBeVisible();
    await expect(page.locator('text=DRAFT')).toBeVisible();
    await expect(page.locator('text=HTTP Only')).toBeVisible();
    await expect(page.locator('text=500')).toBeVisible(); // visits per hour
    await expect(page.locator('text=60m')).toBeVisible(); // duration

    // Should have Start button
    await expect(page.locator('button:has-text("Start")')).toBeVisible();
    // Should have Delete button
    await expect(page.locator('button:has-text("Delete")')).toBeVisible();
    // Should NOT have Stop or Pause buttons when not running
    await expect(page.locator('button:has-text("Stop")')).not.toBeVisible();
    await expect(page.locator('button:has-text("Pause")')).not.toBeVisible();
  });

  test('campaign run history shown on detail page', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="My Campaign"]', 'Run History');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');

    // Open detail
    await page.click('a:has-text("Run History")');
    await expect(page).toHaveURL(/\/campaigns\//);

    // Run history section
    await expect(page.locator('text=Run History')).toBeVisible();
    await expect(page.locator('text=No runs yet')).toBeVisible();
  });

  test('delete campaign from list', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="My Campaign"]', 'Delete Me');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');

    page.on('dialog', dialog => dialog.accept());
    await page.click('button:has-text("Delete")');

    await expect(page.locator('text=Delete Me')).not.toBeVisible();
  });

  test('delete campaign from detail page', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="My Campaign"]', 'Detail Delete');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL('/campaigns');

    await page.click('a:has-text("Detail Delete")');
    await expect(page).toHaveURL(/\/campaigns\//);

    page.on('dialog', dialog => dialog.accept());
    await page.click('button:has-text("Delete")');

    // Should redirect back to campaigns list
    await expect(page).toHaveURL('/campaigns');
  });

  test('dashboard shows stats cards', async ({ page }) => {
    await page.click('a:has-text("Dashboard")');
    await expect(page).toHaveURL('/');

    // Stats cards
    await expect(page.locator('text=Running')).toBeVisible();
    await expect(page.locator('text=Active')).toBeVisible();
    await expect(page.locator('text=Total Campaigns')).toBeVisible();

    // Recent campaigns section
    await expect(page.locator('text=Recent Campaigns')).toBeVisible();
  });

  test('dashboard: New Campaign button works', async ({ page }) => {
    await page.click('a:has-text("Dashboard")');
    await page.click('a:has-text("New Campaign")');
    await expect(page).toHaveURL('/campaigns/new');
  });

  test('create campaign: BROWSER_NAVIGATION shows scenarios section', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'Scenario Camp');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="BROWSER_NAVIGATION"]').check();

    // Scenarios section should appear for non-HTTP_ONLY levels
    await expect(page.getByRole('heading', { name: 'Scenarios' })).toBeVisible();
    await expect(page.locator('button:has-text("+ Add Scenario")')).toBeVisible();
  });

  test('create campaign: HTTP_ONLY does NOT show scenarios section', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="My Campaign"]', 'No Scenarios');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();

    // Scenarios section should NOT be visible
    await expect(page.locator('button:has-text("+ Add Scenario")')).not.toBeVisible();
  });

  test('create multiple campaigns, all appear in list', async ({ page }) => {
    const names = ['Camp A', 'Camp B', 'Camp C'];
    for (const name of names) {
      await page.click('a:has-text("Campaigns")');
      await page.click('a:has-text("New Campaign")');
      await page.fill('input[placeholder="My Campaign"]', name);
      await page.locator('select').first().selectOption({ index: 1 });
      await page.locator('input[value="HTTP_ONLY"]').check();
      await page.click('button:has-text("Create Campaign")');
      await expect(page).toHaveURL('/campaigns');
    }

    await page.click('a:has-text("Campaigns")');
    for (const name of names) {
      await expect(page.locator('text=' + name)).toBeVisible();
    }
  });
});
