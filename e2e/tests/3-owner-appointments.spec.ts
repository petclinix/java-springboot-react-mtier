import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Owner appointment tests: book appointment, list appointments, cancel appointment.
 * Covers: Book Appointment (select vet, time slot), view and cancel appointments.
 */

const ts = Date.now();
const ownerUser = `appt_owner_${ts}`;
const vetUser = `appt_vet_${ts}`;
const password = 'testpass';
const petName = `TestPet_${ts}`;

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();

  // Register vet so the owner can book against it
  await registerUser(page, vetUser, password, 'VET');

  // Register owner, add a pet
  await registerUser(page, ownerUser, password, 'OWNER');
  await loginAs(page, ownerUser, password);
  await page.goto('/pets');
  await page.getByLabel('Name').fill(petName);
  await page.getByLabel('Species').selectOption('DOG');
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
    await expect(page.getByText('Choose a veterinarian')).toBeVisible();
    await expect(page.getByText('Choose a pet')).toBeVisible();
    await expect(page.getByText('Date & time')).toBeVisible();
  });

  test('owner can book appointment with prefilled tomorrow date', async ({ page }) => {
    // Use the prefill button to set a valid future date
    await page.getByRole('button', { name: /prefill.*tomorrow/i }).click();

    // Vet dropdown should have our registered vet
    await expect(page.locator('select').first()).not.toHaveValue('');

    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText(/appointment created/i)).toBeVisible();
  });

  test('booking fails when no date is set', async ({ page }) => {
    // Do not fill date/time, just submit
    await page.getByRole('button', { name: /book appointment/i }).click();

    await expect(page.getByText(/date|time|choose/i)).toBeVisible();
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
  test('owner can cancel an appointment', async ({ page }) => {
    await loginAs(page, ownerUser, password);
    await page.goto('/appointments/book');

    // Book one
    await page.getByRole('button', { name: /prefill.*tomorrow/i }).click();
    await page.getByRole('button', { name: /book appointment/i }).click();
    await expect(page.getByText(/appointment created/i)).toBeVisible();

    // Go to appointments list and cancel
    await page.goto('/appointments');

    // Wait for list to load
    const cancelBtn = page.getByRole('button', { name: 'Cancel' }).first();
    await expect(cancelBtn).toBeVisible();
    await cancelBtn.click();

    // After cancel, appointment is removed
    await expect(page.getByText('No appointments found.')).toBeVisible({ timeout: 5000 });
  });
});
