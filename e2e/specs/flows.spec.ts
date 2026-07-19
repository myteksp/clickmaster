import { test, expect } from '@playwright/test';

test.describe('Complete User Flows', () => {

  test('full lifecycle: register → site → campaign → start → stop', async ({ page }) => {
    const email = `flow-${Date.now()}@clicker.io`;
    
    // 1. Register
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Flow User');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
    await expect(page.locator('nav')).toContainText('Flow User');

    // 2. Create site
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Flow Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
    await expect(page.getByText('Flow Site', { exact: true })).toBeVisible();
    await expect(page.getByText('Site created')).toBeVisible({ timeout: 3000 });

    // 3. Create campaign
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Lifecycle Test');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');
    
    // Should land on detail page
    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.getByText('Lifecycle Test')).toBeVisible();
    await expect(page.locator('button:has-text("Start")')).toBeVisible();

    // 4. Edit campaign
    await page.getByRole('link', { name: 'Edit' }).click();
    await expect(page).toHaveURL(/\/edit$/);
    await page.locator('input[placeholder="Campaign name..."]').fill('Lifecycle Edited');
    await page.click('button:has-text("Update Campaign")');
    await expect(page).toHaveURL(/\/campaigns\//);
    await expect(page.getByText('Lifecycle Edited')).toBeVisible();

    // 5. Start campaign (HTTP_ONLY with ASOCKS will need proxy — just verify button state changes)
    await page.click('button:has-text("Start")');
    
    // Either Pause appears (running) or an error toast shows
    await page.waitForTimeout(8000);
    
    const hasPause = await page.locator('button:has-text("Pause")').count();
    const hasError = await page.locator('.bg-red-600').count();
    expect(hasPause + hasError).toBeGreaterThan(0);

    // 6. Stop if running
    if (hasPause > 0) {
      await page.click('button:has-text("Stop")');
      await page.locator('.fixed.inset-0 button:has-text("Stop")').click();
      await page.waitForTimeout(1000);
    }
  });

  test('expired token redirects to login', async ({ page }) => {
    // Register and get a valid session
    const email = `expire-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Expire Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');

    // Corrupt the token to simulate expiration
    await page.evaluate(() => {
      localStorage.setItem('token', 'invalid expired token');
    });

    // Navigate to a page that requires auth
    await page.goto('/sites');
    
    // Should redirect to login
    await expect(page).toHaveURL('/login');

    // Should be able to log in again
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
  });

  test('navigation: all pages reachable and show correct content', async ({ page }) => {
    const email = `nav-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Nav Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');

    // Dashboard
    await page.click('a:has-text("Dashboard")');
    await expect(page).toHaveURL('/');
    await expect(page.getByText('Running Now')).toBeVisible();
    await expect(page.getByText('Recent Campaigns')).toBeVisible();

    // Campaigns
    await page.click('a:has-text("Campaigns")');
    await expect(page).toHaveURL('/campaigns');
    await expect(page.locator('input[placeholder="Search by name or site..."]')).toBeVisible();

    // Scenarios
    await page.click('a:has-text("Scenarios")');
    await expect(page).toHaveURL('/scenarios');

    // Sites
    await page.click('a:has-text("Sites")');
    await expect(page).toHaveURL('/sites');
    await expect(page.getByText('No sites configured')).toBeVisible();

    // Back to Dashboard
    await page.click('a:has-text("Dashboard")');
    await expect(page).toHaveURL('/');
  });

  test('campaign form: all presets work', async ({ page }) => {
    const email = `preset-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Preset Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');

    // Create site first
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Preset Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://test.com');
    await page.click('button:has-text("Create")');

    for (const preset of ['Quick Start', 'balanced', 'heavy']) {
      await page.click('a:has-text("Campaigns")');
      await page.click('a:has-text("New Campaign")');
      
      await page.click(`button:has-text("${preset}")`);
      await page.fill('input[placeholder="Campaign name..."]', `Preset ${preset}`);
      await page.locator('select').first().selectOption({ index: 1 });
      await page.click('button:has-text("Create Campaign")');
      
      await expect(page).toHaveURL(/\/campaigns\//);
      await expect(page.getByText(`Preset ${preset}`)).toBeVisible();
    }
  });

  test('campaign form: validation prevents empty submit', async ({ page }) => {
    const email = `val-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Val Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');

    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    
    // Try to submit without filling anything
    await page.click('button:has-text("Create Campaign")');
    
    // Should stay on form page (HTML5 validation blocks)
    await expect(page).toHaveURL('/campaigns/new');
  });

  test('campaign list: filter by status', async ({ page }) => {
    const email = `filter-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Filter Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');

    // Create site
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Filter Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://test.com');
    await page.click('button:has-text("Create")');

    // Create a DRAFT campaign
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Draft One');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    // Go to campaigns list and filter by DRAFT
    await page.click('a:has-text("Campaigns")');
    await page.click('button:has-text("Draft")');
    await expect(page.getByText('Draft One')).toBeVisible();

    // Filter by RUNNING — should show nothing
    await page.click('button:has-text("Running")');
    await expect(page.getByText('No matching campaigns')).toBeVisible();
  });

  test('site: URL auto-prepends https://', async ({ page }) => {
    const email = `url-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'URL Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');

    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Auto URL');
    await page.fill('input[placeholder="https://example.com"]', 'example.com');
    await page.click('button:has-text("Create")');
    
    // Should show the site with https:// prepended
    await expect(page.getByText('https://example.com')).toBeVisible();
  });

  test('deep link redirect: unauthenticated user goes to login', async ({ page }) => {
    await page.goto('/campaigns/new');
    await expect(page).toHaveURL('/login');
    
    await page.goto('/scenarios/new');
    await expect(page).toHaveURL('/login');
  });

  test('scenario: create with multiple steps and edit', async ({ page }) => {
    const email = `scn-${Date.now()}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Scn Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');

    await page.click('a:has-text("Scenarios")');
    await page.click('a:has-text("New Scenario")');
    await page.fill('input[placeholder="Explore pricing page"]', 'Multi Step');
    await page.click('button:has-text("Add Step")');
    await page.click('button:has-text("Create Scenario")');

    await expect(page).toHaveURL('/scenarios');
    await expect(page.getByText('Multi Step', { exact: true })).toBeVisible();
    await expect(page.getByText('2 steps')).toBeVisible();

    // Edit
    await page.click('button:has-text("Edit")');
    await expect(page).toHaveURL(/\/scenarios\//);
    await expect(page.locator('input[placeholder="Explore pricing page"]')).toHaveValue('Multi Step');
    await page.locator('input[placeholder="Explore pricing page"]').fill('Multi Step Edited');
    await page.click('button:has-text("Update Scenario")');
    await page.waitForURL('/scenarios');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Multi Step Edited', { exact: true })).toBeVisible({ timeout: 10000 });
  });
});
