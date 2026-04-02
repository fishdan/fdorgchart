# Speckit Constitution

This constitution defines the default engineering standards for work in this repository. All Speckit tasks, specs, implementation plans, and code changes should align with these principles unless an approved task explicitly documents a justified exception.

## 1. Modularity First

Code must be designed to be as modular and reusable as possible.

Rules:
- Prefer small, focused classes and methods with one clear responsibility.
- Keep business logic out of controllers and views.
- Extract reusable logic into services, utilities, or shared domain types instead of duplicating behavior.
- Avoid tightly coupling features when a clean interface or shared component will keep future changes cheaper.
- Favor composition over inheritance unless inheritance clearly simplifies the model.

## 2. Prefer POJOs Over DTOs

We prefer POJOs over DTOs.

Rules:
- Do not introduce DTOs by default.
- Use plain domain objects or other simple POJOs unless there is a concrete boundary that requires a separate transport shape.
- If a DTO is introduced, the task must explain why a POJO was insufficient.
- Avoid mapping layers that add ceremony without reducing real complexity.

## 3. Simple, Explicit Design

The codebase should stay easy to understand.

Rules:
- Choose straightforward designs over clever abstractions.
- Add indirection only when it creates real reuse, testability, or separation of concerns.
- Keep naming concrete and aligned with the business/domain language.
- Prefer explicit behavior over hidden magic.

## 4. Safe Change Boundaries

Changes should be easy to reason about and easy to review.

Rules:
- Keep tasks scoped to a clear outcome.
- Avoid opportunistic refactors unless they are necessary for the active task.
- Preserve existing behavior unless the task explicitly changes it.
- Document notable tradeoffs when a change affects architecture, data shape, or runtime behavior.

## 5. Test and Verify What Matters

Work is not complete when code merely compiles.

Rules:
- Prefer TDD when practical. Start with a failing test or executable check before implementing behavior.
- For bug fixes, reproduce the failure with a test or another reliable verification step before changing code when feasible.
- New business logic should normally arrive with tests close to the code it protects.
- Validate behavior at the level appropriate for the change.
- Prefer tests around business logic and regression-prone paths.
- When tests are missing or not practical, record the manual verification performed.
- Bug fixes should include verification that the failure mode is actually addressed.

## 6. Comments for Shared Maintenance

Comments should be written for a codebase maintained by both humans and AI tools.

Rules:
- Write comments to explain intent, constraints, and non-obvious reasoning.
- Prefer comments that help a future maintainer understand why the code exists or why a tradeoff was chosen.
- Do not add comments that restate obvious code behavior.
- Keep comments accurate, durable, and easy for both humans and automated tools to interpret.
- When behavior is subtle, comment the invariant or assumption directly near the relevant code.

## 7. Security and Secrets Discipline

Security shortcuts are not acceptable.

Rules:
- Never commit secrets, credentials, tokens, or private keys.
- Prefer environment-based configuration for sensitive values.
- Minimize exposure of internal diagnostics and administrative endpoints.
- Treat authentication, authorization, and data-handling changes as high scrutiny areas.

## 8. Spec-Driven Work

We prefer work to be tied to an explicit Speckit task.

Rules:
- New implementation work should be connected to a defined Speckit task or spec.
- If work is requested outside a tracked task, pause and decide whether it should be formalized first.
- Mark tasks complete only when implementation and verification are both complete.

## 9. Exceptions

Exceptions are allowed only when they are intentional and documented.

Rules:
- Any exception to this constitution should be noted in the relevant Speckit task, plan, or review notes.
- Exceptions should explain the constraint, the tradeoff, and why the default rule was not used.

## Decision Standard

When multiple valid approaches exist, prefer the option that is:
- more modular
- more reusable
- simpler to understand
- easier to verify
- less dependent on unnecessary DTO or mapping layers
