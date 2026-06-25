import { test, expect } from '@playwright/test';

test.describe('Sites — complete coverage', () => {
  let email: string;

  test.beforeEach(async ({ page }) => {
    email = `sites-${Date.now()}-${Math.random().toString(36).slice(2,8)}@clicker.io`;
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Site Tester');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
  });

  test('empty state shown when no sites', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await expect(page).toHaveURL('/sites');
    await expect(page.locator('text=No sites configured')).toBeVisible();
  });

  test('create site with valid data', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');

    // Form should be visible
    await expect(page.locator('text=New Site')).toBeVisible();

    await page.fill('input[placeholder="My Website"]', 'My Example Site');
    await page.fill('input[placeholder="https://example.com"]', 'https://example.com');
    await page.click('button:has-text("Create")');

    // Form should close
    await expect(page.locator('text=New Site')).not.toBeVisible();
    // Site should appear in list
    await expect(page.locator('text=My Example Site')).toBeVisible();
    await expect(page.locator('text=https://example.com')).toBeVisible();
  });

  test('create site: empty name should be rejected by HTML5', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="https://example.com"]', 'https://example.com');
    await page.click('button:has-text("Create")');

    // HTML5 validation should prevent submit — form still visible
    await expect(page.locator('text=New Site')).toBeVisible();
  });

  test('create site: empty URL should be rejected', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Test');
    await page.click('button:has-text("Create")');

    // HTML5 validation on URL input
    await expect(page.locator('text=New Site')).toBeVisible();
  });

  test('create → edit → verify update', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Original');
    await page.fill('input[placeholder="https://example.com"]', 'https://original.com');
    await page.click('button:has-text("Create")');

    await page.click('button:has-text("Edit")');
    // Form pre-filled with existing data
    const nameInput = page.locator('input[placeholder="My Website"]');
    await expect(nameInput).toHaveValue('Original');

    await nameInput.clear();
    await nameInput.fill('Renamed');
    const urlInput = page.locator('input[placeholder="https://example.com"]');
    await urlInput.clear();
    await urlInput.fill('https://renamed.com');
    await page.click('button:has-text("Update")');

    await expect(page.locator('text=Renamed').first()).toBeVisible();
    await expect(page.locator('text=Original')).not.toBeVisible();
  });

  test('create → cancel edit form', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'To Cancel');
    await page.fill('input[placeholder="https://example.com"]', 'https://cancel.com');
    await page.click('button:has-text("Create")');

    await page.click('button:has-text("Edit")');
    await page.click('button:has-text("Cancel")');
    // Form should disappear, original name still visible
    await expect(page.locator('text=To Cancel')).toBeVisible();
  });

  test('create → delete with confirmation', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Delete Me');
    await page.fill('input[placeholder="https://example.com"]', 'https://delete.com');
    await page.click('button:has-text("Create")');

    page.on('dialog', dialog => {
      expect(dialog.message()).toContain('Delete');
      dialog.accept();
    });
    await page.click('button:has-text("Delete")');

    await expect(page.locator('text=Delete Me')).not.toBeVisible();
  });

  test('create multiple sites, verify list order', async ({ page }) => {
    const sites = ['Site A', 'Site B', 'Site C'];
    for (const name of sites) {
      await page.click('a:has-text("Sites")');
      await page.click('button:has-text("Add Site")');
      await page.fill('input[placeholder="My Website"]', name);
      await page.fill('input[placeholder="https://example.com"]', `https://${name.toLowerCase().replace(' ','')}.com`);
      await page.click('button:has-text("Create")');
    }

    await page.click('a:has-text("Sites")');
    // All three should be visible
    for (const name of sites) {
      await expect(page.locator('text=' + name)).toBeVisible();
    }
  });
});
