import { test, expect } from '@playwright/test';

const BASE = 'http://169.58.43.127';
const EMAIL = 'admin@clickmaster.io';
const PASS = 'admin123';

test('production: full sanity check', async ({ page }) => {
  test.setTimeout(120000);

  // LOGIN
  await page.goto(`${BASE}/login`);
  await page.fill('input[type="email"]', EMAIL);
  await page.fill('input[type="password"]', PASS);
  await page.click('button[type="submit"]');
  await expect(page).toHaveURL(`${BASE}/`);
  console.log('✓ Login');

  // SITES
  await page.click('a:has-text("Sites")');
  await expect(page.locator('h1')).toContainText('Sites');
  console.log('✓ Sites page');

  // Create site
  await page.click('button:has-text("Add Site")');
  await page.fill('input[placeholder="My Website"]', 'Sanity Test Site');
  await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
  await page.click('button:has-text("Create")');
  await page.waitForTimeout(1000);
  await expect(page.getByText('Sanity Test Site')).toBeVisible();
  console.log('✓ Site created');

  // CAMPAIGNS
  await page.click('a:has-text("Campaigns")');
  await expect(page.locator('h1')).toContainText('Campaigns');
  console.log('✓ Campaigns page');

  // Create campaign
  await page.click('a:has-text("New Campaign")');
  await page.fill('input[placeholder="Campaign name..."]', 'Sanity Campaign');
  await page.locator('select').first().selectOption({ index: 1 });
  await page.locator('input[value="HTTP_ONLY"]').check();
  await page.click('button:has-text("Create Campaign")');
  await page.waitForURL(/\/campaigns\//, { timeout: 10000 });
  console.log('✓ Campaign created');

  // Detail page
  await expect(page.locator('h1')).toContainText('Sanity Campaign');
  await expect(page.locator('button:has-text("Start")')).toBeVisible();
  console.log('✓ Campaign detail');

  // Edit campaign
  await page.getByRole('link', { name: 'Edit' }).click();
  await page.waitForURL(/\/edit$/);
  await expect(page.locator('input[placeholder="Campaign name..."]')).toHaveValue('Sanity Campaign');
  console.log('✓ Campaign edit');

  // Update campaign
  await page.locator('input[placeholder="Campaign name..."]').fill('Sanity Campaign Updated');
  await page.click('button:has-text("Update Campaign")');
  await page.waitForURL(/\/campaigns\//, { timeout: 10000 });
  await expect(page.locator('h1')).toContainText('Sanity Campaign Updated');
  console.log('✓ Campaign updated');

  // Delete campaign
  await page.click('button:has-text("Delete")');
  await page.locator('.fixed.inset-0 button:has-text("Delete")').click();
  await page.waitForURL(`${BASE}/campaigns`);
  console.log('✓ Campaign deleted');

  // DASHBOARD
  await page.click('a:has-text("Dashboard")');
  await expect(page.locator('h1')).toContainText('Dashboard');
  console.log('✓ Dashboard');

  // USERS
  await page.click('a:has-text("Users")');
  await expect(page.locator('h1')).toContainText('Users');
  await expect(page.getByText('admin@clickmaster.io')).toBeVisible();
  console.log('✓ Users page');

  // SCENARIOS
  await page.click('a:has-text("Scenarios")');
  await expect(page.locator('h1')).toContainText('Scenarios');
  console.log('✓ Scenarios page');

  // LOGOUT
  await page.click('text=Logout');
  await expect(page).toHaveURL(`${BASE}/login`);
  console.log('✓ Logout');

  // Register page redirects to login
  await page.goto(`${BASE}/register`);
  await expect(page).toHaveURL(`${BASE}/login`);
  console.log('✓ Register blocked');

  console.log('\n=== ALL PRODUCTION CHECKS PASSED ===');
});
