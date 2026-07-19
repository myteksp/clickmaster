import { test, expect } from '@playwright/test';

const email = `auth-${Date.now()}@clicker.io`;
const password = 'testpass123';
const name = 'Auth Test';

test.describe('Auth — complete coverage', () => {
  test('register: success then redirect to dashboard', async ({ page }) => {
    await page.goto('/register');
    await page.fill('input[type="text"]', name);
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
    await expect(page.locator('nav')).toContainText(name);
  });

  test('register: required fields have HTML5 validation', async ({ page }) => {
    await page.goto('/register');
    const nameInput = page.locator('input[type="text"]');
    await expect(nameInput).toHaveAttribute('required', '');
    const emailInput = page.locator('input[type="email"]');
    await expect(emailInput).toHaveAttribute('required', '');
    const passInput = page.locator('input[type="password"]');
    await expect(passInput).toHaveAttribute('required', '');
    await expect(passInput).toHaveAttribute('minlength', '6');
  });

  test('register: reject duplicate email with toast error', async ({ page }) => {
    await page.goto('/register');
    await page.fill('input[type="text"]', 'Test');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'testpass123');
    await page.click('button[type="submit"]');
    await expect(page.locator('.bg-red-600')).toBeVisible({ timeout: 5000 });
  });

  test('login: success with valid credentials', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');
    await expect(page.locator('nav')).toContainText(name);
  });

  test('login: reject wrong password with toast error', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', 'wrongpassword999');
    await page.click('button[type="submit"]');
    await expect(page.locator('.bg-red-600')).toBeVisible({ timeout: 5000 });
  });

  test('login: reject unknown email with toast error', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', `nobody-${Date.now()}@nowhere.com`);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await expect(page.locator('.bg-red-600')).toBeVisible({ timeout: 5000 });
  });

  test('redirect to login when unauthenticated', async ({ page }) => {
    await page.goto('/campaigns');
    await expect(page).toHaveURL('/login');
    await page.goto('/scenarios');
    await expect(page).toHaveURL('/login');
    await page.goto('/sites');
    await expect(page).toHaveURL('/login');
    await page.goto('/campaigns/new');
    await expect(page).toHaveURL('/login');
  });

  test('logout clears session and redirects', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await expect(page).toHaveURL('/');

    await page.click('text=Logout');
    await expect(page).toHaveURL('/login');

    await page.goto('/sites');
    await expect(page).toHaveURL('/login');
  });

  test('nav shows correct user name after login', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="email"]', email);
    await page.fill('input[type="password"]', password);
    await page.click('button[type="submit"]');
    await expect(page.locator('nav')).toContainText(name);
  });

  test('login page → register link works', async ({ page }) => {
    await page.goto('/login');
    await page.click('text=Register');
    await expect(page).toHaveURL('/register');
  });

  test('register page → login link works', async ({ page }) => {
    await page.goto('/register');
    await page.click('text=Sign in');
    await expect(page).toHaveURL('/login');
  });

  test('show/hide password toggle works on login', async ({ page }) => {
    await page.goto('/login');
    await page.fill('input[type="password"]', 'secret123');
    await page.click('button:has-text("Show")');
    const passInput = page.locator('input').nth(1);
    await expect(passInput).toHaveAttribute('type', 'text');
    await page.click('button:has-text("Hide")');
    await expect(passInput).toHaveAttribute('type', 'password');
  });
});
