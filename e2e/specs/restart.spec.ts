import { test, expect } from '@playwright/test';
import { execSync } from 'child_process';

test.describe('Backend Restart Recovery', () => {
  test('campaign started before restart is reconciled on boot', async ({ browser }) => {
    const page = await browser.newPage();

    const email = `rst-${Date.now()}@clicker.io`;
    await page.goto('http://localhost:3000/register');
    await page.fill('input[type="text"]', 'RST User');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await page.waitForURL('http://localhost:3000/');

    await page.click('a:has-text("Sites")');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'RST Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
    await page.waitForTimeout(1000);

    await page.click('a:has-text("Campaigns")');
    await page.click('a:has-text("New Campaign")');
    await page.fill('input[placeholder="Campaign name..."]', 'Pre-Restart Campaign');
    await page.locator('select').first().selectOption({ index: 1 });
    await page.locator('input[value="HTTP_ONLY"]').check();
    await page.click('button:has-text("Create Campaign")');
    await page.waitForURL(/\/campaigns\//);

    await page.click('button:has-text("Start")');
    await expect(page.locator('button:has-text("Pause")')).toBeVisible({ timeout: 15000 });

    await page.close();
  });
});
