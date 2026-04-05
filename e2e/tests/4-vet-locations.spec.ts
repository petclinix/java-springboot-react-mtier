import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Vet location management tests: create, view, edit, delete locations with weekly periods.
 * Covers: Set Availability (weekly schedule, bookable slots).
 */

const ts = Date.now();
const vetUser = `loc_vet_${ts}`;
const password = 'testpass';

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

  // Fill location details
  await page.getByLabel('Name').fill(`Clinic ${ts}`);
  await page.getByLabel('Zone ID').fill('Europe/Vienna');
  await page.getByLabel('Street').fill('Main Street 1');
  await page.getByLabel('Postal Code').fill('1010');
  await page.getByLabel('City').fill('Vienna');
  await page.getByLabel('Country').fill('Austria');

  await page.getByRole('button', { name: 'Save' }).click();

  // Location should appear in the list
  await expect(page.getByText(`Clinic ${ts}`)).toBeVisible();
});

test('vet can add a weekly period to a location', async ({ page }) => {
  const locationName = `WeeklyClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();

  await page.getByLabel('Name').fill(locationName);
  await page.getByLabel('Zone ID').fill('Europe/Berlin');
  await page.getByLabel('Street').fill('Vet Street 5');
  await page.getByLabel('Postal Code').fill('10115');
  await page.getByLabel('City').fill('Berlin');
  await page.getByLabel('Country').fill('Germany');

  // Add a weekly period
  await page.getByRole('button', { name: 'Add period' }).click();

  // A period row should appear with day/time selects
  await expect(page.getByText('No weekly periods')).not.toBeVisible();

  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName)).toBeVisible();

  // Open the location to verify period is saved
  await page.getByRole('button', { name: 'Open' }).first().click();

  // The period should still be there after reload
  await expect(page.getByText('No weekly periods')).not.toBeVisible();
});

test('vet can add an opening exception (closed day)', async ({ page }) => {
  const locationName = `ExceptionClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();

  await page.getByLabel('Name').fill(locationName);
  await page.getByLabel('Zone ID').fill('Europe/Vienna');
  await page.getByLabel('Street').fill('Holiday Rd');
  await page.getByLabel('Postal Code').fill('5020');
  await page.getByLabel('City').fill('Salzburg');
  await page.getByLabel('Country').fill('Austria');

  // Add an override/exception
  await page.getByRole('button', { name: 'Add override' }).click();

  // Override row should appear
  await expect(page.getByText('No overrides')).not.toBeVisible();

  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName)).toBeVisible();
});

test('vet can view location detail by clicking Open', async ({ page }) => {
  const locationName = `DetailClinic_${ts}`;
  await page.getByRole('button', { name: 'New' }).click();
  await page.getByLabel('Name').fill(locationName);
  await page.getByLabel('Zone ID').fill('UTC');
  await page.getByLabel('Street').fill('Detail St');
  await page.getByLabel('Postal Code').fill('12345');
  await page.getByLabel('City').fill('TestCity');
  await page.getByLabel('Country').fill('TestCountry');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName)).toBeVisible();

  // Close the detail panel then reopen via list button
  await page.getByRole('button', { name: 'Close' }).click();
  await page.getByRole('button', { name: 'Open' }).first().click();

  // Should show the location name in the detail panel
  await expect(page.getByText(locationName).first()).toBeVisible();
});

test('vet can edit a location name', async ({ page }) => {
  const originalName = `EditMe_${ts}`;
  const updatedName = `Edited_${ts}`;

  // Create location
  await page.getByRole('button', { name: 'New' }).click();
  await page.getByLabel('Name').fill(originalName);
  await page.getByLabel('Zone ID').fill('Europe/London');
  await page.getByLabel('Street').fill('Edit Lane');
  await page.getByLabel('Postal Code').fill('SW1A');
  await page.getByLabel('City').fill('London');
  await page.getByLabel('Country').fill('UK');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(originalName)).toBeVisible();

  // Click Edit in detail panel
  await page.getByRole('button', { name: 'Edit' }).first().click();

  // Clear and fill new name
  const nameInput = page.getByLabel('Name');
  await nameInput.clear();
  await nameInput.fill(updatedName);

  await page.getByRole('button', { name: 'Save' }).click();

  // Updated name should appear in the list
  await expect(page.getByText(updatedName)).toBeVisible();
});

test('vet can delete a location', async ({ page }) => {
  const locationName = `DeleteMe_${ts}`;

  // Create location
  await page.getByRole('button', { name: 'New' }).click();
  await page.getByLabel('Name').fill(locationName);
  await page.getByLabel('Zone ID').fill('UTC');
  await page.getByLabel('Street').fill('Gone St');
  await page.getByLabel('Postal Code').fill('00000');
  await page.getByLabel('City').fill('Nowhere');
  await page.getByLabel('Country').fill('Neverland');
  await page.getByRole('button', { name: 'Save' }).click();
  await expect(page.getByText(locationName)).toBeVisible();

  // Click Del in the list (not the detail panel Delete)
  page.on('dialog', dialog => dialog.accept());
  await page.getByRole('button', { name: 'Del' }).first().click();

  // Location should be gone
  await expect(page.getByText(locationName)).not.toBeVisible({ timeout: 5000 });
});
