import { test, expect } from '@playwright/test';
import { loginAs, registerUser } from '../helpers/auth';

/**
 * Admin feature tests: user management and stats dashboard.
 * Covers: User Management (list, deactivate users), Simple Stats Dashboard.
 * Admin credentials are seeded via docker-compose env vars: admin / supersecret123
 */

const ADMIN_USER = 'admin';
const ADMIN_PASS = 'supersecret123';

const ts = Date.now();
const targetOwner = `admin_target_owner_${ts}`;
const targetVet = `admin_target_vet_${ts}`;
const password = 'testpass';

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await registerUser(page, targetOwner, password, 'OWNER');
  await registerUser(page, targetVet, password, 'VET');
  await page.close();
});

test.describe('Admin Dashboard (Stats)', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ADMIN_USER, ADMIN_PASS);
    await page.goto('/admin/dashboard');
  });

  test('admin can view dashboard with stats cards', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Admin Dashboard' })).toBeVisible();
    // Target the label divs inside stat cards specifically (exact text match via regex)
    await expect(page.locator('div').filter({ hasText: /^Owners$/ })).toBeVisible();
    await expect(page.locator('div').filter({ hasText: /^Vets$/ })).toBeVisible();
    await expect(page.locator('div').filter({ hasText: /^Pets$/ })).toBeVisible();
    await expect(page.locator('div').filter({ hasText: /^Appointments$/ })).toBeVisible();
  });

  test('stats dashboard shows numeric values for owners and vets', async ({ page }) => {
    // After registering targetOwner and targetVet, counts should be at least 1
    await expect(page.locator('div').filter({ hasText: /^Owners$/ })).toBeVisible();
    await expect(page.locator('div').filter({ hasText: /^Vets$/ })).toBeVisible();
  });

  test('stats dashboard shows appointments per vet table', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Appointments per Vet' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Vet' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Appointments' })).toBeVisible();
  });

  test('admin nav bar shows Dashboard and Users links', async ({ page }) => {
    await loginAs(page, ADMIN_USER, ADMIN_PASS);

    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Logout' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'My Pets' })).not.toBeVisible();
  });
});

test.describe('Admin User Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, ADMIN_USER, ADMIN_PASS);
    await page.goto('/admin/users');
  });

  test('admin can view all users list', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'All Users' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Username' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Role' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Status' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Actions' })).toBeVisible();
  });

  test('registered users appear in the users list', async ({ page }) => {
    await expect(page.getByRole('cell', { name: targetOwner, exact: true })).toBeVisible();
    await expect(page.getByRole('cell', { name: targetVet, exact: true })).toBeVisible();
  });

  test('user roles are displayed correctly', async ({ page }) => {
    // Use exact cell match to find the row, then check the role cell (2nd column)
    const ownerRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: targetOwner, exact: true }),
    });
    await expect(ownerRow.locator('td').nth(1)).toContainText('OWNER');

    const vetRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: targetVet, exact: true }),
    });
    await expect(vetRow.locator('td').nth(1)).toContainText('VET');
  });

  test('active users show Active status', async ({ page }) => {
    const ownerRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: targetOwner, exact: true }),
    });
    await expect(ownerRow.locator('td').nth(2)).toContainText('Active');
  });

  test('admin can deactivate a user', async ({ page, browser }) => {
    const deactivateUser = `deactivate_${ts}`;

    // Use an isolated browser page so localStorage isn't shared with admin session
    const setupPage = await browser.newPage();
    await registerUser(setupPage, deactivateUser, password, 'OWNER');
    await setupPage.close();

    await page.reload();

    const userRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: deactivateUser, exact: true }),
    });
    await expect(userRow).toBeVisible({ timeout: 5000 });

    await userRow.getByRole('button', { name: 'Deactivate' }).click();

    await expect(userRow.locator('td').nth(2)).toContainText('Deactivated', { timeout: 5000 });
    await expect(userRow.getByRole('button', { name: 'Activate' })).toBeVisible();
  });

  test('admin can re-activate a deactivated user', async ({ page, browser }) => {
    const toggleUser = `toggle_${ts}`;

    const setupPage = await browser.newPage();
    await registerUser(setupPage, toggleUser, password, 'OWNER');
    await setupPage.close();

    await page.reload();

    const userRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: toggleUser, exact: true }),
    });
    await expect(userRow).toBeVisible({ timeout: 5000 });

    await userRow.getByRole('button', { name: 'Deactivate' }).click();
    await expect(userRow.locator('td').nth(2)).toContainText('Deactivated', { timeout: 5000 });

    await userRow.getByRole('button', { name: 'Activate' }).click();
    await expect(userRow.locator('td').nth(2)).toContainText('Active', { timeout: 5000 });
    await expect(userRow.getByRole('button', { name: 'Deactivate' })).toBeVisible();
  });

  test('admin cannot deactivate their own account', async ({ page }) => {
    // Use exact cell match to uniquely identify the admin row
    const adminRow = page.getByRole('row').filter({
      has: page.getByRole('cell', { name: ADMIN_USER, exact: true }),
    });
    await expect(adminRow).toBeVisible();
    await expect(adminRow.getByRole('button', { name: 'Deactivate' })).not.toBeVisible();
  });
});
