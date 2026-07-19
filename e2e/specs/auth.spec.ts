import { test, expect } from '@playwright/test';

const ADMIN_EMAIL = 'admin@clickmaster.io';
const ADMIN_PASSWORD = 'admin123';

test.describe('Auth', () => {
  test('login: success with admin credentials', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', ADMIN_EMAIL);
    await page.fill('input[type="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
    await expect(page.locator('nav')).toContainText('Admin');
  });

  test('login: reject wrong password', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', ADMIN_EMAIL);
    await page.fill('input[type="password"]', 'wrongpassword999');
    await page.click('button[type="submit"]');
    await expect(page.locator('.bg-red-600')).toBeVisible({ timeout: 5000 });
  });

  test('login: reject unknown email', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', 'nobody@nowhere.com');
    await page.fill('input[type="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await expect(page.locator('.bg-red-600')).toBeVisible({ timeout: 5000 });
  });

  test('redirect to login when unauthenticated', async ({ page }) => {
    await page.goto('/campaigns');
    await expect(page).toHaveURL('/login');
    await page.goto('/sites');
    await expect(page).toHaveURL('/login');
    await page.goto('/users');
    await expect(page).toHaveURL('/login');
  });

  test('no register page exists', async ({ page }) => {
    await page.goto('/register');
    await expect(page).toHaveURL('/login');
  });

  test('logout clears session', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', ADMIN_EMAIL);
    await page.fill('input[type="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');

    await page.click('text=Logout');
    await expect(page).toHaveURL('/login');

    await page.goto('/sites');
    await expect(page).toHaveURL('/login');
  });

  test('show/hide password toggle works', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="password"]', 'secret123');
    await page.click('button:has-text("Show")');
    const passInput = page.locator('input').nth(1);
    await expect(passInput).toHaveAttribute('type', 'text');
    await page.click('button:has-text("Hide")');
    await expect(passInput).toHaveAttribute('type', 'password');
  });

  test('user management: create and delete user', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', ADMIN_EMAIL);
    await page.fill('input[type="password"]', ADMIN_PASSWORD);
    await page.click('button[type="submit"]');
    await page.waitForURL('/');

    await page.click('a:has-text("Users")');
    const testEmail = `testuser-${Date.now()}@clickmaster.io`;
    
    await page.click('button:has-text("Add User")');
    await page.fill('input[type="text"]', 'Test User');
    await page.fill('input[type="email"]', testEmail);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button:has-text("Create User")');
    
    await page.waitForTimeout(1000);
    await expect(page.getByText('Test User')).toBeVisible();
    await expect(page.getByText(testEmail)).toBeVisible();

    await page.click('button:has-text("Delete")');
    await page.locator('.fixed.inset-0 button:has-text("Delete")').click();
    await page.waitForTimeout(500);
    await expect(page.getByText(testEmail)).not.toBeVisible();
  });
});
