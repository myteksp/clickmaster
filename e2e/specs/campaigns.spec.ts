import { test, expect } from '@playwright/test';

test.describe('Campaigns — complete coverage', () => {
  let email: string;

  test.beforeEach(async ({ page }) => {
    email = `camp-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@clicker.io`;
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@clickmaster.io');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');;

    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Test Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
  });

  test('empty campaigns list shows empty state', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await expect(page.locator('text=No campaigns yet')).toBeVisible();
  });

  test('create campaign: HTTP_ONLY + CONSTANT', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'HTTP Constant');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.locator('text=HTTP Constant')).toBeVisible();
  });

  test('create campaign: BROWSER_NAVIGATION level', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'Browser Nav');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[name="simulationLevel"][value="BROWSER_NAVIGATION"]').check();
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.locator('text=Browser Nav')).toBeVisible();
  });

  test('create campaign: FULL_BROWSER level', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'Full Browser');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[name="simulationLevel"][value="FULL_BROWSER"]').check();
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
  });

  test('create campaign with preset template', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.click('button:has-text("Quick Start")');
    await page.fill('input[placeholder="Campaign name..."]', 'Preset Camp');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
  });

  test('create campaign: BROWSER_NAVIGATION shows scenarios section', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'Scenario Camp');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="BROWSER_NAVIGATION"]').check();

    await expect(page.getByRole('heading', { name: 'Scenarios' })).toBeVisible();
  });

  test('create campaign: HTTP_ONLY does NOT show scenarios section', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'No Scenarios');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();

    await expect(page.getByRole('heading', { name: 'Scenarios' })).not.toBeVisible();
  });

  test('advanced settings collapsible', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');

    await page.fill('input[placeholder="Campaign name..."]', 'Advanced Test');
    await page.locator('select').first().selectOption({ index: 1 });

    await expect(page.getByText('Advanced Settings')).toBeVisible();

    await page.click('summary:has-text("Advanced Settings")');
    await expect(page.getByText('Geographic Distribution')).toBeVisible();
  });

  test('campaign detail page shows correct info', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Detail Check');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.locator('text=Detail Check')).toBeVisible();
    await expect(page.locator('text=HTTP ONLY')).toBeVisible();
    await expect(page.locator('button:has-text("Start")')).toBeVisible();
    await expect(page.getByRole('link', { name: 'Edit' })).toBeVisible();
  });

  test('campaign run history shown on detail page', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Run History');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.getByRole('heading', { name: 'Run History', level: 3 })).toBeVisible();
    await expect(page.getByText('No runs yet')).toBeVisible();
  });

  test('delete campaign from list with confirm dialog', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Delete Me');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await page.click('a:has-text("Campaigns")');
    await page.click('button:has-text("Delete")');
    await page.locator('.fixed button:has-text("Delete")').click();

    await expect(page.getByRole('link', { name: 'Delete Me' })).not.toBeVisible();
  });

  test('edit campaign from detail page', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Edit Me');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await page.getByRole('link', { name: 'Edit' }).click();
    await expect(page).toHaveURL(/\/edit$/);
    await expect(page.locator('input[value="Edit Me"]')).toHaveValue('Edit Me');

    await page.locator('input[value="Edit Me"]').fill('Edited Name');
    await page.click('button:has-text("Update Campaign")');
    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.getByText('Edited Name')).toBeVisible();
  });

  test('campaign search filters results', async ({ page }) => {
    for (const name of ['Alpha', 'Beta', 'Gamma']) {
      await page.click('a:has-text("Campaigns")');
      await page.click('a:has-text("New Campaign")');
      await page.fill('input[placeholder="Campaign name..."]', name);
      await page.locator('select').first().selectOption({ index: 1 });
      await page.click('button:has-text("Create Campaign")');
      await page.click('a:has-text("Campaigns")');
    }

    await page.fill('input[placeholder="Search by name or site..."]', 'Beta');
    await expect(page.getByText('Beta')).toBeVisible();
    await expect(page.getByText('Alpha')).not.toBeVisible();
    await expect(page.getByText('Gamma')).not.toBeVisible();
  });

  test('dashboard shows stats', async ({ page }) => {
    await page.click('a:has-text("Dashboard")');
    await expect(page.locator('text=Running Now')).toBeVisible();
    await expect(page.locator('text=Total Campaigns')).toBeVisible();
    await expect(page.locator('text=Recent Campaigns')).toBeVisible();
  });

  test('create multiple campaigns, all appear in list', async ({ page }) => {
    for (const name of ['Camp A', 'Camp B', 'Camp C']) {
      await page.click('a:has-text("Campaigns")');
      await page.click('a:has-text("New Campaign")');
      await page.fill('input[placeholder="Campaign name..."]', name);
      await page.locator('select').first().selectOption({ index: 1 });
      await page.locator('input[value="HTTP_ONLY"]').check();
      await page.click('button:has-text("Create Campaign")');
      await page.click('a:has-text("Campaigns")');
    }

    for (const name of ['Camp A', 'Camp B', 'Camp C']) {
      await expect(page.getByText(name)).toBeVisible();
    }
  });
});
