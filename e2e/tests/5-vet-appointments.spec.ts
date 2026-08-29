import { test, expect, Browser, Page } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';
import { ensureVetIsAlwaysOpen } from '../helpers/locations';
import { tomorrowDateString } from '../helpers/dates';

/**
 * Vet appointment management and visit documentation tests.
 * Covers:
 *   - Vet: View Appointments, Confirm, Mark no-show, Cancel Appointment
 *   - Vet: Record Visit (diagnosis/vet summary, vaccination, owner summary) — gated to CONFIRMED
 *   - Owner: View visit history (diagnosis & vaccination history) via Pet Visits page
 */

const ts = Date.now();
const ownerUser = `vis_owner_${ts}`;
const vetUser = `vis_vet_${ts}`;
const password = 'testpass';
const petName = `VisPet_${ts}`;
let locationName: string;

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();

  await registerUser(page, vetUser, password, 'VET');
  await loginAs(page, vetUser, password);
  locationName = await ensureVetIsAlwaysOpen(page);

  await registerUser(page, ownerUser, password, 'OWNER');

  // Owner adds a pet
  await loginAs(page, ownerUser, password);
  await page.goto('/pets');
  await page.locator('label:has-text("Name") + input').fill(petName);
  await page.locator('label:has-text("Species") + select').selectOption('CAT');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText(petName)).toBeVisible();

  // Owner books appointment at the specific vet's location for tomorrow, picking
  // the first live-fetched available slot.
  await page.goto('/appointments/book');
  await page
    .locator('label:has-text("Choose a location") + select')
    .selectOption({ label: `${locationName} — ${vetUser}` });
  await page.locator('label:has-text("Date") + input').fill(tomorrowDateString());
  const slotButton = page.getByRole('button').filter({ hasText: /\d{1,2}:\d{2}/ }).first();
  await expect(slotButton).toBeVisible({ timeout: 5000 });
  await slotButton.click();
  await page.getByRole('button', { name: /book appointment/i }).click();
  await expect(page.getByText(/appointment created/i)).toBeVisible();

  await page.close();
});

/**
 * Registers a fresh vet + owner + pet, and books one appointment for tomorrow
 * against the first available slot. Used to isolate Confirm/No-show/Cancel
 * scenarios (which transition appointment status) from each other and from the
 * shared vetUser/petName appointment used by the Visit documentation tests below.
 */
async function setupBookedAppointment(
  browser: Browser,
  suffix: string,
): Promise<{ vet: string; owner: string; pet: string }> {
  const vet = `vis_${suffix}_vet_${ts}`;
  const owner = `vis_${suffix}_owner_${ts}`;
  const pet = `${suffix}Pet_${ts}`;

  const setupPage = await browser.newPage();
  await registerUser(setupPage, vet, password, 'VET');
  await loginAs(setupPage, vet, password);
  const locName = await ensureVetIsAlwaysOpen(setupPage);

  await registerUser(setupPage, owner, password, 'OWNER');
  await loginAs(setupPage, owner, password);
  await setupPage.goto('/pets');
  await setupPage.locator('label:has-text("Name") + input').fill(pet);
  await setupPage.locator('label:has-text("Species") + select').selectOption('DOG');
  await setupPage.getByRole('button', { name: 'Add Pet' }).click();
  await expect(setupPage.getByText(pet)).toBeVisible();

  await setupPage.goto('/appointments/book');
  await setupPage
    .locator('label:has-text("Choose a location") + select')
    .selectOption({ label: `${locName} — ${vet}` });
  await setupPage.locator('label:has-text("Date") + input').fill(tomorrowDateString());
  const slotButton = setupPage.getByRole('button').filter({ hasText: /\d{1,2}:\d{2}/ }).first();
  await expect(slotButton).toBeVisible({ timeout: 5000 });
  await slotButton.click();
  await setupPage.getByRole('button', { name: /book appointment/i }).click();
  await expect(setupPage.getByText(/appointment created/i)).toBeVisible();
  await setupPage.close();

  return { vet, owner, pet };
}

/**
 * Navigates to /appointments/vet, confirms the appointment matching petNameToMatch
 * if it's still BOOKED (Confirm button present), then clicks Visit. Confirming is
 * idempotent across repeated calls against the same appointment — once CONFIRMED,
 * the Confirm button is gone and this just clicks Visit directly. This lets several
 * independent tests share one appointment the way the original suite did, without
 * each one assuming a specific prior status.
 */
async function confirmAndOpenVisit(page: Page, petNameToMatch: string): Promise<void> {
  await page.goto('/appointments/vet');

  const row = page.getByRole('listitem').filter({ hasText: petNameToMatch });
  await expect(row).toBeVisible({ timeout: 5000 });

  const confirmBtn = row.getByRole('button', { name: 'Confirm' });
  if (await confirmBtn.isVisible().catch(() => false)) {
    await confirmBtn.click();
    await expect(row.getByRole('button', { name: 'Visit' })).toBeVisible({ timeout: 5000 });
  }

  await row.getByRole('button', { name: 'Visit' }).click();
}

test.describe('Vet appointment list', () => {
  test('vet can view their appointments page', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    await expect(page.getByRole('heading', { name: 'My Appointments' })).toBeVisible();
    // The booked appointment should appear
    await expect(page.getByText(petName)).toBeVisible({ timeout: 5000 });
    await expect(page.getByText(ownerUser)).toBeVisible();
  });

  test('freshly booked appointment shows Confirm and Cancel buttons, not Visit', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await page.goto('/appointments/vet');

    const row = page.getByRole('listitem').filter({ hasText: petName });
    await expect(row).toBeVisible({ timeout: 5000 });
    await expect(row.getByRole('button', { name: 'Confirm' })).toBeVisible();
    await expect(row.getByRole('button', { name: 'Cancel' })).toBeVisible();
    await expect(row.getByRole('button', { name: 'Visit' })).not.toBeVisible();
    await expect(row.getByRole('button', { name: 'Mark no-show' })).not.toBeVisible();
  });
});

test.describe('Confirm appointment', () => {
  test('vet can confirm a booked appointment', async ({ page, browser }) => {
    const { vet, pet } = await setupBookedAppointment(browser, 'confirm');

    await loginAs(page, vet, password);
    await page.goto('/appointments/vet');

    const row = page.getByRole('listitem').filter({ hasText: pet });
    await expect(row).toBeVisible({ timeout: 5000 });
    await expect(row.getByRole('button', { name: 'Confirm' })).toBeVisible();

    await row.getByRole('button', { name: 'Confirm' }).click();

    // Status transitions in place — no reload needed.
    await expect(row.getByText('CONFIRMED', { exact: true })).toBeVisible({ timeout: 5000 });
    await expect(row.getByRole('button', { name: 'Confirm' })).not.toBeVisible();
    await expect(row.getByRole('button', { name: 'Mark no-show' })).toBeVisible();
    await expect(row.getByRole('button', { name: 'Visit' })).toBeVisible();
    await expect(row.getByRole('button', { name: 'Cancel' })).toBeVisible();
  });
});

test.describe('Mark no-show', () => {
  test('vet can mark a confirmed appointment as no-show', async ({ page, browser }) => {
    const { vet, pet } = await setupBookedAppointment(browser, 'noshow');

    await loginAs(page, vet, password);
    await page.goto('/appointments/vet');

    const row = page.getByRole('listitem').filter({ hasText: pet });
    await expect(row).toBeVisible({ timeout: 5000 });
    await row.getByRole('button', { name: 'Confirm' }).click();
    await expect(row.getByRole('button', { name: 'Mark no-show' })).toBeVisible({ timeout: 5000 });

    await row.getByRole('button', { name: 'Mark no-show' }).click();

    // Immediately after: badge updates and every action button disappears (status
    // is terminal), but the row itself is not removed from the local list yet.
    await expect(row.getByText('NO_SHOW', { exact: true })).toBeVisible({ timeout: 5000 });
    await expect(row.getByRole('button', { name: 'Confirm' })).not.toBeVisible();
    await expect(row.getByRole('button', { name: 'Mark no-show' })).not.toBeVisible();
    await expect(row.getByRole('button', { name: 'Visit' })).not.toBeVisible();
    await expect(row.getByRole('button', { name: 'Cancel' })).not.toBeVisible();

    // GET /vet/appointments only returns BOOKED/CONFIRMED, so after a refetch the
    // no-show'd appointment disappears entirely. This vet has no other appointment.
    await page.reload();
    await expect(page.getByText('No appointments found.')).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Visit documentation', () => {
  test('vet can open visit documentation for an appointment', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await confirmAndOpenVisit(page, petName);

    await expect(page).toHaveURL(/\/appointments\/vet\/visit\/\d+/);
    await expect(page.getByRole('heading', { name: 'Visit Documentation' })).toBeVisible();
  });

  test('vet can record vet summary, vaccination, and owner summary', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await confirmAndOpenVisit(page, petName);

    await expect(page.getByLabel('Vet Summary')).toBeVisible();

    await page.getByLabel('Vet Summary').fill('Patient is healthy, no issues found.');
    await page.getByLabel('Vaccination').fill('Rabies booster administered');
    await page.getByLabel('Owner Summary').fill('Annual checkup complete. Keep up with diet.');

    await page.getByRole('button', { name: 'Save' }).click();

    await expect(page.getByText(/saved successfully/i)).toBeVisible();
  });

  test('visit documentation persists after reload', async ({ page }) => {
    await loginAs(page, vetUser, password);
    await confirmAndOpenVisit(page, petName);

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
    await confirmAndOpenVisit(page, petName);
    await page.getByRole('button', { name: 'Back' }).click();

    await expect(page).toHaveURL('/appointments/vet');
  });
});

test.describe('Owner views pet visit history', () => {
  test.beforeEach(async ({ page }) => {
    // Vet records visit first (appointment is CONFIRMED by the Visit documentation
    // tests above; confirmAndOpenVisit is idempotent either way).
    await loginAs(page, vetUser, password);
    await confirmAndOpenVisit(page, petName);
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
    const { vet } = await setupBookedAppointment(browser, 'cancel');

    await loginAs(page, vet, password);
    await page.goto('/appointments/vet');

    const cancelBtn = page.getByRole('button', { name: 'Cancel' }).first();
    await expect(cancelBtn).toBeVisible({ timeout: 5000 });
    await cancelBtn.click();

    await expect(page.getByText('No appointments found.')).toBeVisible({ timeout: 5000 });
  });
});
