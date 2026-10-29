// @ts-check
const { test, expect } = require('@playwright/test');

test.describe('iTalase Agentic Dashboard E2E', () => {
  test('has title and loads Warung Liquid Glass interface', async ({ page }) => {
    await page.goto('/');

    // Expect a title containing "iTalase"
    await expect(page).toHaveTitle(/iTalase/);

    // Expect the dashboard root container to be visible
    const root = page.locator('#root');
    await expect(root).toBeVisible();
  });
});
