import { test, expect } from '@playwright/test';

test.describe('Campaign Lifecycle — every button tested', () => {
  let email: string;

  test.beforeEach(async ({ page }) => {
    email = `life-${Date.now()}-${Math.random().toString(36).slice(2, 8)}@clicker.io`;
    await page.goto('/login');
    await page.fill('input[type="email"]', 'admin@clickmaster.io');
    await page.fill('input[type="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');;

    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Life Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
  });

  const dialogConfirm = async (page: import('@playwright/test').Page, buttonText: string) => {
    const dialog = page.locator('.fixed.inset-0');
    await expect(dialog).toBeVisible({ timeout: 5000 });
    await dialog.locator(`button:has-text("${buttonText}")`).click();
  };

  test('start → pause → resume → stop', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Pause Flow');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');
    await expect(page).toHaveURL(/\/campaigns\//);

    // Start — proxy creation may take up to 10s
    await page.click('button:has-text("Start")');
    await expect(page.locator('button:has-text("Pause")')).toBeVisible({ timeout: 15000 });

    // Pause
    await page.click('button:has-text("Pause")');
    await expect(page.locator('button:has-text("Resume")')).toBeVisible({ timeout: 10000 });

    // Resume
    await page.click('button:has-text("Resume")');
    await expect(page.locator('button:has-text("Pause")')).toBeVisible({ timeout: 10000 });

    // Stop via confirm dialog
    await page.click('button:has-text("Stop")');
    await dialogConfirm(page, 'Stop');
    
    // After stop — Start button appears again
    await expect(page.locator('button:has-text("Start")')).toBeVisible({ timeout: 10000 });
  });

  test('stop shows confirm dialog, cancel keeps running', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Stop Cancel');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');

    await page.click('button:has-text("Start")');
    await expect(page.locator('button:has-text("Stop")')).toBeVisible({ timeout: 15000 });

    // Click stop, then cancel
    await page.click('button:has-text("Stop")');
    await dialogConfirm(page, 'Cancel');

    // Campaign still running
    await expect(page.locator('button:has-text("Stop")')).toBeVisible();

    // Clean up
    await page.click('button:has-text("Stop")');
    await dialogConfirm(page, 'Stop');
  });

  test('delete from detail page (draft campaign)', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Delete Detail');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await page.click('button:has-text("Delete")');
    await dialogConfirm(page, 'Delete');
    await expect(page).toHaveURL('/campaigns');
    await expect(page.getByRole('link', { name: 'Delete Detail' })).not.toBeVisible();
  });

  test('delete completed campaign from list', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Delete Completed');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');

    // Start and stop to make it COMPLETED
    await page.click('button:has-text("Start")');
    await expect(page.locator('button:has-text("Stop")')).toBeVisible({ timeout: 15000 });
    await page.click('button:has-text("Stop")');
    await dialogConfirm(page, 'Stop');
    await expect(page.locator('button:has-text("Start")')).toBeVisible({ timeout: 10000 });

    // Go to list, delete
    await page.click('a:has-text("Campaigns")');
    await page.click('button:has-text("Delete")');
    await dialogConfirm(page, 'Delete');
    await expect(page.getByRole('link', { name: 'Delete Completed' })).not.toBeVisible();
  });

  test('edit button navigates to edit form', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Edit Nav');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.click('button:has-text("Create Campaign")');

    await page.getByRole('link', { name: 'Edit' }).click();
    await expect(page).toHaveURL(/\/edit$/);
    await expect(page.locator('input[placeholder="Campaign name..."]')).toHaveValue('Edit Nav');
  });

  test('cancel button on form returns without saving', async ({ page }) => {
    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Cancel Me');
    await page.locator('select').first().selectOption({ index: 1 });

    await page.click('button:has-text("Cancel")');
    await expect(page).toHaveURL('/campaigns');
    await expect(page.getByText('Cancel Me')).not.toBeVisible();
  });

  test('campaign list: search and status filter combined', async ({ page }) => {
    // Create two campaigns
    for (const name of ['Searchable Alpha', 'Searchable Beta']) {
      await page.click('a:has-text("Campaigns")');
      await page.click('a:has-text("New Campaign")');
      await page.fill('input[placeholder="Campaign name..."]', name);
      await page.locator('select').first().selectOption({ index: 1 });
      await page.click('button:has-text("Create Campaign")');
    }

    await page.click('a:has-text("Campaigns")');

    // Search for Alpha
    await page.fill('input[placeholder="Search by name or site..."]', 'Alpha');
    await expect(page.getByRole('link', { name: 'Searchable Alpha' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Searchable Beta' })).not.toBeVisible();

    // Clear search
    await page.fill('input[placeholder="Search by name or site..."]', '');
    await expect(page.getByRole('link', { name: 'Searchable Alpha' })).toBeVisible();

    // Filter by Draft status
    await page.click('button:has-text("Draft")');
    await expect(page.getByRole('link', { name: 'Searchable Alpha' })).toBeVisible();
  });

  test('site edit and delete from sites page', async ({ page }) => {
    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Edit Delete Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://edit.com');
    await page.click('button:has-text("Create")');
    await expect(page.getByText('Edit Delete Site')).toBeVisible();

    // Edit
    await page.click('button:has-text("Edit")');
    await expect(page.locator('input[placeholder="My Website"]')).toHaveValue('Edit Delete Site');
    await page.locator('input[placeholder="My Website"]').fill('Renamed Site');
    await page.click('button:has-text("Update")');
    await expect(page.getByText('Renamed Site')).toBeVisible();
    await expect(page.getByText('Edit Delete Site')).not.toBeVisible();

    // Delete
    await page.click('button:has-text("Delete")');
    await dialogConfirm(page, 'Delete');
    await expect(page.getByText('Renamed Site', { exact: true })).not.toBeVisible();
  });

  test('scenario create, edit, delete from scenarios page', async ({ page }) => {
    await page.click('a:has-text("Scenarios")');
    await page.click('a:has-text("New Scenario")');
    await page.fill('input[placeholder="Explore pricing page"]', 'Lifecycle Scenario');
    await page.click('button:has-text("Create Scenario")');
    await expect(page).toHaveURL('/scenarios');
    await expect(page.getByText('Lifecycle Scenario', { exact: true })).toBeVisible();

    // Edit
    await page.click('button:has-text("Edit")');
    await expect(page.locator('input[placeholder="Explore pricing page"]')).toHaveValue('Lifecycle Scenario');
    await page.locator('input[placeholder="Explore pricing page"]').fill('Edited Scenario');
    await page.click('button:has-text("Update Scenario")');
    await page.waitForURL('/scenarios');
    await page.waitForLoadState('networkidle');
    await expect(page.getByText('Edited Scenario', { exact: true })).toBeVisible({ timeout: 10000 });

    // Delete
    await page.click('button:has-text("Delete")');
    await dialogConfirm(page, 'Delete');
    await expect(page.getByText('Edited Scenario', { exact: true })).not.toBeVisible();
  });
});
