import { test, expect } from '@playwright/test';

test('debug campaign creation', async ({ page }) => {
  page.on('response', res => {
    if (res.url().includes('/api/campaigns') && res.request().method() === 'POST') {
      console.log('CREATE RESPONSE:', res.status());
      res.text().then(body => console.log('BODY:', body.substring(0, 300)));
    }
    if (res.status() >= 400) {
      console.log('ERROR:', res.status(), res.url().substring(res.url().indexOf('/api')));
    }
  });

  await page.goto('http://169.58.43.127/login');
  await page.fill('input[type="email"]', 'admin@clickmaster.io');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('http://169.58.43.127/', { timeout: 10000 });
  console.log('1. Logged in');

  // Make sure a site exists
  await page.click('a:has-text("Sites")');
  await page.waitForTimeout(1000);
  
  let hasSite = await page.locator('text=No sites').count() === 0;
  if (!hasSite) {
    console.log('2a. Creating site...');
    await page.click('button:has-text("Add Site")');
    await page.fill('input[placeholder="My Website"]', 'Test Site');
    await page.fill('input[placeholder="https://example.com"]', 'http://httpbin.org/get');
    await page.click('button:has-text("Create")');
    await page.waitForTimeout(1000);
  } else {
    console.log('2a. Site exists');
  }

  // Create campaign
  await page.click('a:has-text("Campaigns")');
  await page.click('a:has-text("New Campaign")');
  console.log('3. On campaign form');

  await page.fill('input[placeholder="Campaign name..."]', 'Debug Test');
  await page.locator('select').first().selectOption({ index: 1 });
  console.log('4. Form filled');

  // Submit and capture response
  const responsePromise = page.waitForResponse(
    r => r.url().includes('/api/campaigns') && r.request().method() === 'POST',
    { timeout: 15000 }
  );

  await page.click('button:has-text("Create Campaign")');
  console.log('5. Clicked create');

  const response = await responsePromise;
  console.log('6. Response:', response.status());

  if (response.status() >= 400) {
    const body = await response.text();
    console.log('   ERROR:', body.substring(0, 500));
    
    // Check what we sent
    const pageText = await page.innerText('body');
    console.log('   Page still on form:', pageText.includes('Campaign name'));
  } else {
    const body = await response.text();
    console.log('   SUCCESS:', body.substring(0, 200));
    await page.waitForURL(/\/campaigns\//, { timeout: 10000 }).catch(() => {
      console.log('   URL did not change');
    });
    console.log('   Final URL:', page.url());
  }
});
