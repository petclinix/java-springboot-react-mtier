import { Page } from '@playwright/test';

/**
 * Fills the login form and waits for navigation to complete.
 * Assumes the login page renders a Username field, a Password field,
 * and a submit button matching /login/i.
 */
export async function loginAs(page: Page, username: string, password: string): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: /login/i }).click();
  // Wait for the post-login redirect to home, which also ensures the JWT is
  // stored in localStorage before the test navigates to a protected page.
  await page.waitForURL('/');
}

/**
 * Registers a new user via the register form, then navigates to login.
 * type must be 'OWNER' or 'VET'.
 */
export async function registerUser(
  page: Page,
  username: string,
  password: string,
  type: 'OWNER' | 'VET',
): Promise<void> {
  await page.goto('/register');
  await page.getByLabel('Username').fill(username);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('Type').selectOption(type.toLowerCase());
  await page.getByRole('button', { name: /register/i }).click();
  await page.waitForURL('/login');
}