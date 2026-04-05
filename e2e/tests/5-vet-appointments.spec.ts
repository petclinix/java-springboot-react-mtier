import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Vet appointment management and visit documentation tests.
 * Covers:
 *   - Vet: View Appointments, Cancel Appointment
 *   - Vet: Record Visit (diagnosis/vet summary, vaccination, owner summary)
 *   - Owner: View visit history (diagnosis & vaccination history) via Pet Visits page
 */

const ts = Date.now();
const ownerUser = `vis_owner_${ts}`;
const vetUser = `vis_vet_${ts}`;
const password = 'testpass';
const petName = `VisPet_${ts}`;

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();

  await registerUser(page, vetUser, password, 'VET');
  await registerUser(page, ownerUser, password, 'OWNER');

  // Owner adds a pet
  await loginAs(page, ownerUser, password);
  await page.goto('/pets');
  await page.locator('label:has-text("Name") + input').fill(petName);
  await page.locator('label:has-text("Species") + select').selectOption('CAT');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText(petName)).toBeVisible();

  // Owner books appointment with the specific vet
  await page.goto('/appointments/book');
  await page.locator('select').first().selectOption({ label: vetUser });
  await page.getByRole('button', { name: /prefill.*tomorrow/i }).click();
  await page.getByRole('button', { name: /book appointment/i }).click();
  await expect(page.getByText(/appointment created/i)).toBeVisible();

  await page.close();
});

test.describe('Vet appointment list', () => {
  test('vet can view their appointments page', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByRole('heading', { name: 'My Appointments' })).toBeVisible();
    // The booked appointment should appear
    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(ownerUser)).toBeVisible();
  });

  test('vet appointment row shows Visit and Cancel buttons', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await expect(page.getByRole('button', { name: 'Visit' })).toBeVisible();
    await expect(page.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });
});

test.describe('Visit documentation', () => {
  test('vet can open visit documentation for an appointment', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'Visit' }).first().click();

    await expect(page).toHaveURL(/\/appointments\/vet\/visit\/\d+/);
    await expect(page.getByRole('heading', { name: 'Visit Documentation' })).toBeVisible();
  });

  test('vet can record vet summary, vaccination, and owner summary', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'Visit' }).first().click();

    await expect(page.getByLabel('Vet Summary')).toBeVisible();

    await page.getByLabel('Vet Summary').fill('Patient is healthy, no issues found.');
    await page.getByLabel('Vaccination').fill('Rabies booster administered');
    await page.getByLabel('Owner Summary').fill('Annual checkup complete. Keep up with diet.');

    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText(/saved successfully/i)).toBeVisible();
  });

  test('visit documentation persists after reload', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'Visit' }).first().click();

    // Fill and save
    await page.getByLabel('Vet Summary').fill('Persistent vet notes');
    await page.getByLabel('Vaccination').fill('FVRCP');
    await page.getByLabel('Owner Summary').fill('Persistent owner notes');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText(/saved successfully/i)).toBeVisible();

    // Reload the page
    await page.reload();

    // Values should still be present
    await expect(page.getByLabel('Vet Summary')).toHaveValue('Persistent vet notes');
    await expect(page.getByLabel('Vaccination')).toHaveValue('FVRCP');
    await expect(page.getByLabel('Owner Summary')).toHaveValue('Persistent owner notes');
  });

  test('visit documentation Back button returns to vet appointments', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'Visit' }).first().click();
    await page.getByRole('button', { name: 'Back' }).click();

    await expect(page).toHaveURL('/appointments/vet');
  });
});

test.describe('Owner views pet visit history', () => {
  test.beforeEach(async ({ page }) => {
    // Vet records visit first
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');
    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'Visit' }).first().click();
    await page.getByLabel('Owner Summary').fill('Vaccination given, all clear.');
    await page.getByLabel('Vaccination').fill('Parvovirus');
    await page.getByRole('button', { name: 'Save' }).click();
    await expect(page.getByText(/saved successfully/i)).toBeVisible();
  });

  test('owner can see visit history for their pet', async ({ page }) => {
    await loginAs(page, ownerUser, password);
    await page.goto('/pets');

    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await page.getByRole('button', { name: 'View Visits' }).first().click();

    await expect(page).toHaveURL(/\/pets\/\d+\/visits/);
    await expect(page.getByRole('heading', { name: 'Pet Visits' })).toBeVisible();

    // Visit data should be visible
    await expect(page.getByText(vetUser)).toBeVisible();
    await expect(page.getByText('Vaccination given, all clear.')).toBeVisible();
    await expect(page.getByText('Parvovirus')).toBeVisible();
  });
});

test.describe('Vet cancel appointment', () => {
  test('vet can cancel an appointment', async ({ page, browser }) => {
    // Book a fresh appointment for cancellation (to not disturb other tests)
    const cancelOwner = `cancel_owner_${ts}`;
    const cancelVet = `cancel_vet_${ts}`;

    const setupPage = await browser.newPage();
    await registerUser(setupPage, cancelVet, password, 'VET');
    await registerUser(setupPage, cancelOwner, password, 'OWNER');
    await loginAs(setupPage, cancelOwner, password);
    await setupPage.goto('/pets');
    await setupPage.locator('label:has-text("Name") + input').fill(`CancelPet_${ts}`);
    await setupPage.locator('label:has-text("Species") + select').selectOption('DOG');
    await setupPage.getByRole('button', { name: 'Add Pet' }).click();
    await expect(setupPage.getByText(`CancelPet_${ts}`)).toBeVisible();
    await setupPage.goto('/appointments/book');
    await setupPage.locator('select').first().selectOption({ label: cancelVet });
    await setupPage.getByRole('button', { name: /prefill.*tomorrow/i }).click();
    await setupPage.getByRole('button', { name: /book appointment/i }).click();
    await expect(setupPage.getByText(/appointment created/i)).toBeVisible();
    await setupPage.close();

    await loginAs(page, cancelVet, password);
    await page.goto('/appointments/vet');

    const cancelBtn = page.getByRole('button', { name: 'Cancel' }).first();
    await expect(cancelBtn).toBeVisible({ timeout: 5000 });
    await cancelBtn.click();

    await expect(page.getByText('No appointments found.')).toBeVisible({ timeout: 5000 });
  });
});
