import { test, expect, type Page } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Vet location management tests: create, view, edit, delete locations with weekly periods.
 * Covers: Set Availability (weekly schedule, bookable slots).
 *
 * LocationsPage uses <label>Text</label><input> without htmlFor/id, so getByLabel()
 * does not work. Use CSS adjacent-sibling selectors instead.
 */

const ts = Date.now();
const vetUser = `loc_vet_${ts}`;
const password = 'testpass';

/** Locators for the location edit form fields (no htmlFor/id association in LocationsPage). */
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

/** Fills all required location form fields. */
async function fillLocationForm(page: Page, name: string, overrides: Record<string, string> = {}) {
  const f = locationForm(page);
  await f.name.fill(name);
  await f.zoneId.fill(overrides.zoneId ?? 'Europe/Vienna');
  await f.street.fill(overrides.street ?? 'Main Street 1');
  await f.postalCode.fill(overrides.postalCode ?? '1010');
  await f.city.fill(overrides.city ?? 'Vienna');
  await f.country.fill(overrides.country ?? 'Austria');
}

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await registerUser(page, vetUser, password, 'VET');
  await page.close();
});

test.beforeEach(async ({ page }) => {
  await loginAs(page, vetUser, password);
  await page.goto('/locations');
});

test('locations page renders with empty state', async ({ page }) => {
  await expect(page.getByRole('heading', { name: 'Locations' })).toBeVisible();
  await expect(page.getByText('No locations yet.')).toBeVisible();
  await expect(page.getByRole('button', { name: 'New' })).toBeVisible();
});

test('vet can create a new location', async ({ page }) => {
  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, `Clinic ${ts}`);
  await page.getByRole('button', { name: 'Save' }).click();

  await expect(page.getByText(`Clinic ${ts}`, { exact: true })).toBeVisible();
});

test('vet can add a weekly period to a location', async ({ page }) => {
  const locationName = `WeeklyClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, locationName, { zoneId: 'Europe/Berlin', street: 'Vet Street 5', postalCode: '10115', city: 'Berlin', country: 'Germany' });

  await page.getByRole('button', { name: 'Add period' }).click();
  await expect(page.getByText('No weekly periods')).not.toBeVisible();

  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName, { exact: true })).toBeVisible();

  // Reopen and verify period persisted
  await page.getByRole('button', { name: 'Close' }).click();
  await page.getByRole('button', { name: 'Open' }).first().click();
  await expect(page.getByText('No weekly periods')).not.toBeVisible();
});

test('vet can add an opening exception (closed day)', async ({ page }) => {
  const locationName = `ExceptionClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, locationName, { zoneId: 'Europe/Vienna', street: 'Holiday Rd', postalCode: '5020', city: 'Salzburg', country: 'Austria' });

  await page.getByRole('button', { name: 'Add override' }).click();
  await expect(page.getByText('No overrides')).not.toBeVisible();

  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName, { exact: true })).toBeVisible();
});

test('vet can view location detail by clicking Open', async ({ page }) => {
  const locationName = `DetailClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, locationName, { zoneId: 'UTC', street: 'Detail St', postalCode: '12345', city: 'TestCity', country: 'TestCountry' });
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName, { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Close' }).click();
  await page.getByRole('button', { name: 'Open' }).first().click();

  await expect(page.getByText(locationName, { exact: true }).first()).toBeVisible();
});

test('vet can edit a location name', async ({ page }) => {
  const originalName = `EditMe_${ts}`;
  const updatedName = `Edited_${ts}`;

  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, originalName, { zoneId: 'Europe/London', street: 'Edit Lane', postalCode: 'SW1A', city: 'London', country: 'UK' });
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(originalName, { exact: true })).toBeVisible();

  await page.getByRole('button', { name: 'Edit' }).first().click();

  const nameInput = locationForm(page).name;
  await nameInput.clear();
  await nameInput.fill(updatedName);

  await page.getByRole('button', { name: 'Save' }).click();

  await expect(page.getByText(updatedName, { exact: true })).toBeVisible();
});

test('vet can delete a location', async ({ page }) => {
  const locationName = `DeleteMe_${ts}`;

  await page.getByRole('button', { name: 'New' }).click();
  await fillLocationForm(page, locationName, { zoneId: 'UTC', street: 'Gone St', postalCode: '00000', city: 'Nowhere', country: 'Neverland' });
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName, { exact: true })).toBeVisible();

  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Del' }).first().click();

  await expect(page.getByText(locationName, { exact: true })).not.toBeVisible({ timeout: 5000 });
});
