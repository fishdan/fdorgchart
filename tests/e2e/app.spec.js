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

async function createPersonWithSupervisor(
  page,
  suffix,
  domain,
  {
    emailPrefix,
    name,
    department,
    supervisorEmail
  }
) {
  const email = `${emailPrefix}-${suffix}@${domain}`;

  await page.goto("/create-person");
  await page.locator("#fullName").fill(name);
  await page.locator("#email").fill(email);
  await page.locator("#domain").fill(domain);
  await page.locator("#department").fill(department);

  if (supervisorEmail) {
    await page.locator("#supervisorEmail").fill(supervisorEmail);
  }

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

test("org chart shows a top leader with two direct reports and cleans up afterwards", async ({ page, request }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);

  try {
    const ceo = await createPersonWithSupervisor(page, suffix, domain, {
      emailPrefix: "ceo",
      name: `Chief Executive ${suffix}`,
      department: "Executive"
    });

    const engineerOne = await createPersonWithSupervisor(page, suffix, domain, {
      emailPrefix: "engineer-one",
      name: `Engineer One ${suffix}`,
      department: "Engineering",
      supervisorEmail: ceo.email
    });

    const engineerTwo = await createPersonWithSupervisor(page, suffix, domain, {
      emailPrefix: "engineer-two",
      name: `Engineer Two ${suffix}`,
      department: "Engineering",
      supervisorEmail: ceo.email
    });

    await loadOrgChart(page, domain);

    const tree = page.locator("#orgchart-tree");
    const rootNode = tree.locator(":scope > li");
    const directReports = rootNode.locator(":scope > ul > li");

    await expect(rootNode).toHaveCount(1);
    await expect(rootNode).toContainText(`Chief Executive ${suffix}`);
    await expect(rootNode).toContainText(ceo.email);
    await expect(directReports).toHaveCount(2);
    await expect(directReports.nth(0)).toContainText(`Engineer One ${suffix}`);
    await expect(directReports.nth(0)).toContainText(engineerOne.email);
    await expect(directReports.nth(1)).toContainText(`Engineer Two ${suffix}`);
    await expect(directReports.nth(1)).toContainText(engineerTwo.email);
    await expect(rootNode.locator(":scope > ul > li > ul > li")).toHaveCount(0);
  } finally {
    const response = await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
    expect(response.ok()).toBeTruthy();

    const cleanup = await response.json();
    expect(cleanup.deletedOrganizations).toBe(1);
    expect(cleanup.deletedPeople).toBe(3);
  }
});
