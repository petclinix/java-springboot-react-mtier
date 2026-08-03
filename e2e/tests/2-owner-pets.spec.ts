import { test, expect, type Page } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Owner pet management tests: add pet, list pets, navigate to pet visits.
 * Covers: Add Pet (name, species, gender, birthDate, breed, picture), View Pet Profile.
 *
 * PetsPage uses <label>Text</label><input> without htmlFor/id, so getByLabel()
 * does not work. Use CSS adjacent-sibling selectors instead.
 */

const ts = Date.now();
const ownerUser = `pet_owner_${ts}`;
const password = 'testpass';

// Smallest valid PNG (1x1 transparent pixel), used to exercise picture upload
// without needing a fixture file on disk.
const ONE_PX_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=',
  'base64'
);

/** Locators for the Add Pet form fields (no htmlFor/id association in PetsPage). */
function petForm(page: Page) {
  return {
    name:      page.locator('label:has-text("Name") + input'),
    species:   page.locator('label:has-text("Species") + select'),
    gender:    page.locator('label:has-text("Gender") + select'),
    birthDate: page.locator('label:has-text("Birth date") + input'),
    breed:     page.locator('label:has-text("Breed") + input'),
    picture:   page.locator('label:has-text("Picture") + input[type="file"]'),
  };
}

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await registerUser(page, ownerUser, password, 'OWNER');
  await page.close();
});

test.beforeEach(async ({ page }) => {
  await loginAs(page, ownerUser, password);
  await page.goto('/pets');
});

test('pets page renders with add pet form and empty list', async ({ page }) => {
  // Use exact:true to avoid matching "All Pets" heading
  await expect(page.getByRole('heading', { name: 'Pets', exact: true })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Add Pet' })).toBeVisible();
  await expect(page.getByText('No pets found.')).toBeVisible();
});

test('owner can add a pet with name and species', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Fluffy');
  await f.species.selectOption('CAT');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Fluffy')).toBeVisible();
  const petItem = page.getByRole('listitem').filter({ hasText: 'Fluffy' });
  await expect(petItem).toContainText('CAT');
});

test('owner can add a pet with all optional fields', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Rex');
  await f.species.selectOption('DOG');
  await f.gender.selectOption('MALE');
  await f.birthDate.fill('2020-06-15');
  await f.breed.fill('Labrador');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Rex')).toBeVisible();
  const petItem = page.getByRole('listitem').filter({ hasText: 'Rex' });
  await expect(petItem).toContainText('DOG');
  await expect(petItem).toContainText('MALE');
  await expect(petItem).toContainText('Labrador');
});

test('owner can add a pet with a picture and see its thumbnail', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Pixel');
  await f.species.selectOption('CAT');
  await f.picture.setInputFiles({
    name: 'pixel.png',
    mimeType: 'image/png',
    buffer: ONE_PX_PNG,
  });
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Pixel')).toBeVisible();
  const petItem = page.getByRole('listitem').filter({ hasText: 'Pixel' });
  const thumbnail = petItem.locator('img');
  await expect(thumbnail).toBeVisible();
  await expect(thumbnail).toHaveAttribute('src', /^data:image\//);
});

test('owner can add a pet with breed but no other optional fields', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Whiskers');
  await f.species.selectOption('CAT');
  await f.breed.fill('Siamese');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Whiskers')).toBeVisible();
  const petItem = page.getByRole('listitem').filter({ hasText: 'Whiskers' });
  await expect(petItem).toContainText('CAT');
  await expect(petItem).toContainText('Siamese');
});

test('form resets after successful pet creation', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Birdie');
  await f.species.selectOption('BIRD');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Birdie')).toBeVisible();
  await expect(f.name).toHaveValue('');
});

test('pet validation: name is required', async ({ page }) => {
  const f = petForm(page);
  await f.species.selectOption('DOG');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  // Browser native required validation focuses the empty field
  await expect(f.name).toBeFocused();
});

test('clicking View Visits navigates to pet visits page', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Spot');
  await f.species.selectOption('DOG');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Spot')).toBeVisible();

  await page.getByRole('button', { name: 'View Visits' }).first().click();

  await expect(page).toHaveURL(/\/pets\/\d+\/visits/);
  await expect(page.getByRole('heading', { name: 'Pet Visits' })).toBeVisible();
});

test('pet visits page shows no visits for a new pet', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Nemo');
  await f.species.selectOption('OTHER');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Nemo')).toBeVisible();

  await page.getByRole('button', { name: 'View Visits' }).first().click();

  await expect(page.getByText('No visits found.')).toBeVisible();
});

test('pet visits page has back button that returns to pets', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Cleo');
  await f.species.selectOption('RABBIT');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Cleo')).toBeVisible();

  await page.getByRole('button', { name: 'View Visits' }).first().click();
  await page.getByRole('button', { name: 'Back' }).click();

  await expect(page).toHaveURL('/pets');
});

test('owner can edit a pet name and see the correction reflected in the list', async ({ page }) => {
  // Note: pet names must be unique per owner (DB constraint on name+owner_id), and this
  // file shares one owner across tests, so "Buddyy"/"Buddy" are picked to avoid colliding
  // with any pet name used by another test in this file (e.g. the existing "Fluffy" pet
  // added by "owner can add a pet with name and species").
  const f = petForm(page);
  await f.name.fill('Buddyy');
  await f.species.selectOption('CAT');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Buddyy', { exact: true })).toBeVisible();

  const petRow = page.getByRole('listitem').filter({ hasText: 'Buddyy' });
  await petRow.getByRole('button', { name: 'Edit' }).click();

  await expect(page.getByRole('heading', { name: 'Edit Pet' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Save' })).toBeVisible();

  await f.name.fill('Buddy');
  await page.getByRole('button', { name: 'Save' }).click();

  // corrected name shows up, typo name is gone (exact match avoids "Buddyy" vs "Buddy" false positives)
  await expect(page.getByText('Buddy', { exact: true })).toBeVisible();
  await expect(page.getByText('Buddyy', { exact: true })).not.toBeVisible();

  // form reverts to Add Pet mode — catches a stuck-in-edit-mode regression
  await expect(page.getByRole('heading', { name: 'Add Pet' })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Add Pet' })).toBeVisible();
});

test('owner can remove a pet and it disappears from the list', async ({ page }) => {
  page.on('dialog', dialog => dialog.accept());

  const f = petForm(page);
  await f.name.fill('Ghost');
  await f.species.selectOption('OTHER');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Ghost', { exact: true })).toBeVisible();

  const petRow = page.getByRole('listitem').filter({ hasText: 'Ghost' });
  await petRow.getByRole('button', { name: 'Remove' }).click();

  // Shared ownerUser accumulates pets from earlier tests — assert only that
  // this test's pet is gone, not that the list is empty.
  await expect(page.getByText('Ghost', { exact: true })).not.toBeVisible();
});

test('dismissing the remove confirmation keeps the pet in the list', async ({ page }) => {
  const f = petForm(page);
  await f.name.fill('Casper');
  await f.species.selectOption('OTHER');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Casper', { exact: true })).toBeVisible();

  // Local one-time handler so it doesn't interact with other tests' dialogs.
  page.once('dialog', dialog => dialog.dismiss());
  const petRow = page.getByRole('listitem').filter({ hasText: 'Casper' });
  await petRow.getByRole('button', { name: 'Remove' }).click();

  await expect(page.getByText('Casper', { exact: true })).toBeVisible();
});
