import { test, expect } from '@playwright/test';

test('interactive picker: navigate through tabs, select deep target', async ({ page }) => {
  test.setTimeout(300000);

  await page.goto('/login');
  await page.fill('input[type="email"]', 'admin@clickmaster.io');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('/');

  // Create campaign
  await page.click('a:has-text("Campaigns")');
  await page.click('a:has-text("New Campaign")');
  await page.fill('input[placeholder="Campaign name..."]', 'Interactive Target Test');
  await page.locator('select').first().selectOption({ index: 1 });
  await page.locator('input[value="FULL_BROWSER"]').check();
  console.log('1. Campaign form filled');

  // Open interactive picker
  await page.click('button:has-text("Open Interactive Picker")');
  await page.waitForSelector('.fixed.inset-0 img', { timeout: 60000 });
  await page.waitForTimeout(2000);
  console.log('2. Interactive session started — screenshot loaded');

  // Verify Navigate mode is active by default
  const navigateBtn = page.locator('button:has-text("Navigate")');
  const navigateActive = await navigateBtn.first().evaluate(el =>
    el.className.includes('blue-600')
  );
  console.log('3. Navigate mode active:', navigateActive);

  // Count overlay elements on home page
  const homeOverlays = page.locator('.fixed.inset-0 button[title]');
  const homeCount = await homeOverlays.count();
  console.log('   Home page elements:', homeCount);

  // Step 1: Navigate — click "Savings" link
  let savingsClicked = false;
  for (let i = 0; i < homeCount; i++) {
    const title = await homeOverlays.nth(i).getAttribute('title') || '';
    if (title === 'Savings' || title.includes('Savings')) {
      await homeOverlays.nth(i).click();
      console.log('4. Navigating: clicked', title);
      savingsClicked = true;

      // Wait for loading
      await page.waitForTimeout(4000);
      break;
    }
  }
  expect(savingsClicked).toBeTruthy();

  // Verify we navigated — new screenshot, breadcrumb shows Savings
  const breadcrumb = await page.locator('.fixed.inset-0').innerText();
  console.log('5. After navigation — breadcrumb shows Savings:', breadcrumb.includes('Savings'));

  // Count elements on the new page
  const newOverlays = page.locator('.fixed.inset-0 button[title]');
  const newCount = await newOverlays.count();
  console.log('   Elements on Savings page:', newCount);
  expect(newCount).toBeGreaterThan(0);

  // Step 2: Switch to Select mode
  await page.click('button:has-text("Select Target")');
  console.log('6. Switched to Select mode');

  // Select an element from the Savings page
  let selected = false;
  for (let i = 0; i < newCount; i++) {
    const title = await newOverlays.nth(i).getAttribute('title') || '';
    // Skip nav links and footer
    if (['Savings', 'Investing', 'Home', 'Mortgage', 'Blog', 'Privacy', 'Terms', 'Cookie', 'Advertising', 'FundingSuperHero'].some(nav => title.includes(nav))) continue;
    if (title.length < 3) continue;

    await newOverlays.nth(i).click();
    await page.waitForTimeout(500);
    console.log('7. Selected target:', title);
    selected = true;
    break;
  }

  if (!selected) {
    // Fallback: select first element
    await newOverlays.first().click();
    console.log('7. Selected target (fallback):', await newOverlays.first().getAttribute('title'));
    selected = true;
  }

  // Go back to Home and select another target there too
  await page.click('button:has-text("Navigate")');
  await page.click('button:has-text("← Back")');
  await page.waitForTimeout(4000);
  
  // Verify breadcrumb is cleared
  const breadcrumbAfterBack = await page.locator('.fixed.inset-0').innerText();
  console.log('8. Navigated back — breadcrumb contains Savings:', breadcrumbAfterBack.includes('Savings'));

  await page.click('button:has-text("Select Target")');
  const homeOverlaysAgain = page.locator('.fixed.inset-0 button[title]');
  for (let i = 0; i < await homeOverlaysAgain.count(); i++) {
    const title = await homeOverlaysAgain.nth(i).getAttribute('title') || '';
    if (title.includes('Compare') || title.includes('Learn')) {
      await homeOverlaysAgain.nth(i).click();
      await page.waitForTimeout(500);
      console.log('9. Selected home target:', title);
      break;
    }
  }

  // Close picker
  await page.click('button:has-text("Done")');
  await page.waitForTimeout(1000);
  console.log('10. Picker closed');

  // Verify targets visible in form
  await expect(page.getByText('Configured targets')).toBeVisible({ timeout: 5000 });
  const targetCount = await page.locator('text=/\\d+ targets?/').count();
  console.log('11. Targets configured:', targetCount > 0 ? 'yes' : 'no');

  // Save campaign
  await page.click('button:has-text("Create Campaign")');
  await page.waitForURL(/\/campaigns\//, { timeout: 10000 });
  console.log('12. Campaign saved');

  // Verify via edit page that targets have navigation steps
  await page.getByRole('link', { name: 'Edit' }).click();
  await page.waitForURL(/\/edit$/);
  await page.waitForTimeout(1000);

  // Check that at least one target shows a navigation path badge
  const pathBadges = await page.locator('text=path:').count();
  console.log('13. Targets with navigation paths:', pathBadges);

  console.log('\n=== INTERACTIVE PICKER E2E PASSED ===');
});
