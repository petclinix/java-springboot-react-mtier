import { test, expect, Browser } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';
import { ensureVetIsAlwaysOpen } from '../helpers/locations';
import { tomorrowDateString, yesterdayDateString } from '../helpers/dates';

/**
 * Owner appointment tests: book appointment (location -> pet -> appointment type
 * -> date -> live-fetched slot pick), list appointments, reschedule, cancel.
 */

const ts = Date.now();
const ownerUser = `appt_owner_${ts}`;
const vetUser = `appt_vet_${ts}`;
const password = 'testpass';
const petName = `TestPet_${ts}`;
let locationName: string;

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();

  // Register vet so the owner can book against it, and give it a location that is
  // open every day/hour so picking "tomorrow" as the booking date always yields a
  // non-empty slot list regardless of the backend's opening-hours conflict check.
  await registerUser(page, vetUser, password, 'VET');
  await loginAs(page, vetUser, password);
  locationName = await ensureVetIsAlwaysOpen(page);

  // Register owner, add a pet
  await registerUser(page, ownerUser, password, 'OWNER');
  await loginAs(page, ownerUser, password);
  await page.goto('/pets');
  await page.locator('label:has-text("Name") + input').fill(petName);
  await page.locator('label:has-text("Species") + select').selectOption('DOG');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText(petName)).toBeVisible();

  await page.close();
});

/**
 * Registers a fresh vet + location + owner + pet, and books one appointment for
 * tomorrow against the first available slot. Used to isolate reschedule/cancel
 * scenarios from each other and from the shared ownerUser/vetUser above.
 */
async function setupOwnerWithBookedAppointment(
  browser: Browser,
  suffix: string,
): Promise<{ owner: string; pet: string }> {
  const vet = `appt_${suffix}_vet_${ts}`;
  const owner = `appt_${suffix}_owner_${ts}`;
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

  return { owner, pet };
}

test.describe('Appointments list', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ownerUser, password);
    await page.goto('/appointments');
  });

  test('appointments page shows empty state initially', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'My Appointments' })).toBeVisible();
    await expect(page.getByText('No appointments found.')).toBeVisible();
  });

  test('Book appointment button links to booking page', async ({ page }) => {
    await page.getByRole('button', { name: '+ Book appointment' }).click();

    await expect(page).toHaveURL('/appointments/book');
  });
});

test.describe('Booking appointment', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ownerUser, password);
    await page.goto('/appointments/book');
  });

  test('booking page shows location, pet, appointment type, and date fields', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /book an appointment/i })).toBeVisible();
    await expect(page.getByText('Choose a location')).toBeVisible();
    await expect(page.getByText('Choose a pet')).toBeVisible();
    await expect(page.getByText('Appointment type')).toBeVisible();
    await expect(page.getByText('Date', { exact: true })).toBeVisible();
  });

  test('owner can book an appointment by choosing location, pet, type, date, and slot', async ({ page }) => {
    await page
      .locator('label:has-text("Choose a location") + select')
      .selectOption({ label: `${locationName} — ${vetUser}` });
    await page.locator('label:has-text("Appointment type") + select').selectOption('CHECKUP');
    await page.locator('label:has-text("Date") + input').fill(tomorrowDateString());

    // Available slots are fetched live once location, type, and date are all set.
    const slotButton = page.getByRole('button').filter({ hasText: /\d{1,2}:\d{2}/ }).first();
    await expect(slotButton).toBeVisible({ timeout: 5000 });
    await slotButton.click();

    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText(/appointment created/i)).toBeVisible();
  });

  test('booking fails when no date is set', async ({ page }) => {
    // Location and pet are preselected automatically; submitting without a date
    // should surface the date validation message.
    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText('Please choose a date.')).toBeVisible();
  });

  test('booking a past date shows no available slots', async ({ page }) => {
    // There's no free-text time entry to "make invalid" anymore — picking a date
    // that's already elapsed simply yields an empty slot list from the backend.
    await page.locator('label:has-text("Date") + input').fill(yesterdayDateString());

    await expect(
      page.getByText('No available slots for this day — try another date.'),
    ).toBeVisible({ timeout: 5000 });
  });
});

test.describe('Reschedule appointment', () => {
  test('owner can reschedule an appointment to a new date and time', async ({ page, browser }) => {
    const { owner, pet } = await setupOwnerWithBookedAppointment(browser, 'resched1');

    await loginAs(page, owner, password);
    await page.goto('/appointments');

    const row = page.getByRole('listitem').filter({ hasText: pet });
    await expect(row).toBeVisible({ timeout: 5000 });
    const originalTime = await row.locator('strong').textContent();

    await row.getByRole('button', { name: 'Reschedule' }).click();
    await expect(row.getByRole('button', { name: 'Close' })).toBeVisible();

    // Pick a date two days out so the new slot is guaranteed to differ from the
    // original booking regardless of which slot the backend offers first.
    const target = new Date();
    target.setDate(target.getDate() + 2);
    const targetDateStr = target.toISOString().substring(0, 10);

    const dateInput = row.locator('input[type="date"]');
    await dateInput.fill(targetDateStr);

    const slotButton = row.getByRole('button').filter({ hasText: /\d{1,2}:\d{2}/ }).first();
    await expect(slotButton).toBeVisible({ timeout: 5000 });
    await slotButton.click();

    await row.getByRole('button', { name: 'Save' }).click();

    // Reschedule form closes on success (button reverts from "Close" to "Reschedule").
    await expect(row.getByRole('button', { name: 'Reschedule' })).toBeVisible({ timeout: 5000 });
    const updatedTime = await row.locator('strong').textContent();
    expect(updatedTime).not.toBe(originalTime);
  });

  test('reschedule shows an inline error and keeps the form open when no slot is chosen', async ({
    page,
    browser,
  }) => {
    const { owner, pet } = await setupOwnerWithBookedAppointment(browser, 'resched2');

    await loginAs(page, owner, password);
    await page.goto('/appointments');

    const row = page.getByRole('listitem').filter({ hasText: pet });
    await expect(row).toBeVisible({ timeout: 5000 });

    await row.getByRole('button', { name: 'Reschedule' }).click();
    await expect(row.getByRole('button', { name: 'Close' })).toBeVisible();

    // Submit without picking a date/slot — purely client-side validation, no
    // backend round trip, so this is not subject to timing flakiness.
    await row.getByRole('button', { name: 'Save' }).click();

    await expect(row.getByText('Please choose an available time slot.')).toBeVisible();
    // Form stays open — state isn't lost on a failed reschedule.
    await expect(row.getByRole('button', { name: 'Close' })).toBeVisible();
  });
});

test.describe('Cancel appointment', () => {
  test('owner can cancel an appointment', async ({ page, browser }) => {
    // Book against a freshly registered vet/location rather than vetUser/locationName
    // so a single vet can't end up double-booked at the same moment by other tests.
    const cancelVet = `appt_cancel_vet_${ts}`;
    const setupPage = await browser.newPage();
    await registerUser(setupPage, cancelVet, password, 'VET');
    await loginAs(setupPage, cancelVet, password);
    const cancelLocationName = await ensureVetIsAlwaysOpen(setupPage);
    await setupPage.close();

    await loginAs(page, ownerUser, password);
    await page.goto('/appointments/book');

    await page
      .locator('label:has-text("Choose a location") + select')
      .selectOption({ label: `${cancelLocationName} — ${cancelVet}` });
    await page.locator('label:has-text("Date") + input').fill(tomorrowDateString());
    const slotButton = page.getByRole('button').filter({ hasText: /\d{1,2}:\d{2}/ }).first();
    await expect(slotButton).toBeVisible({ timeout: 5000 });
    await slotButton.click();
    await page.getByRole('button', { name: /book appointment/i }).click();
    await expect(page.getByText(/appointment created/i)).toBeVisible();

    // Go to appointments list and cancel
    await page.goto('/appointments');

    // Wait for list to load and record count before cancelling
    const cancelBtns = page.getByRole('button', { name: 'Cancel' });
    await expect(cancelBtns.first()).toBeVisible();
    const countBefore = await cancelBtns.count();
    await cancelBtns.first().click();

    // After cancel, one fewer appointment in the list
    await expect(cancelBtns).toHaveCount(countBefore - 1, { timeout: 10000 });
  });
});
