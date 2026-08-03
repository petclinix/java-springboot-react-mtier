import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';
import { ensureVetIsAlwaysOpen } from '../helpers/locations';

/**
 * Owner appointment tests: book appointment, list appointments, cancel appointment.
 * Covers: Book Appointment (select location, time slot), view and cancel appointments.
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
  // open every day/hour so the "prefill tomorrow 10:00" booking flow is never
  // rejected by the backend's opening-hours conflict check.
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

  test('booking page shows vet and pet selects', async ({ page }) => {
    await expect(page.getByRole('heading', { name: /book an appointment/i })).toBeVisible();
    await expect(page.getByText('Choose a location')).toBeVisible();
    await expect(page.getByText('Choose a pet')).toBeVisible();
    await expect(page.getByText('Date & time')).toBeVisible();
  });

  test('owner can book appointment with prefilled tomorrow date', async ({ page }) => {
    // The booking page preselects the first location in the dropdown, which may not be
    // the one with opening hours set up in beforeAll — select explicitly.
    await page.locator('select').first().selectOption({ label: `${locationName} — ${vetUser}` });

    // Use the prefill button to set a valid future date
    await page.getByRole('button', { name: /prefill.*tomorrow/i }).click();

    // Location dropdown should have our registered location
    await expect(page.locator('select').first()).not.toHaveValue('');

    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText(/appointment created/i)).toBeVisible();
  });

  test('booking fails when no date is set', async ({ page }) => {
    // Do not fill date/time, just submit
    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText('Please choose a date and time.')).toBeVisible();
  });

  test('booking fails when date is in the past', async ({ page }) => {
    // Set a past date directly in the datetime-local input
    const pastDate = new Date();
    pastDate.setDate(pastDate.getDate() - 1);
    const pastStr = pastDate.toISOString().substring(0, 16);

    await page.locator('input[type="datetime-local"]').fill(pastStr);
    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText(/future/i)).toBeVisible();
  });
});

test.describe('Cancel appointment', () => {
  test('owner can cancel an appointment', async ({ page, browser }) => {
    // Book against a freshly registered vet/location rather than vetUser/locationName —
    // the "Booking appointment" tests above already booked that vet at the exact same
    // prefilled "tomorrow 10:00" slot, and the backend now rejects overlapping
    // appointments for the same vet (422 "already has an appointment overlapping").
    const cancelVet = `appt_cancel_vet_${ts}`;
    const setupPage = await browser.newPage();
    await registerUser(setupPage, cancelVet, password, 'VET');
    await loginAs(setupPage, cancelVet, password);
    const cancelLocationName = await ensureVetIsAlwaysOpen(setupPage);
    await setupPage.close();

    await loginAs(page, ownerUser, password);
    await page.goto('/appointments/book');

    // Book one
    await page.locator('select').first().selectOption({ label: `${cancelLocationName} — ${cancelVet}` });
    await page.getByRole('button', { name: /prefill.*tomorrow/i }).click();
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
