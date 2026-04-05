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
  // Create users that the admin will manage
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
    // Stats cards should be visible
    await expect(page.getByText('Owners')).toBeVisible();
    await expect(page.getByText('Vets')).toBeVisible();
    await expect(page.getByText('Pets')).toBeVisible();
    await expect(page.getByText('Appointments')).toBeVisible();
  });

  test('stats dashboard shows numeric values for owners and vets', async ({ page }) => {
    // After registering targetOwner and targetVet, counts should be at least 1
    const ownersCard = page.locator('div').filter({ hasText: /^Owners$/ }).first();
    const vetsCard = page.locator('div').filter({ hasText: /^Vets$/ }).first();

    await expect(ownersCard).toBeVisible();
    await expect(vetsCard).toBeVisible();
  });

  test('stats dashboard shows appointments per vet table', async ({ page }) => {
    await expect(page.getByRole('heading', { name: 'Appointments per Vet' })).toBeVisible();
    // Table headers
    await expect(page.getByRole('columnheader', { name: 'Vet' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Appointments' })).toBeVisible();
  });

  test('admin nav bar shows Dashboard and Users links', async ({ page }) => {
    await loginAs(page, ADMIN_USER, ADMIN_PASS);

    await expect(page.getByRole('link', { name: 'Dashboard' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Users' })).toBeVisible();
    await expect(page.getByRole('link', { name: 'Logout' })).toBeVisible();
    // Admin should NOT see pet-owner-specific links
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

    // Table columns should be visible
    await expect(page.getByRole('columnheader', { name: 'Username' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Role' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Status' })).toBeVisible();
    await expect(page.getByRole('columnheader', { name: 'Actions' })).toBeVisible();
  });

  test('registered users appear in the users list', async ({ page }) => {
    await expect(page.getByRole('cell', { name: targetOwner })).toBeVisible();
    await expect(page.getByRole('cell', { name: targetVet })).toBeVisible();
  });

  test('user roles are displayed correctly', async ({ page }) => {
    // The registered users should show their role
    const ownerRow = page.getByRole('row').filter({ hasText: targetOwner });
    await expect(ownerRow.getByText('OWNER')).toBeVisible();

    const vetRow = page.getByRole('row').filter({ hasText: targetVet });
    await expect(vetRow.getByText('VET')).toBeVisible();
  });

  test('active users show Active status', async ({ page }) => {
    const ownerRow = page.getByRole('row').filter({ hasText: targetOwner });
    await expect(ownerRow.getByText('Active')).toBeVisible();
  });

  test('admin can deactivate a user', async ({ page }) => {
    const deactivateUser = `deactivate_${ts}`;
    // Register a new user to deactivate
    const setupPage = await page.context().newPage();
    await registerUser(setupPage, deactivateUser, password, 'OWNER');
    await setupPage.close();

    await page.reload();

    // Find the user row and deactivate
    const userRow = page.getByRole('row').filter({ hasText: deactivateUser });
    await expect(userRow).toBeVisible({ timeout: 5000 });

    await userRow.getByRole('button', { name: 'Deactivate' }).click();

    // Status should change to Deactivated
    await expect(userRow.getByText('Deactivated')).toBeVisible({ timeout: 5000 });
    await expect(userRow.getByRole('button', { name: 'Activate' })).toBeVisible();
  });

  test('admin can re-activate a deactivated user', async ({ page }) => {
    const toggleUser = `toggle_${ts}`;

    const setupPage = await page.context().newPage();
    await registerUser(setupPage, toggleUser, password, 'OWNER');
    await setupPage.close();

    await page.reload();

    const userRow = page.getByRole('row').filter({ hasText: toggleUser });
    await expect(userRow).toBeVisible({ timeout: 5000 });

    // Deactivate
    await userRow.getByRole('button', { name: 'Deactivate' }).click();
    await expect(userRow.getByText('Deactivated')).toBeVisible({ timeout: 5000 });

    // Re-activate
    await userRow.getByRole('button', { name: 'Activate' }).click();
    await expect(userRow.getByText('Active')).toBeVisible({ timeout: 5000 });
    await expect(userRow.getByRole('button', { name: 'Deactivate' })).toBeVisible();
  });

  test('admin cannot deactivate their own account', async ({ page }) => {
    const adminRow = page.getByRole('row').filter({ hasText: ADMIN_USER });
    await expect(adminRow).toBeVisible();
    // No Deactivate button for own account
    await expect(adminRow.getByRole('button', { name: 'Deactivate' })).not.toBeVisible();
  });
});
