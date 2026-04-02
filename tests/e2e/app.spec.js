const { test, expect } = require("@playwright/test");

function uniqueSuffix() {
  return `${Date.now()}-${Math.floor(Math.random() * 100000)}`;
}

async function createOrganization(page, suffix) {
  const domain = `e2e-${suffix}.example.com`;
  const email = `admin-${suffix}@${domain}`;

  await page.goto("/create-organization");
  await page.getByLabel("Organization Name").fill(`E2E Org ${suffix}`);
  await page.getByLabel("Domain").fill(domain);
  await page.getByLabel("Email Address").fill(email);
  await page.getByLabel("Password").fill("PlaywrightPassword123!");
  await page.getByRole("button", { name: "Create Organization" }).click();

  await expect(page).toHaveURL(/\/create-organization\?success=true$/);
  await expect(page.getByText("Organization created successfully!")).toBeVisible();

  return { domain, email };
}

async function createPerson(page, suffix, domain, namePrefix = "E2E Person") {
  const email = `person-${suffix}@${domain}`;

  await page.goto("/create-person");
  await page.locator("#fullName").fill(`${namePrefix} ${suffix}`);
  await page.locator("#email").fill(email);
  await page.locator("#domain").fill(domain);
  await page.locator("#department").fill("Engineering");
  await page.getByRole("button", { name: "Create Person" }).click();

  await expect(page).toHaveURL(/\/create-person\?success=true$/);
  await expect(page.getByText("Person created successfully!")).toBeVisible();

  return { email };
}

async function loadOrgChart(page, domain) {
  await page.goto("/orgchart");
  await page.getByPlaceholder("Enter organization domain").fill(domain);
  await page.getByRole("button", { name: "See Org Chart" }).click();
}

test("homepage loads with primary navigation", async ({ page }) => {
  await page.goto("/");

  await expect(page).toHaveTitle(/Welcome to MyOrgChart/);
  await expect(page.getByRole("heading", { name: "Welcome to MyOrgChart" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Create Organization" })).toBeVisible();
  await expect(page.getByRole("link", { name: "Create Person" })).toBeVisible();
  await expect(page.getByRole("link", { name: "See Org Chart" })).toBeVisible();
});

test("organization creation succeeds through the browser flow", async ({ page }) => {
  const suffix = uniqueSuffix();

  await createOrganization(page, suffix);
});

test("person creation succeeds for an existing organization", async ({ page }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);

  await createPerson(page, suffix, domain);
});

test("person creation shows an error when the organization does not exist", async ({ page }) => {
  const suffix = uniqueSuffix();
  const domain = `missing-${suffix}.example.com`;

  await page.goto("/create-person");
  await page.locator("#fullName").fill(`Missing Org User ${suffix}`);
  await page.locator("#email").fill(`missing-${suffix}@${domain}`);
  await page.locator("#domain").fill(domain);
  await page.locator("#department").fill("Engineering");
  await page.getByRole("button", { name: "Create Person" }).click();

  await expect(page).toHaveURL(/\/api\/people$/);
  await expect(page.getByText(`No organization exists with the domain: ${domain}`)).toBeVisible();
});

test("org chart shows an error for a missing organization", async ({ page }) => {
  const suffix = uniqueSuffix();
  const domain = `missing-orgchart-${suffix}.example.com`;

  await loadOrgChart(page, domain);

  await expect(page.locator("#error-message")).toContainText(
    `Organization with domain '${domain}' does not exist.`
  );
  await expect(page.locator("#orgchart-tree li")).toHaveCount(0);
});

test("org chart shows an empty-state message for an organization with no people", async ({ page }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);

  await loadOrgChart(page, domain);

  await expect(page.locator("#info-message")).toContainText(
    `Organization '${domain}' exists, but no people have been added yet.`
  );
  await expect(page.locator("#orgchart-tree li")).toHaveCount(0);
});

test("org chart shows people for a populated organization", async ({ page }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);

  await createPerson(page, suffix, domain, "Org Chart User");
  await loadOrgChart(page, domain);

  await expect(page.locator("#error-message")).toBeHidden();
  await expect(page.locator("#info-message")).toBeHidden();
  await expect(page.locator("#orgchart-tree li")).toHaveCount(1);
  await expect(page.locator("#orgchart-tree")).toContainText(`Org Chart User ${suffix}`);
  await expect(page.locator("#orgchart-tree")).toContainText(`person-${suffix}@${domain}`);
});
