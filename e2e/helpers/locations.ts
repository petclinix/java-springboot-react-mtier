import { Page, expect } from '@playwright/test';

/**
 * Locators for the location edit form fields (no htmlFor/id association in
 * LocationsPage). Mirrors locationForm() in 4-vet-locations.spec.ts.
 */
function locationForm(page: Page) {
  return {
    name:       page.locator('label:has-text("Name") + input'),
    zoneId:     page.locator('label:has-text("Zone ID") + input'),
    street:     page.locator('label:has-text("Street") + input'),
    postalCode: page.locator('label:has-text("Postal Code") + input'),
    city:       page.locator('label:has-text("City") + input'),
    country:    page.locator('label:has-text("Country") + input'),
  };
}

/**
 * Assumes `page` is already logged in as a VET. Creates a new location with a
 * weekly period for every day of the week (1-7) spanning 00:00-23:59, and saves it.
 *
 * This guarantees the vet is "always open" regardless of which weekday the e2e suite
 * happens to run on, so appointment-booking flows that use the "prefill tomorrow
 * 10:00" shortcut always find an open location and aren't rejected by the backend's
 * opening-hours conflict check (VetClosedAtRequestedTimeException).
 */
export async function ensureVetIsAlwaysOpen(page: Page): Promise<string> {
  await page.goto('/locations');
  await page.getByRole('button', { name: 'New' }).click();

  const f = locationForm(page);
  const name = `AlwaysOpen_${Date.now()}`;
  await f.name.fill(name);
  await f.zoneId.fill('UTC');
  await f.street.fill('Main Street 1');
  await f.postalCode.fill('00000');
  await f.city.fill('Anytown');
  await f.country.fill('Anyland');

  // Add one period row per day of week (1-7); LocationsPage has no other <select>
  // or <input type="time"> elements on this panel, so plain nth() indexing is safe.
  for (let i = 0; i < 7; i++) {
    await page.getByRole('button', { name: 'Add period' }).click();
  }

  const daySelects = page.locator('select');
  const timeInputs = page.locator('input[type="time"]');

  for (let i = 0; i < 7; i++) {
    await daySelects.nth(i).selectOption(String(i + 1));
    await timeInputs.nth(i * 2).fill('00:00');
    await timeInputs.nth(i * 2 + 1).fill('23:59');
  }

  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(name, { exact: true })).toBeVisible();

  return name;
}
