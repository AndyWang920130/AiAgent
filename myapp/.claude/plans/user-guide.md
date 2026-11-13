# User Guide section

Requested 2026-10-28: a User Guide section holding tutorials, with two to begin with —
how to follow other users, and how to invite players to a 70235 game, accept invitations
and play together. "Two tutorials for now" is the operative phrase, so the structure has
to make a third tutorial cheap.

## Shape

An index page plus one route per tutorial, which is how `tools` and `mini-game/card-games`
already work in this app:

| Route | Name | What it is |
| --- | --- | --- |
| `/guide` | `user-guide` | Card grid, one card per tutorial |
| `/guide/:slug` | `guide-tutorial` | The tutorial itself |

One view renders every tutorial from a data entry rather than each tutorial getting its own
`.vue` file. A new tutorial is then an entry in `guides.ts` plus its locale keys — no new
component, no router change, no menu change beyond an optional deep link. If some later
tutorial needs bespoke markup (a diagram, an embedded widget), it can be given its own
route at that point; nothing here blocks that.

Unknown slug redirects to `/guide` rather than showing a broken page.

## Files

**New — `front/src/guides.ts`**

The tutorial registry. Per entry: `slug`, locale-key prefix, icon, step count, and the
in-app links the tutorial should offer. No prose — all copy lives in the locale files.

```ts
export interface GuideLink { labelKey: string; path: string }
export interface GuideEntry {
  slug: string
  /** Locale key prefix under `guide.`, e.g. 'follow' → guide.followTitle. */
  key: string
  icon: Component
  /** How many step1Title/step1Body pairs this tutorial has. */
  steps: number
  /** Whether a `<prefix>Prereq` alert exists. */
  prereq?: boolean
  links: GuideLink[]
}
```

**New — `front/src/views/UserGuideView.vue`** — the index. `a-page-header` with title and
subtitle, then `a-row`/`a-col` (`:xs="24" :sm="12" :md="8" :lg="6"`) of hoverable `a-card`s
with an icon cover, exactly as `ToolsView.vue` and `CardGameListView.vue` do. Clicking a
card pushes `{ name: 'guide-tutorial', params: { slug } }`.

**New — `front/src/views/GuideTutorialView.vue`** — resolves `route.params.slug` against
the registry, redirects to `/guide` if absent. Layout:

- `a-page-header` with the tutorial title and a back arrow to `/guide`.
- `a-alert type="info" show-icon` for the intro.
- `a-alert type="warning" show-icon` for the prerequisite, when the entry declares one.
  Tutorial 2 needs this: you can only invite people who already follow you.
- `a-steps direction="vertical"` with `:current="-1"` so no step reads as "in progress" —
  this is a reference list, not a wizard. One `a-step` per step, `:title` from
  `<prefix>Step<n>Title`, description from `<prefix>Step<n>Body`.
- A row of `a-button`s from `links`, pushing straight to the feature being described, so a
  reader can act on the tutorial without hunting through the menu.
- `a-alert type="success"` for the closing tip where one exists.

The whole library is registered globally in `main.ts` (`app.use(Antd)`), so `a-steps` needs
no import.

**Edited — `front/src/router/index.ts`** — two entries in the flat `children` array, lazy
imports, placed next to the `tools` block:

```ts
{ path: 'guide', name: 'user-guide', component: () => import('../views/UserGuideView.vue') },
{ path: 'guide/:slug', name: 'guide-tutorial', component: () => import('../views/GuideTutorialView.vue') },
```

No `meta` — the parent `MainLayout` route already carries `requiresAuth`, and a guide is not
admin-only.

**Edited — `front/src/layouts/AppMenu.vue`** — a `guide-sub` sub-menu with a `ReadOutlined`
icon, carrying three items: the index, then a deep link to each tutorial. Visible to every
signed-in user (no `v-if="isAdmin"`). Labels come from `t('menu.*')` like every other entry.

**Edited — `front/src/layouts/MainLayout.vue`** — three `keyToPath` entries:

```ts
'user-guide': '/guide',
'guide-follow': '/guide/follow-users',
'guide-card-game': '/guide/invite-and-play-70235',
```

`resolveSelectedKey` already falls back to the longest matching prefix, so landing on
`/guide/:slug` from a card click highlights the right menu item without further work.

**Edited — `front/src/locales/en-US.ts` and `zh-CN.ts`** — a new `guide` block plus three
`menu` keys. Keys stay two levels deep (`guide.followStep1Title`), matching the existing
files, rather than nesting a third level. Roughly 45 keys per locale.

## Tutorial content

Every claim below was read out of the code, not assumed.

### 1 — How to follow other users (`follow-users`)

Five steps, no prerequisite.

1. Open **Personal** from the menu. Its follow card has a **Find users to follow…** search
   box (`PersonalView.vue`, `userApi.search`).
2. Type a name or login and wait a moment — the search is debounced 300ms. Pick a result to
   open that person's profile.
3. You can also reach a profile by clicking an author's name in the blog list, on a post, or
   on a comment (`BlogListView.vue:156`, `BlogDetailView.vue:111`,
   `BlogCommentSection.vue:16`).
4. On the profile, click **Follow**. It becomes **Following**, the follower count rises, and
   a confirmation appears (`UserProfileView.vue:131-140`).
5. Check it: the **Following** tab on your Personal page lists them, and their posts appear
   under Home → **Following**. Clicking **Following** on their profile unfollows.

Links: Personal, Home. Closing tip points at tutorial 2 — following is what makes someone
invitable to a game.

### 2 — Invite players to 70235 and play (`invite-and-play-70235`)

Eight steps, with a prerequisite alert.

Prerequisite: the invite list is **your followers**, not the people you follow.
`CardGameService.listOpponents` builds it from `followersOf(me)`, and `followsMe` guards the
invite itself, so someone has to follow you before you can seat them. Links to tutorial 1.

1. Open **Mini Game → Card Games → 70235**.
2. Under **New table**, set **CPU players** and pick people under **Invite followers**. The
   seat counter must land between 2 and 5 (`CardGame.MIN_PLAYERS`/`MAX_PLAYERS`); you are one
   of those seats.
3. Click **Create table**. Every invited human gets a notification that links straight back
   to the table. An unanswered table expires after 5 minutes
   (`app.card-game.invite-timeout-seconds: 300`).
4. As an invitee: open the bell notification, or the 70235 page. The **Invitations** card
   names the host, shows the seat count and a countdown. Click **Accept** or **Decline** —
   the host is notified either way.
5. As host: **Start** unlocks once every invitee has answered and at least two players are
   seated. Until then the page says so.
6. Playing a turn: click a card to select it, click it again to play, or select several and
   press **Play cards**. Holding the left mouse button and dragging across the hand selects a
   run of cards in one gesture. **Pass** declines only the play in front of you — if someone
   behind you raises it, the turn comes back round. A turn left idle for 30 seconds is played
   for you (`turn-timeout-seconds: 30`).
7. **Auto-play** hands your turns to the server; switch it off to take them back. Two timed-out
   turns in a row switch it on by themselves, and it stays on until switched off — per game,
   so the next deal starts with it off again.
8. When the game ends, final scores and placings appear on each seat, and **Leave** in the
   top-right corner returns you to the lobby.

Links: the 70235 table, Notifications. Closing tip: a CPU-only table needs no invitations —
it deals the moment you create it (`CardGameService.createTable`, the `humans.isEmpty()`
branch), which makes it the quick way to learn the game.

The step copy describes the flow and defers the card-beating rules to the rules banner
already on the table page, so the two don't drift apart.

## Verification

- `npx vue-tsc -b` must exit 0 — catches a registry entry whose icon or link shape is wrong.
- `npm run build` must exit 0.
- A script asserts that every key the registry implies (`<prefix>Title`, `Desc`, `Intro`,
  `Step1..N` × Title/Body, plus `Prereq`/`Tip` where declared) exists in both locale files
  and that neither file has keys the other lacks. A missing key renders as the raw key
  string, which neither the compiler nor the build would report.
- Manual: `/guide` lists two cards; both open; back arrow returns; an invented slug such as
  `/guide/nope` redirects to `/guide`; the menu highlights correctly on a deep link; both
  languages read correctly.

There is no test runner or browser automation in the frontend, so the type check, the build
and the key-parity script are the automated coverage available.

## Built 2026-10-28

Implemented as planned. Five quoted labels were wrong in the first draft of the copy and were
corrected against the locale files before the build:

- English step 6 said the button reads "Play cards"; `cardGame.playCards` is `'Play'`.
- English step 8 said "Leave"; `cardGame.leaveGame` is `'Leave game'`.
- zh step 1 named the menu path 「休闲游戏」→「桌面牌类」; the real labels are
  `menu.miniGame` 「小游戏」 and `menu.cardGames` 「纸牌游戏」.
- zh step 7 called the toggle 「托管」, which is the per-seat tag on `PlayerSeat`; the switch
  label `cardGame.autoPlay` is 「自动出牌」.
- zh step 1 of the follow tutorial quoted the search placeholder as 「查找要关注的用户…」;
  `userProfile.findUsers` is 「搜索用户以关注…」.

One claim was also wrong: step 4 said the Invitations card shows "how many are seated". The
tag renders `inv.players.length` through `cardGame.seatCount` (`'{count} seats'`), which is
every seat on the table including seats still pending, so both locales now say how many seats
the table has.

`canStart` was read directly (`CardGameService.java:1126-1131`) to confirm step 5: pending,
host, no seat left PENDING, and accepted seats within 2..5. The Home feed claim in the follow
tutorial was confirmed against `BlogResource.getFollowingBlogs`.

Verification run: the key-parity script reports 44 required keys present in both locales with
no orphans and no en/zh drift; `npx vue-tsc -b` exit 0; `npm run build` exit 0, emitting
`UserGuideView`, `GuideTutorialView` and a shared `guides` chunk. A second script replayed
`resolveSelectedKey` over the real 22-entry `keyToPath` map and confirmed `/guide`,
`/guide/follow-users`, `/guide/invite-and-play-70235` and an unknown slug all highlight the
intended menu item, with no existing route captured by the new `/guide` prefix.

Not verified: rendered appearance. There is no browser automation here, so the vertical
`a-steps` layout, the dark-theme look of the card covers (`#f5f5f5`, copied from `ToolsView`
and `CardGameListView`, which no view overrides for dark mode) and the mobile button stack
are unobserved. Landing directly on `/guide/<slug>` highlights the right menu item but does
not auto-expand `guide-sub`, since `openKeys` is seeded with `home-sub` only — existing
sub-menus behave the same way, so this was left alone.

## Out of scope

No backend change — nothing here is persisted and no new endpoint is needed. No screenshots:
images would need hosting and would go stale against the UI; the step text names the actual
button labels instead. No "mark as read" or progress tracking on tutorials.

## Deepened 2026-11-13

The follow tutorial went from five steps to eight. Old step 5 bundled the Home feed and the
Personal page follow lists into one sentence; it now covers the feed alone, and the lists,
the other person's view and unfollowing each get a step. `followDesc` on the index card was
widened to match. Only `guides.ts` (`steps: 5` → `8`) and the two locale files changed.

Three things were read out of the code before the copy was written, because each contradicts
what a reader would reasonably assume:

- **Following nobody notifies.** `UserProfileService` injects no `NotificationService`
  (its fields are the two repositories, `BlogLockRepository`, `BlogMapper` and
  `AchievementService`), and `AchievementType` holds only `REGISTRATION`,
  `PUBLISH_ARTICLE` and `RECEIVE_LIKE`. So a follow is silent: no notification, no
  achievement. Step 7 says so rather than leaving someone waiting for a reply.
- **Unfollow lives in exactly one place.** `userApi.unfollow` is called only from
  `UserProfileView.vue:64`, through the same button that follows. The Personal page lists
  render an avatar, name and `@login` per row and nothing else, so there is no unfollow
  there. Step 8 names the button as the only route and warns that it fires with no
  confirmation — `toggleFollow` calls the API straight away, no `a-popconfirm`.
- **Only your own follow counts are clickable, and the lists are yours alone.**
  `PersonalView.vue:282-288` wires `@click` on each count to `activeFollowTab`;
  `UserProfileView.vue:110-119` renders the same two counts as plain divs. The backend does
  expose `/{login}/followers` and `/{login}/following` for any login, but no view calls them
  for another user, so step 6 says another profile shows the counts without the lists.

One layout detail worth keeping: the counts are in the left `a-col` and the list card they
drive is in the right one, so on `lg` and up the click moves a card beside the numbers, and
below that breakpoint the columns stack and it moves a card underneath. Step 6 mentions both.

Labels were taken from the locale files, not from memory — the first build got five wrong.
Quoted this time: `userProfile.followers` 'Followers'/「粉丝」,
`userProfile.followingCount` 'Following'/「关注」 (the count label and the button label are
different keys that both read "Following" in English), `userProfile.following` 'Following'
/「已关注」, `userProfile.noFollowers`, `userProfile.noFollowing`, `home.recommended`
'🔥 Recommended'/「🔥 推荐」 and `home.following` '👥 Following'/「👥 关注」, emoji included
since that is what the tab shows.

Verification: `check_guide_keys.py` reports 50 required keys present in both locales, no
en/zh drift, no orphans — it already covered this change, so it ran unmodified.
`npx vue-tsc -b` exit 0. `npm run build` exit 0, emitting `UserGuideView`,
`GuideTutorialView` and the `guides` chunk. `git status` shows the three intended files and
nothing else. The 1,649 kB `index` chunk warning predates this change and is untouched by it;
these edits add only locale strings.

Not verified: rendered appearance, as before. Eight vertical `a-step`s with bodies this long
make a taller page than five did, and that has not been looked at in a browser.
