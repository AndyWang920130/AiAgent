# Role Config under System Setting

Add a **Role Config** page under System Setting that manages the catalog of roles, and make
that catalog the source of truth so custom roles become assignable in User Management.

## Design decisions

`User.role` currently uses the `Role` enum (`@Enumerated(EnumType.STRING)` over a
`varchar(255)` column). To make catalog-defined roles assignable, the field becomes a
`String` holding the role **code**. The column type does not change, so **no data
migration is needed** for existing rows.

The `Role` enum is kept, but demoted to named constants for the two built-in roles
(`Role.ADMIN.name()`, `Role.USER.name()`) — used by seeding and the last-admin guard. This
avoids scattering `"ADMIN"` string literals through the security code.

**Access control is unchanged.** `SecurityConfig` keeps its `hasRole("ADMIN")` rules, so
`ADMIN` remains the only privileged role. A custom role (e.g. `EDITOR`) receives a
`ROLE_EDITOR` authority and therefore has the same access as `USER`. That follows directly
from "identity only, no permissions" — roles are labels until permissions are added.

Guards on the catalog, enforced server-side since the API is reachable without the UI:
- A built-in role (`USER`, `ADMIN`) cannot be deleted, disabled, or have its code changed.
- A role still held by any user cannot be deleted or disabled.
- Codes are uppercased and validated as `[A-Z0-9_]+`, unique case-insensitively.

## Backend — new files

| File | Purpose |
|---|---|
| `domain/RoleConfig.java` | Entity `twsny_role_config`: `code` (unique), `name`, `description`, `sortOrder`, `enabled`, `builtIn`. Extends `AbstractAuditingEntity`, fluent setters, matching `GameConfig`. |
| `repository/RoleConfigRepository.java` | `findAllByOrderBySortOrderAscIdAsc`, `findOneByCodeIgnoreCase`, `existsByCodeIgnoreCase` |
| `service/dto/RoleConfigDTO.java` | Mirrors the entity plus a read-only `userCount`. `@NotBlank`/`@Size`/`@Pattern` on `code`, `@NotBlank` on `name`. |
| `service/mapper/RoleConfigMapper.java` | `@Mapper(componentModel = "spring")` extending `EntityMapper`, as `GameConfigMapper` |
| `service/RoleConfigService.java` | CRUD + the guards above; populates `userCount` per role via one grouped count query |
| `web/rest/RoleConfigResource.java` | `GET/POST/PUT/DELETE /api/v1/role-configs`, shaped like `GameConfigResource` |
| `resources/db/mysql/upgrade-role-config.sql` | `CREATE TABLE IF NOT EXISTS` + `INSERT IGNORE` seeding ADMIN/USER as built-in, `AUTO_INCREMENT = 1000` |

## Backend — modified files

**Role type change (`Role` → `String`), mechanical:**
- `domain/User.java:50-52` — drop `@Enumerated`, field becomes `String role`; getter/setter/fluent setter and `toString` follow.
- `service/dto/AdminUserDTO.java:104-108` — `Role` → `String`.
- `web/rest/vm/AdminCreateUserRequest.java` / `AdminUpdateUserRequest.java` — `@NotNull Role role` → `@NotBlank @Size(max = 50) String role`.
- `web/rest/AdminUserResource.java:63` — `@RequestParam Role role` → `String role`.
- `repository/UserRepository.java:66-75` — `findForAdmin` and `countActiveByRole` take `String`; JPQL is unchanged.
- `security/UserDetailsServiceImpl.java:23` — `.roles(u.getRole() != null ? u.getRole() : Role.USER.name())`.
- `security/JwtAuthFilter.java:58-61` — build `ROLE_` + the stored string, same null fallback.
- `web/rest/AuthController.java:62-64` — role string straight into the JWT claim and `UserInfo`.

**Behavioural changes:**
- `service/AdminUserService.java` — `create`/`update` validate the submitted role against the
  catalog (must exist and be enabled) and reject otherwise with `BadRequestAlertException`.
  `guardRoleChange`/`guardLastAdmin` compare against `Role.ADMIN.name()`; the self-demotion and
  last-admin rules at `:202-221` keep working as they do now. Role comparison moves from `!=`
  to `equals` (`:135`) — a reference comparison on strings would silently misbehave.
- `config/SecurityConfig.java:54-56` — add `/api/v1/role-configs/**` to the existing
  POST/PUT/DELETE `hasRole("ADMIN")` rules, so reads stay available to any authenticated
  user (the User Management dropdown needs them) while writes are admin-only.
- `config/DataInitializer.java` — seed the two built-in role configs before seeding users
  (guarded by `existsByCodeIgnoreCase`, so it is safe across restarts, matching
  `createBlogConfigIfMissing`); `createUserIfMissing` passes `Role.ADMIN.name()`.

## Frontend — new files

| File | Purpose |
|---|---|
| `api/roleConfig.ts` | `RoleConfigDTO` type + `list/create/update/remove`, shaped like `api/gameConfig.ts` |
| `stores/roleConfig.ts` | `roleConfigs`/`loading` refs, `fetchRoleConfigs`, add/update/remove, `assignableRoles` computed (enabled only, sorted) — module-scope refs, matching `stores/gameConfig.ts` |
| `views/RoleConfigView.vue` | `a-page-header` + `a-card` + `a-table`: code, name, description, user count, sort order, enabled switch, delete. Inline add form above the table. Built-in rows show a tag and have code/delete locked. |

## Frontend — modified files

- `router/index.ts:31` — add `settings/role-config` → `RoleConfigView`, `meta: { requiresAdmin: true }`.
- `layouts/AppMenu.vue:92-105` — third item in `settings-sub` after Game Config, `key="role-config"`, `TeamOutlined`.
- `layouts/MainLayout.vue:77` — `'role-config': '/settings/role-config'` in `keyToPath`.
- `locales/zh-CN.ts` / `locales/en-US.ts` — `menu.roleConfig` ("角色配置" / "Role Config") plus a `roleConfig` block for the page, following the existing `gameConfig` block.
- `api/adminUser.ts:4` — `Role` becomes `string` (a catalog code is no longer a closed set); `AdminUserDTO.role`, `AdminUserQuery.role` and both request types follow.
- `views/UserListView.vue:44` — replace the hard-coded `const roles: Role[] = ['USER', 'ADMIN']`
  with the catalog from the store, fetched `onMounted`. The filter and form dropdowns
  (`:358`, `:469`) render each role's configured `name` instead of `t('user.roles.' + r)`,
  falling back to the code. The role tag at `:398` keeps gold for `ADMIN`, default otherwise.
  The `user.roles` locale keys stay for the two built-ins but are no longer the only source.

## Verification

- `cd front && npm run typecheck` — must pass clean.
- `cd back/myapp && ./mvnw -q compile` — must pass clean.
- Report results honestly, including any failure output.

## Out of scope

Per-role permissions, menu-level access scoping, and replacing the `hasRole("ADMIN")` checks.
Custom roles are labels with no extra access until permissions are added.

