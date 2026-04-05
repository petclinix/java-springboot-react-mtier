import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Authentication tests: registration and login for OWNER and VET roles.
 * Covers: Register/Login for all user types, validation errors, About Me page.
 */

const ts = Date.now();
const ownerUser = `auth_owner_${ts}`;
const vetUser = `auth_vet_${ts}`;
const password = 'testpass';

test.describe('Registration', () => {
  test('owner can register and is redirected to login', async ({ page }) => {
    await registerUser(page, ownerUser, password, 'OWNER');

    await expect(page).toHaveURL('/login');
    await expect(page.getByText('Registration successful')).toBeVisible();
  });

  test('vet can register and is redirected to login', async ({ page }) => {
    await registerUser(page, vetUser, password, 'VET');

    await expect(page).toHaveURL('/login');
    await expect(page.getByText('Registration successful')).toBeVisible();
  });

  test('registration fails when username is too short', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Username').fill('ab');
    await page.getByLabel('Password').fill(password);
    await page.getByRole('button', { name: /register/i }).click();

    await expect(page.getByRole('alert')).toContainText('3 characters');
  });

  test('registration fails when password is too short', async ({ page }) => {
    await page.goto('/register');
    await page.getByLabel('Username').fill(`shortpw_${ts}`);
    await page.getByLabel('Password').fill('ab');
    await page.getByRole('button', { name: /register/i }).click();

    await expect(page.getByRole('alert')).toContainText('3 characters');
  });
});

test.describe('Login', () => {
  test.beforeAll(async ({ browser }) => {
    const page = await browser.newPage();
    await registerUser(page, `login_owner_${ts}`, password, 'OWNER');
    await page.close();
  });

  test('owner can log in and sees owner nav items', async ({ page }) => {
    await loginAs(page, `login_owner_${ts}`, password);

    await expect(page).toHaveURL('/');
    await expect(page.getByRole('link', { name: 'My Pets' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Appointments' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Logout' })).toBeVisible();
  });

  test('login fails with wrong password', async ({ page }) => {
    await page.goto('/login');
    await page.getByLabel('Username').fill(`login_owner_${ts}`);
    await page.getByLabel('Password').fill('wrongpassword');
    await page.getByRole('button', { name: /login/i }).click();

    await expect(page.getByText(/error|invalid|incorrect|unauthorized/i)).toBeVisible();
  });

  test('unauthenticated user is redirected when accessing protected page', async ({ page }) => {
    await page.goto('/aboutme');

    await expect(page).not.toHaveURL('/aboutme');
  });
});

test.describe('About Me', () => {
  test('authenticated user can view About Me page with username and role', async ({ page }) => {
    const user = `aboutme_owner_${ts}`;
    await registerUser(page, user, password, 'OWNER');
    await loginAs(page, user, password);

    await page.goto('/aboutme');

    await expect(page.getByText(user)).toBeVisible();
    await expect(page.getByText('OWNER', { exact: true })).toBeVisible();
  });
});
