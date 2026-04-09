const { test, expect } = require("@playwright/test");

function pathWithOptionalSession(path, query) {
  return new RegExp(`${path}(?:;jsessionid=[^?]+)?\\?${query}$`);
}

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

  await expect(page).toHaveURL(pathWithOptionalSession("/create-organization", "success=true"));
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

  await expect(page).toHaveURL(pathWithOptionalSession("/create-person", "success=true"));
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

  await expect(page).toHaveURL(pathWithOptionalSession("/create-person", "success=true"));
  await expect(page.getByText("Person created successfully!")).toBeVisible();

  return { email };
}

async function loadOrgChart(page, domain) {
  await page.goto("/orgchart");
  await page.getByPlaceholder("Enter organization domain").fill(domain);
  await page.getByRole("button", { name: "See Org Chart" }).click();
}

async function registerAndVerifyAccount(page, suffix) {
  const email = `private-${suffix}@example.com`;

  await page.goto("/register");
  await page.getByLabel("Email Address").fill(email);
  await page.getByLabel("Password").fill("PrivatePassword123!");
  await page.getByRole("button", { name: "Create Account" }).click();

  await expect(page).toHaveURL(pathWithOptionalSession("/verify-account", "sent=true"));

  const response = await page.request.get(`/api/dev/test-data/verification-code?email=${email}`);
  expect(response.ok()).toBeTruthy();
  const payload = await response.json();
  expect(payload.code).toMatch(/^\d{6}$/);

  await page.getByLabel("Verification Code").fill(payload.code);
  await page.getByRole("button", { name: "Verify Email" }).click();
  await expect(page).toHaveURL(pathWithOptionalSession("/", "verified=true"));

  return { email };
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

test("reserved verified email cannot be added publicly by someone else", async ({ browser, page, request }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);
  const { email } = await registerAndVerifyAccount(page, suffix);

  const strangerContext = await browser.newContext({ baseURL: "http://127.0.0.1:18080" });
  const strangerPage = await strangerContext.newPage();

  try {
    await strangerPage.goto("/create-person");
    await strangerPage.locator("#fullName").fill(`Blocked User ${suffix}`);
    await strangerPage.locator("#email").fill(email);
    await strangerPage.locator("#domain").fill(domain);
    await strangerPage.locator("#department").fill("Engineering");
    await strangerPage.getByRole("button", { name: "Create Person" }).click();

    await expect(strangerPage.locator(".alert-danger")).toContainText(
      `${email} has a private account. Please send an email to ${email} asking them to add themselves to E2E Org ${suffix}`
    );
  } finally {
    await strangerContext.close();
    await request.delete(`/api/dev/test-data/account?email=${email}`);
    await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
  }
});

test("verified user can add and edit their own org entry from settings", async ({ page, request }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);
  const { email } = await registerAndVerifyAccount(page, suffix);

  try {
    await page.goto("/settings");
    await page.locator("#fullName").fill(`Private User ${suffix}`);
    await page.locator("#domain").fill(domain);
    await page.locator("#department").fill("Product");
    await page.getByRole("button", { name: "Add Myself" }).click();

    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "membershipAdded=true"));
    await expect(page.getByText(`Private User ${suffix}`)).toBeVisible();
    await expect(page.getByText(domain)).toBeVisible();

    const updateForm = page.locator("form[action='/account/memberships/update']").first();
    await updateForm.locator("input[name='department']").fill("Operations");
    await updateForm.locator("input[name='supervisorEmail']").fill(`boss-${suffix}@${domain}`);
    await updateForm.getByRole("button", { name: "Update Entry" }).click();

    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "membershipUpdated=true"));
    const updatedForm = page.locator("form[action='/account/memberships/update']").first();
    await expect(updatedForm.locator("input[name='department']")).toHaveValue("Operations");
    await expect(updatedForm.locator("input[name='supervisorEmail']")).toHaveValue(`boss-${suffix}@${domain}`);
  } finally {
    const accountCleanup = await request.delete(`/api/dev/test-data/account?email=${email}`);
    expect(accountCleanup.ok()).toBeTruthy();
    const orgCleanup = await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
    expect(orgCleanup.ok()).toBeTruthy();
  }
});

test("verified user can claim an existing open organization through DNS TXT verification", async ({ page, request }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);
  const { email } = await registerAndVerifyAccount(page, suffix);

  try {
    await page.goto("/settings");
    await page.locator("#verificationDomain").fill(domain);
    await page.getByRole("button", { name: "Start Domain Verification" }).click();

    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "challengeCreated=true"));

    const challengeCard = page.locator(".border.rounded.p-3.mb-3").filter({ hasText: domain }).first();
    const challengeToken = (await challengeCard.locator("code").textContent()).trim();

    const dnsResponse = await request.post(
      `/api/dev/test-data/dns-txt?domain=${encodeURIComponent(domain)}&value=${encodeURIComponent(challengeToken)}`
    );
    expect(dnsResponse.ok()).toBeTruthy();

    await challengeCard.getByRole("button", { name: "Check DNS TXT Record" }).click();

    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "domainVerified=true"));
    await expect(page.getByText("Domain verified successfully. You are now the first admin for that organization."))
      .toBeVisible();
    const adminCard = page.locator(".border.rounded.p-3.mb-2").filter({ hasText: `E2E Org ${suffix}` }).first();
    await expect(adminCard).toBeVisible();
    await expect(adminCard.getByText("OFFICIAL", { exact: true })).toBeVisible();
  } finally {
    await request.delete(`/api/dev/test-data/dns-txt?domain=${domain}`);
    const accountCleanup = await request.delete(`/api/dev/test-data/account?email=${email}`);
    expect(accountCleanup.ok()).toBeTruthy();
    const orgCleanup = await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
    expect(orgCleanup.ok()).toBeTruthy();
  }
});

test("official org self-join stays provisional until an admin approves it", async ({ browser, page, request }) => {
  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);
  const adminAccount = await registerAndVerifyAccount(page, `${suffix}-admin`);

  const memberContext = await browser.newContext({ baseURL: "http://127.0.0.1:18080" });
  const memberPage = await memberContext.newPage();
  let memberAccount = null;

  try {
    await page.goto("/settings");
    await page.locator("#verificationDomain").fill(domain);
    await page.getByRole("button", { name: "Start Domain Verification" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "challengeCreated=true"));

    const challengeCard = page.locator(".border.rounded.p-3.mb-3").filter({ hasText: domain }).first();
    const challengeToken = (await challengeCard.locator("code").textContent()).trim();
    const dnsResponse = await request.post(
      `/api/dev/test-data/dns-txt?domain=${encodeURIComponent(domain)}&value=${encodeURIComponent(challengeToken)}`
    );
    expect(dnsResponse.ok()).toBeTruthy();
    await challengeCard.getByRole("button", { name: "Check DNS TXT Record" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "domainVerified=true"));

    memberAccount = await registerAndVerifyAccount(memberPage, `${suffix}-member`);
    await memberPage.goto("/settings");
    await memberPage.locator("#fullName").fill(`Official Member ${suffix}`);
    await memberPage.locator("#domain").fill(domain);
    await memberPage.locator("#department").fill("Operations");
    await memberPage.getByRole("button", { name: "Add Myself" }).click();
    await expect(memberPage).toHaveURL(pathWithOptionalSession("/settings", "membershipAdded=true"));
    await expect(memberPage.getByText("PROVISIONAL")).toBeVisible();

    const beforeApproval = await request.get(`/api/orgchart?domain=${domain}`);
    expect(beforeApproval.ok()).toBeTruthy();
    expect(await beforeApproval.json()).toEqual([]);

    await page.goto("/settings");
    const managedCard = page.locator(".border.rounded.p-3.mb-3").filter({ hasText: `Official Member ${suffix}` }).first();
    await managedCard.getByRole("button", { name: "Approve" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "membershipApproved=true"));

    const approvedCard = page.locator(".border.rounded.p-3.mb-2").filter({ hasText: `Official Member ${suffix}` }).first();
    await approvedCard.getByRole("button", { name: "Grant Admin" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "adminGranted=true"));

    const afterApproval = await request.get(`/api/orgchart?domain=${domain}`);
    expect(afterApproval.ok()).toBeTruthy();
    const chart = await afterApproval.json();
    expect(chart).toHaveLength(1);
    expect(chart[0].email).toBe(memberAccount.email);
  } finally {
    await memberContext.close();
    await request.delete(`/api/dev/test-data/dns-txt?domain=${domain}`);
    if (memberAccount) {
      const memberCleanup = await request.delete(`/api/dev/test-data/account?email=${memberAccount.email}`);
      expect(memberCleanup.ok()).toBeTruthy();
    }
    const adminCleanup = await request.delete(`/api/dev/test-data/account?email=${adminAccount.email}`);
    expect(adminCleanup.ok()).toBeTruthy();
    const orgCleanup = await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
    expect(orgCleanup.ok()).toBeTruthy();
  }
});

test("private org charts show only the private message to outsiders", async ({ browser, page, request }) => {
  test.setTimeout(60000);

  const suffix = uniqueSuffix();
  const { domain } = await createOrganization(page, suffix);
  const adminAccount = await registerAndVerifyAccount(page, `${suffix}-privacy-admin`);
  let memberAccount = null;
  const memberContext = await browser.newContext({ baseURL: "http://127.0.0.1:18080" });
  const memberPage = await memberContext.newPage();
  const outsiderContext = await browser.newContext({ baseURL: "http://127.0.0.1:18080" });
  const outsiderPage = await outsiderContext.newPage();

  try {
    await page.goto("/settings");
    await page.locator("#verificationDomain").fill(domain);
    await page.getByRole("button", { name: "Start Domain Verification" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "challengeCreated=true"));

    const challengeCard = page.locator(".border.rounded.p-3.mb-3").filter({ hasText: domain }).first();
    const challengeToken = (await challengeCard.locator("code").textContent()).trim();
    const dnsResponse = await request.post(
      `/api/dev/test-data/dns-txt?domain=${encodeURIComponent(domain)}&value=${encodeURIComponent(challengeToken)}`
    );
    expect(dnsResponse.ok()).toBeTruthy();
    await challengeCard.getByRole("button", { name: "Check DNS TXT Record" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "domainVerified=true"));

    memberAccount = await registerAndVerifyAccount(memberPage, `${suffix}-privacy-member`);
    await memberPage.goto("/settings");
    await memberPage.locator("#fullName").fill(`Privacy Member ${suffix}`);
    await memberPage.locator("#domain").fill(domain);
    await memberPage.locator("#department").fill("Engineering");
    await memberPage.getByRole("button", { name: "Add Myself" }).click();
    await expect(memberPage).toHaveURL(pathWithOptionalSession("/settings", "membershipAdded=true"));

    await page.goto("/settings");
    const managedCard = page.locator(".border.rounded.p-3.mb-3").filter({ hasText: `Privacy Member ${suffix}` }).first();
    await managedCard.getByRole("button", { name: "Approve" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "membershipApproved=true"));

    const adminOrgCard = page.locator(".border.rounded.p-3.mb-2").filter({ hasText: `E2E Org ${suffix}` }).first();
    await adminOrgCard.getByRole("button", { name: "Make Private" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "chartVisibilityUpdated=true"));

    await outsiderPage.goto("/orgchart");
    await outsiderPage.getByPlaceholder("Enter organization domain").fill(domain);
    await outsiderPage.getByRole("button", { name: "See Org Chart" }).click();
    await expect(outsiderPage.locator("#error-message")).toContainText(`Organization '${domain}' chart is private.`);
    await expect(outsiderPage.locator("#orgchart-tree li")).toHaveCount(0);

    await page.goto("/orgchart");
    await page.getByPlaceholder("Enter organization domain").fill(domain);
    await page.getByRole("button", { name: "See Org Chart" }).click();
    await expect(page.locator("#error-message")).toBeHidden();
    await expect(page.locator("#orgchart-tree")).toContainText(`Privacy Member ${suffix}`);

    await page.goto("/settings");
    await adminOrgCard.getByRole("button", { name: "Make Public" }).click();
    await expect(page).toHaveURL(pathWithOptionalSession("/settings", "chartVisibilityUpdated=true"));

    await outsiderPage.goto("/orgchart");
    await outsiderPage.getByPlaceholder("Enter organization domain").fill(domain);
    await outsiderPage.getByRole("button", { name: "See Org Chart" }).click();
    await expect(outsiderPage.locator("#error-message")).toBeHidden();
    await expect(outsiderPage.locator("#orgchart-tree")).toContainText(`Privacy Member ${suffix}`);
  } finally {
    await memberContext.close();
    await outsiderContext.close();
    await request.delete(`/api/dev/test-data/dns-txt?domain=${domain}`);
    if (memberAccount) {
      const memberCleanup = await request.delete(`/api/dev/test-data/account?email=${memberAccount.email}`);
      expect(memberCleanup.ok()).toBeTruthy();
    }
    const adminCleanup = await request.delete(`/api/dev/test-data/account?email=${adminAccount.email}`);
    expect(adminCleanup.ok()).toBeTruthy();
    const orgCleanup = await request.delete(`/api/dev/test-data/organization?domain=${domain}`);
    expect(orgCleanup.ok()).toBeTruthy();
  }
});
