import { test, expect, type Page } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Owner pet management tests: add pet, list pets, navigate to pet visits.
 * Covers: Add Pet (name, species, gender, birthDate), View Pet Profile.
 *
 * PetsPage uses <label>Text</label><input> without htmlFor/id, so getByLabel()
 * does not work. Use CSS adjacent-sibling selectors instead.
 */

const ts = Date.now();
const ownerUser = `pet_owner_${ts}`;
const password = 'testpass';

/** Locators for the Add Pet form fields (no htmlFor/id association in PetsPage). */
function petForm(page: Page) {
  return {
    name:      page.locator('label:has-text("Name") + input'),
    species:   page.locator('label:has-text("Species") + select'),
    gender:    page.locator('label:has-text("Gender") + select'),
    birthDate: page.locator('label:has-text("Birth date") + input'),
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
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Rex')).toBeVisible();
  const petItem = page.getByRole('listitem').filter({ hasText: 'Rex' });
  await expect(petItem).toContainText('DOG');
  await expect(petItem).toContainText('MALE');
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
