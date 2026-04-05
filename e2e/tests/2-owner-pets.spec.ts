import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Owner pet management tests: add pet, list pets, navigate to pet visits.
 * Covers: Add Pet (name, species, gender, birthDate), View Pet Profile.
 */

const ts = Date.now();
const ownerUser = `pet_owner_${ts}`;
const password = 'testpass';

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
  await expect(page.getByRole('heading', { name: 'Pets' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Add Pet' })).toBeVisible();
  await expect(page.getByText('No pets found.')).toBeVisible();
});

test('owner can add a pet with name and species', async ({ page }) => {
  await page.getByLabel('Name').fill('Fluffy');
  await page.getByLabel('Species').selectOption('CAT');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Fluffy')).toBeVisible();
  await expect(page.getByText('CAT')).toBeVisible();
});

test('owner can add a pet with all optional fields', async ({ page }) => {
  await page.getByLabel('Name').fill('Rex');
  await page.getByLabel('Species').selectOption('DOG');
  await page.getByLabel('Gender').selectOption('MALE');
  await page.getByLabel('Birth date').fill('2020-06-15');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Rex')).toBeVisible();
  await expect(page.getByText('DOG')).toBeVisible();
  await expect(page.getByText('MALE')).toBeVisible();
});

test('form resets after successful pet creation', async ({ page }) => {
  await page.getByLabel('Name').fill('Birdie');
  await page.getByLabel('Species').selectOption('BIRD');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  await expect(page.getByText('Birdie')).toBeVisible();
  // Name field should be cleared
  await expect(page.getByLabel('Name')).toHaveValue('');
});

test('pet validation: name is required', async ({ page }) => {
  await page.getByLabel('Species').selectOption('DOG');
  await page.getByRole('button', { name: 'Add Pet' }).click();

  // Should show validation error (browser native or custom)
  const nameInput = page.getByLabel('Name');
  // Either browser validation or custom error is shown
  await expect(nameInput).toBeFocused().or(expect(page.getByText(/name|species/i)).toBeVisible());
});

test('clicking View Visits navigates to pet visits page', async ({ page }) => {
  // First add a pet
  await page.getByLabel('Name').fill('Spot');
  await page.getByLabel('Species').selectOption('DOG');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Spot')).toBeVisible();

  // Click View Visits for the pet
  await page.getByRole('button', { name: 'View Visits' }).first().click();

  await expect(page).toHaveURL(/\/pets\/\d+\/visits/);
  await expect(page.getByRole('heading', { name: 'Pet Visits' })).toBeVisible();
});

test('pet visits page shows no visits for a new pet', async ({ page }) => {
  // Add a fresh pet
  await page.getByLabel('Name').fill('Nemo');
  await page.getByLabel('Species').selectOption('OTHER');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Nemo')).toBeVisible();

  await page.getByRole('button', { name: 'View Visits' }).first().click();

  await expect(page.getByText('No visits found.')).toBeVisible();
});

test('pet visits page has back button that returns to pets', async ({ page }) => {
  // Add a pet and navigate to its visits
  await page.getByLabel('Name').fill('Cleo');
  await page.getByLabel('Species').selectOption('RABBIT');
  await page.getByRole('button', { name: 'Add Pet' }).click();
  await expect(page.getByText('Cleo')).toBeVisible();

  await page.getByRole('button', { name: 'View Visits' }).first().click();
  await page.getByRole('button', { name: 'Back' }).click();

  await expect(page).toHaveURL('/pets');
});
