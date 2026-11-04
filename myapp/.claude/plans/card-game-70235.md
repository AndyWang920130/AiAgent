# Card game 70235 (七王五二三) — rules as supplied

The game is 七王五二三, also written 七鬼五二三 or 七毛五二三.
Published rules (for reference only): https://apps.apple.com/mx/app/%E4%B8%83%E7%8E%8B%E4%BA%94%E4%BA%8C%E4%B8%89/id1560054334

Rules below come from the user. Where the published version disagrees, the user's wins —
these are their house rules. TODO items are unanswered; do not implement anything resting
on one without asking.

## Deck and players

- 2 players: one pack, 54 cards (52 + 2 jokers).
- 3-5 players: two packs, 108 cards (54 x 2).
- Minimum 2 players, maximum 5.

## Deal

- 5 cards per player.
- The player who leads first gets 6.

## Card ranking — CONFIRMED

Highest to lowest: `7 > 0 > 2 > 3 > 5 > A > K > Q > J > 10 > 9 > 8 > 6 > 4`

- `0` is both jokers in the name of the game and in this ranking notation. Big Joker and
  Little Joker are equal — not distinguished. **On screen a joker is labelled "Joker"
  (zh 王), not `0`** — the `0` here is only how the ranking reads aloud.
- The name "70235" is literally this order. The user confirmed this over the published
  七王五二三 order (`7 > 鬼 > 5 > 2 > 3 > ...`), which puts 5 higher.
- TODO: does suit ever matter, or rank only?
- TODO: with two packs two identical cards can be played — does the first played hold?

## Legal plays

A turn plays one of:

- single
- pair
- full house: three of a kind + two extra cards (5 cards)
- four of a kind + one extra card (5 cards)

## Replenishing

- When a play goes unbeaten by every other player, the player draws from the deck back up
  to the target hand size.
- Target hand size is always 5.
- The trick winner draws first, then every other player draws back up to 5 as well —
  passing does not cost a player their draw.

## Beating

Singles — CONFIRMED by worked example. A lead single 9 can only be answered by a higher
single. Card count must match; a pair or bigger shape does not take a single.

The user enumerated the cards beating a 9 as `10 J Q K 5 3 2 0 7`, omitting `A`. They
confirmed this was a typo: `A` does beat a 9. So the cards beating a lead 9 are
`10 J Q K A 5 3 2 0 7` — every rank above 9 in the confirmed order, nothing else.

All shapes — CONFIRMED. A shape is only ever answered by the same shape with a higher
rank. Shapes never meet each other:

- pair → only a higher pair
- **three of a rank, on its own** → only a higher bare triple (CONFIRMED by the user
  2026-10-28)
- **four of a rank, on its own** → only a higher bare quad (same)
- 3+2 → only a higher 3+2, comparing the **triple's rank only** (the pair's rank is
  irrelevant to the comparison). The "two" must be a **real pair** — two cards of one
  rank. A triple beside two odd cards is not a play at all.
- 4+1 → only a higher 4+1 (comparing the quad's rank)
- **4+1 does NOT beat 3+2.** There is no hierarchy between shapes.
- A bare triple and a 3+2 are likewise **different shapes** and never meet, as are a bare
  quad and a 4+1. Six shapes in total: SINGLE, PAIR, TRIPLE, QUAD, FULL_HOUSE,
  QUAD_PLUS_ONE.

## Point cards and scoring — PARTIALLY CONFIRMED

The game is scored on captured point cards, not purely on emptying a hand first.

- Point cards and values — CONFIRMED: `5` = 5, `10` = 10, `K` = 10.
  100 points per 54-card pack, so 200 points in a two-pack (3-5 player) game.
- The winner of a turn collects every point card played during that turn.
- All other ranks, including the jokers, are worth nothing. Jokers are powerful (rank 0,
  second only to 7) but score zero.

## End of game — CONFIRMED

- The game ends when the deck is empty **and** a player has played their last card.
- Every point card still held by the other players is awarded to whoever emptied their
  hand first.
- Final scores are then totalled and the highest wins.

So emptying first is not an outright win — it is a large scoring bonus.

## Turn flow — CONFIRMED

- **A trick is not won by the first answer to it.** The turn goes round the table in seat
  order and every player may raise again, including the player who led. A lead of 8 can be
  met with a 9, then a 10, then a J, for as long as someone can raise. The trick belongs
  to the **last** player to raise, once the turn comes back round to them unanswered.
- **A pass declines only the play currently on the table, not the rest of the trick.**
  CONFIRMED by the user 2026-10-28, reversing the earlier reading recorded here. Worked
  example: A leads a 4, we pass, B raises to an 8, A raises to a 9 — the turn comes back
  to us and we may beat the 9 or pass again, and so on until the trick ends. A passer
  keeps their cards and does not lose their draw (see Replenishing).
- Consequence: `passedUsernames` is **display only**. It records who declined the current
  play, is cleared by any raise, and is never consulted when choosing the next actor.
- The winner of a turn **leads the next turn**.
- **Every player refills to 5 once the trick is scored**, including those who passed. Draw
  order is the turn winner first, then the others in seat order. Order matters once the deck
  is nearly empty — the winner gets priority on the last cards.
- Play continues as the deck drains. Once the deck is empty nobody refills, so hands
  shrink from then on.

### Why the game terminates

Refilling only ever takes cards out of the deck, so the deck drains monotonically. Once it
is empty nobody draws again and every play strictly shrinks the player's hand, so some hand
reaches zero.

Ending condition: deck empty **and** at least one player out of cards.

## Platform — CONFIRMED

Online, server-authoritative, following the existing Gomoku module:

- Player picks computer opponents, or invites players from their followers.
- Invitation flow mirrors Gomoku's.
- After players are selected, host clicks Start to initialize the game.

### Invite eligibility — CONFIRMED

One-way follow. A user may invite anyone who follows them, even if they do not follow
back. This deliberately differs from Gomoku, which requires a mutual follow
(`GomokuService.isMutualFollow`, rejected at `GomokuService.java:120`). Do not copy
Gomoku's mutual check — query followers of the host only.

### Table composition — CONFIRMED

A single game may mix humans and CPUs. Example: host + 2 invited friends + 1 CPU for a
4-player table. Before starting, the host picks both:

- how many CPU opponents to add, and
- which followers to invite.

Total players (host + invited humans + CPUs) must land within 2-5. Deck size follows from
the final count: 54 cards at exactly 2 players, 108 at 3-5.

## Existing code to follow

- `domain/GomokuGame.java` — one row models both invite and match; `@Version` for
  optimistic locking; board string is the authoritative state.
- `contants/enumeration/GomokuGameStatus.java` — PENDING/ACTIVE + terminal states.
- `service/GomokuService.java` — invite/accept/decline/cancel/resign/leave, lazy timeout
  resolution via `resolveTimeouts`, notifications through `NotificationService`.
- `web/rest/GomokuResource.java` — `/api/v1/gomoku`, polled by the client.
- `views/GomokuView.vue` (909 lines) — lobby + match screen in one view.
- `db/mysql/upgrade-gomoku.sql` — migration pattern.

Key difference from Gomoku: hands are hidden. The server must never send one player's
cards to another, so the DTO has to be built per-recipient.

## Already built (menu + list, done earlier)

- `views/CardGameListView.vue` — card grid, 70235 entry marked "Coming soon".
- Route `mini-game/card-games`, menu item under Mini Game, `keyToPath` entry.
- Locale keys `menu.cardGames` and the `cardGame` block in both locale files.

---

# Implementation plan

## Four rule details I am assuming

These were never answered. Each is called out so it can be corrected at approval time;
each is a small, localised change if wrong.

1. **Suit never matters, and ties do not beat.** Comparison is by rank only. With two
   packs, two identical cards can meet — the play already on the table holds, because
   beating requires *strictly* higher rank. This also settles "which of two identical
   cards wins": the earlier one.
2. **The "two" in 3+2 must be a real pair; the "one" in 4+1 is arbitrary.** CONFIRMED by
   the user 2026-10-28, correcting an earlier assumption here that both sets of extras
   were arbitrary. A triple plus two odd cards is not a play. The pair's *rank* still
   does not enter the comparison — only the triple's does — but it has to pair.
   (Five cards of one rank, possible with two packs, counts as 4+1.)
3. **The host leads the first trick and gets the 6th card.** The host is the player who
   set the table up and clicked Start, so this is the predictable reading. One line to
   change if it should be random instead.
4. **A pass declines one play, not the trick** — CONFIRMED by the user 2026-10-28,
   reversing what this item previously assumed (that a pass was final for the trick). A
   player who passes is asked again whenever the play in front of them is raised. The
   trick still ends when action returns to the player holding the best play, so
   termination is unchanged: a raise must be strictly higher, so the rank on the table
   only climbs, and a full circuit of passes ends the trick.

## Card encoding

Two characters per card, fixed width, so every string parses by chunking into pairs.

- Rank char: `4 6 8 9 T J Q K A 5 3 2 7` (`T` = 10), plus `X` for a joker.
- Suit char: `S H D C`; a joker uses `B` (big) or `L` (little) in the suit slot.
- Examples: `TS` = 10♠, `KH` = K♥, `XB` = big joker (displayed as "Joker" / 王, with a
  small B/L mark to tell the two jokers apart).

Strength order, weakest to strongest — index into this list is the comparison rank:

`4 6 8 9 T J Q K A 5 3 2 X 7`

So `7` is strongest (13), a joker is 12, `4` is weakest (0). Both jokers share rank 12,
as confirmed.

Point values: `5` → 5, `T` → 10, `K` → 10, everything else 0.

## Backend — new files

### 1. `contants/enumeration/CardGameStatus.java`

`PENDING, ACTIVE, FINISHED, CANCELLED, EXPIRED, ABANDONED, TIMED_OUT` — same vocabulary
as `GomokuGameStatus`, minus the two-player-only `DECLINED`/`RESIGNED` (a single invitee
declining does not kill a 5-seat table, so decline is tracked per player instead).

### 2. `domain/CardGame.java` → `twsny_card_game`

Extends `AbstractAuditingEntity`. One row = the table, its invitation phase, and the match.

| field | column | notes |
|---|---|---|
| `id` | `id` | identity |
| `status` | `status` varchar(20) | enum string |
| `hostUsername` | `host_username` varchar(100) | also `createdBy`; leads trick 1 |
| `deck` | `deck` varchar(255) | undealt cards, 2 chars each (108 cards = 216) |
| `currentPlay` | `current_play` varchar(64) | best play on the table, or null on a fresh trick |
| `currentPlayUsername` | `current_play_username` varchar(100) | who holds it |
| `currentShape` | `current_shape` varchar(16) | `SINGLE/PAIR/TRIPLE/QUAD/FULL_HOUSE/QUAD_PLUS_ONE` |
| `currentRank` | `current_rank` int | strength index of the play to beat |
| `trickPile` | `trick_pile` varchar(512) | every card played this trick, for point capture |
| `passedUsernames` | `passed_usernames` varchar(512) | comma-joined; who declined the *current* play. Display only — cleared by any raise, never consulted for turn order |
| `currentTurnUsername` | `current_turn_username` varchar(100) | whose action is due |
| `trickNumber` | `trick_number` int | for display and the trick cap |
| `firstOutUsername` | `first_out_username` varchar(100) | emptied first; collects leftover points |
| `winnerUsername` | `winner_username` varchar(100) | highest final score |
| `leftByUsername` | `left_by_username` varchar(100) | same role as Gomoku's |
| `startedDate`, `lastActionDate` | same | drive the game / per-turn clocks |
| `version` | `version` | `@Version` optimistic lock |

Indexes on `host_username` and `status`.

### 3. `domain/CardGamePlayer.java` → `twsny_card_game_player`

One row per seat. `@ManyToOne(fetch = LAZY)` back to `CardGame`.

| field | notes |
|---|---|
| `username` | real login, or `cpu-1`..`cpu-4` for a CPU (unique within the game) |
| `seat` | 0-based; fixes turn order and draw order |
| `hand` | the player's cards, 2 chars each. **Never leaves the server except to its owner.** |
| `score` | captured points so far |
| `cpu` | boolean |
| `invitationStatus` | `PENDING / ACCEPTED / DECLINED` — the host and CPUs are `ACCEPTED` at creation |

Unique constraint on `(game_id, username)`, index on `username` so "my active game"
is one query.

### 4. Repositories

- `CardGameRepository` — `findByStatus`, plus `@Query` joins through the player table for
  "games where X holds a seat" and a `default findActiveForUser(username)` mirroring
  `GomokuGameRepository.findActiveForUser`.
- `CardGamePlayerRepository` — `findByGameOrderBySeatAsc`, `findByGameAndUsername`,
  `findByUsernameAndInvitationStatus` (for the incoming-invite list).

### 5. `service/CardDeck.java` — pure rules engine, no Spring, no DB

Keeping the rules out of the service makes them unit-testable and keeps the service about
lifecycle. Static methods only:

- `buildDeck(int playerCount)` — 54 cards at 2 players, 108 at 3-5, shuffled.
- `rankOf(card)`, `pointsOf(card)`, `pointsOf(cards)`.
- `detectShape(List<String> cards)` → `{shape, rank}` or empty if the selection is not a
  legal play. Rules: 1 card → SINGLE; 2, 3 or 4 cards **all of one rank** → PAIR, TRIPLE
  or QUAD; 5 cards containing 4+ of a rank → QUAD_PLUS_ONE at the quad's rank (checked
  first, so five of a rank is 4+1); 5 cards that are a triple **plus a pair** →
  FULL_HOUSE at the triple's rank. A triple plus two odd cards is illegal, as is any
  2-4 card selection of mixed ranks. Anything else is illegal.
- `beats(candidateShape, candidateRank, currentShape, currentRank)` — same shape and
  strictly higher rank. No cross-shape hierarchy, so 4+1 never takes 3+2.
- `chooseLead(hand)` / `chooseBeat(hand, shape, rank)` — the CPU brain (below), also
  reused for timeout auto-play.

### 6. `service/CardGameService.java` — lifecycle, server-authoritative

Config, following the Gomoku precedent exactly:

```java
@Value("${app.card-game.invite-timeout-seconds:300}")
@Value("${app.card-game.turn-timeout-seconds:120}")
@Value("${app.card-game.game-timeout-seconds:3600}")
```

**Followers, not mutual follows.** `listInvitable()` uses
`userFollowRepository.findByFollowingUsernameOrderByFollowedDateDesc(me, unpaged())` and
maps `getFollowerUsername()` — the one-way rule, and the deliberate divergence from
`GomokuService.mutualFollows`. Invite validation checks
`existsByFollowerUsernameAndFollowingUsername(invitee, me)` only.

**Table creation** — `createTable(cpuCount, List<String> invitees)`:
- rejects self-invite, unknown user, a non-follower, a duplicate invitee, an invitee
  already in a live game, and a total seat count outside 2-5;
- rejects if the host already has a live game (`alreadyplaying`, as Gomoku does);
- seats the host at 0, invitees next, CPUs last;
- if there are no human invitees the table deals and goes ACTIVE immediately;
- otherwise stays PENDING and notifies each invitee via `NotificationService.notify(...,
  NotificationType.INFO, ..., CARD_GAME_LINK)`.

**Where notifications lead** — `CARD_GAME_LINK = "/mini-game/card-games/70235"`, shared by
all five notification sites (invite, accept, decline, cancel, leave). Every one of them
concerns a specific table, so they open the table itself rather than the card-game list:
the list page cannot show an invitation, so landing there leaves the reader one more click
from the thing they were told about. Gomoku's `GOMOKU_LINK` points at its game view the
same way. Accepting from the table view then lands the player in that table's own
pending lobby, which is what the link now opens onto.

**Invite responses** — `accept(id)` / `decline(id)` flip that player's
`invitationStatus` and notify the host. A decline frees the seat (the host can start
without them, or cancel). `cancel(id)` is host-only and notifies everyone.

**Start** — `start(id)`: host only, PENDING only, requires no invitee still `PENDING` and
a final seat count of 2-5 after dropping decliners. Builds the deck for the final count,
deals 5 to everyone and **6 to the host**, sets `currentTurnUsername` = host, clears the
trick, sets `startedDate`/`lastActionDate`, status ACTIVE.

**Play** — `play(id, List<String> cards)`:
1. `resolveTimeouts` then `runAutomaticTurns` on entry, as every path does;
2. must be ACTIVE and the caller's turn, else `notyourturn`;
3. the cards must all be in the caller's hand (removed by value, counting duplicates —
   two packs mean a hand can legitimately hold two `TS`);
4. `detectShape` must succeed, else `illegalshape`;
5. on a lead, any legal shape is allowed; otherwise `beats(...)` must hold, else
   `cannotbeat`;
6. remove from hand, append to `trickPile`, set the play as current, **clear
   `passedUsernames`** (a raise re-opens the question for everyone), advance the turn.

**Pass** — `pass(id)`: illegal when leading (`mustlead`); otherwise adds the caller to
`passedUsernames` and advances. The caller is not out of the trick — any later raise
brings the turn back to them.

**Turn advance and trick resolution** — the heart of it:
- the next actor is the next seat, in order, that still has cards. Having passed is *not*
  a reason to skip a seat: a pass declined one play, and a raise replaces that play;
- if that search comes back to `currentPlayUsername`, the trick is over:
  - they take `pointsOf(trickPile)` into their `score`;
  - they lead the next trick (`currentTurnUsername` = winner), trick state cleared,
    `trickNumber++`;
  - **refill**: the winner draws first, then every other seat in seat order, each back up
    to 5 cards, while the deck lasts. Passing costs you the trick, not the draw.
- end check after the refill: if the deck is empty **and** some hand is empty, finish.

**Finish** — `firstOutUsername` is the first player to reach an empty hand (recorded the
moment it happens, so the award is unambiguous even if two empty in the same trick). Every
point card left in every other hand is added to that player's score. Highest score wins;
`winnerUsername` is set, status FINISHED. A tie records the first-out player as winner,
since emptying first is the tiebreak the rules already privilege.

**Leave** — `leave(id)` mirrors Gomoku: an ACTIVE game becomes ABANDONED, `leftByUsername`
is stamped, and everyone else is notified. With hidden hands there is no way to continue
fairly once a seat vanishes mid-trick, so ending the table is the honest behaviour.

### Automatic turns — CPU seats and auto-play

There is no scheduler in this codebase, so CPUs move the same way timeouts resolve —
lazily. `runAutomaticTurns(game)` loops while the game is ACTIVE and `currentTurnUsername`
is an automatic seat, playing or passing through the same internal helpers a human action
uses, and stops when a seat playing its own turns is due or the game ends. It is called on
every read and every action, so by the time any response is built the automatic seats have
caught up. Capped at `MAX_AUTO_STEPS` per call as a safety net against a logic bug spinning
forever.

**Auto-play** (requested 2026-10-28) is a human seat the server plays. `isAutomatic(seat)`
is `seat.isCpu() || seat.isAutoPlay()`, and that one condition is the whole engine change:
`act()` was already shared by CPU seats and humans whose turn clock had lapsed, so a seat
on auto-play plays exactly as a CPU in that position would.

- **Server-side, not a client loop.** The strategy already exists on the server, so a
  TypeScript copy of it could disagree with the authority; and a browser playing for you
  stops when the tab closes, which is most of what auto-play is for.
- **Per seat and per game** (`twsny_card_game_player.auto_play`), so it lapses with the
  table rather than following the player to the next one.
- **`setAutoPlay` writes the flag before advancing**, which is what makes the switch work
  in both directions. Switching on while the turn is already yours plays that turn in the
  same response — waiting for the next poll would show a table that has not moved to
  someone who just asked for it to be played. Switching off on your own turn keeps that
  turn, which only holds because the write precedes the `advance()`; with the order
  reversed the player asks for control back and watches a card go down anyway. That was a
  real defect in the first cut, caught by `CardGameAutoPlayVerificationIT`.
- **A finished table is a no-op, not an error** — same treatment as a late play or pass,
  since the caller cannot affect a state that has already closed.
- **It only moves when something reads the table.** Turns resolve inside requests, so an
  auto-play seat advances on someone's poll rather than on its own. A consequence of the
  existing lazy design, not something auto-play introduces. Its visible edge: on a table
  whose only human switches auto-play on, every seat is now automatic and the game plays
  itself out to a winner before that one call returns.
- **Reported for every seat, not just its owner**: it explains a seat answering instantly
  and, unlike a hand, is not private. The client reads its own state off its own seat, so
  `CardGameDTO` needs no separate `myAutoPlay`.

Strategy, deliberately simple and readable:
- **Leading**: play the largest legal shape at its lowest rank — shed cards while holding
  the high ranks that win tricks. Hands refill to 5 anyway, so dumping is cheap.
- **Responding**: play the cheapest legal beat; pass when there is none. Because a pass
  only declines the current play, a CPU that passed is asked again after a raise and will
  answer it if the new play happens to be one it can beat.
- `legalPlays` offers every shape a rank group supports, so four of a rank yields a pair,
  a triple and a quad — the smallest shape is often the only one that answers the table.
  Five-card shapes are padded from a *different* rank: a kicker for 4+1, a real pair for
  3+2, which means a triple with only odd cards beside it has no 3+2 available. Each
  assembled five-card play is run back through `detectShape` rather than trusted.

This is a competent beginner, not a solver. Noted as the intended level, not a limitation
to fix later.

### Timeouts (lazy, `resolveTimeouts`, exactly Gomoku's shape)

- PENDING past `invite-timeout-seconds` → EXPIRED.
- ACTIVE past `game-timeout-seconds` → FINISHED, scored as it stands (there are real
  scores on the table, so a draw would discard information).
- A player past `turn-timeout-seconds` auto-passes, or auto-leads via `chooseLead` when
  passing is illegal. The game keeps moving rather than one idle player freezing five.

### 7. `service/dto/` — per-recipient, hands never leak

- `CardGameDTO` (record) — `id, status, hostUsername, deckCount, trickNumber,
  currentShape, currentPlay, currentPlayUsername, currentPlayName, trickPoints,
  currentTurnUsername, players, myHand, firstOutUsername, winnerUsername, leftByUsername,
  turnTimeoutSeconds, turnSecondsRemaining, gameSecondsRemaining, inviteSecondsRemaining,
  canStart, createdDate`.
- `CardGamePlayerDTO` — `username, name, avatar, seat, cpu, autoPlay, invitationStatus,
  handCount, score, passed, out`. **No `hand` field exists on this record at all**, so there is no
  code path that could serialise another player's cards — the omission is structural, not
  a filter that could be forgotten.
- `myHand` carries only the recipient's own cards, sorted by strength. `toDto(game, me)`
  takes the viewer, as `GomokuService.toDto` does.
- `CardGameInvitesDTO` — `incoming`, `outgoing`.
- `CardGameOpponentDTO` — `username, name, avatar` (followers available to invite).

All records, matching `GomokuGameDTO`; no MapStruct mapper, since the Gomoku DTOs are
hand-built in the service.

### 8. `web/rest/CardGameResource.java` — `/api/v1/card-games`

| method | path | purpose |
|---|---|---|
| GET | `/opponents` | followers the caller may invite |
| POST | `/tables` | create a table: `{cpuCount, invitees[]}` |
| GET | `/invites` | incoming + outgoing, polled by the lobby |
| POST | `/games/{id}/accept` | invitee accepts |
| POST | `/games/{id}/decline` | invitee declines |
| POST | `/games/{id}/cancel` | host withdraws the table |
| POST | `/games/{id}/start` | host deals and begins |
| POST | `/games/{id}/plays` | `{cards: ["TS","TH"]}` |
| POST | `/games/{id}/pass` | pass this trick |
| POST | `/games/{id}/auto-play` | `{enabled}` — hand your own turns to the server, or take them back |
| POST | `/games/{id}/leave` | back to lobby, ends an active table |
| GET | `/games/{id}` | full state **for the caller**, polled during play |
| GET | `/games/active` | resume on load; 204 when none |

Request records nested in the resource (`TableRequest`, `PlayRequest`, `AutoPlayRequest`) with
`@NotNull`/`@Valid`, as `GomokuResource` does. No `@PreAuthorize` — authentication is
global and every method is already scoped to the caller.

## Backend — modified files

- `src/main/resources/config/application.yaml` — add an `app.card-game` block beside
  `app.gomoku`, same three keys with comments.

## Database — `db/mysql/upgrade-card-game.sql`

Two `CREATE TABLE IF NOT EXISTS` statements with the full column set, matching indexes,
`ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`, the audit columns
exactly as `upgrade-gomoku.sql` declares them, and `AUTO_INCREMENT = 1000` on both. A
foreign key from `twsny_card_game_player.game_id` to `twsny_card_game.id` with
`ON DELETE CASCADE`. No idempotent `ALTER` section — both tables are new, so there is no
deployed schema to patch.

Columns added after that first release are appended to the same file as `ALTER TABLE`
statements, most recently `twsny_card_game_player.auto_play bit(1) NOT NULL DEFAULT b'0'`.
They error harmlessly (`1060`, duplicate column) once applied. Only **prod** needs this
file run: `dev` is H2 with `create-drop` and `test` is MySQL with `ddl-auto: update`, so
both pick new columns up on their own.

## Frontend — new files

### `src/api/cardGame.ts`

Flat interfaces + a `cardGameApi` object, shaped like `api/gomoku.ts`, including the 204
handling that file already establishes:

```ts
getActiveGame: () => http.get('/api/v1/card-games/games/active')
  .then(r => (r.status === 204 ? null : (r.data as CardGameState)))
```

### Components, so the view does not become another 909-line file

- `src/components/cardgame/PlayingCard.vue` — one card from its 2-char code: rank glyph
  (localised "Joker" for a joker, `10` for `T`), suit colour, selected/disabled states, a
  point-card marker. Sized off a CSS custom property so the table can shrink it on a phone.
- `src/components/cardgame/CardHand.vue` — the caller's hand, emitting a click per card by
  index (not by code: with two packs a hand can hold two identical cards, which must be
  selectable independently). Overlapping cards on narrow screens.
- `src/components/cardgame/PlayerSeat.vue` — avatar, name, CPU tag, card count, score,
  turn / passed / out badges, and that seat's latest action in the current trick shown
  beneath the seat, so a play reads as belonging to the player who made it. Also the seat's
  `Auto` tag while auto-play is on, and once the game is over its placing badge, a trophy on
  the winner and a first-out tag — the final result reads off the seats rather than a
  separate standings list. A fixed-size box during play, since these sit in a row above the
  trick area and anything that resizes a seat moves the middle of the table with it. The
  `self` prop marks the viewer's own seat, which collapses to a single line on a phone.

### `src/views/CardGame70235View.vue`

Lobby and table in one view, as `GomokuView.vue` does, but with the heavy lifting in the
three components above.

- **Lobby**: a CPU-count stepper, the follower list with invite buttons, the pending-seat
  list with each invitee's status, incoming invites with accept/decline, a seat counter
  showing the 2-5 bound, and Start (enabled when `canStart`). Polls `/invites` and
  `/games/active` every 2s, as Gomoku's lobby does.
- **Table**: opponents' seats around a trick area showing the current play and its
  holder, the caller's hand below, and Play / Pass buttons. Play is enabled only when the
  selection forms a legal shape that beats the current play — the same checks the server
  enforces, duplicated client-side purely so the button can disable itself. The server
  stays the authority. Polls `/games/{id}` every 1500ms.
- **Playing a selection** (CONFIRMED by the user 2026-10-28): the first click on a card
  selects it and a second click on an already-selected card plays the selection. The
  re-click is what commits, which makes leading and following behave identically — a
  single is two clicks on one card, a pair is a click on each and then either again.
  Waiting for that second click is what keeps a lead unambiguous: a selected 9 could be a
  single or the start of a pair, and a first click cannot say which was meant. Re-clicking
  a selection the rules would reject deselects that card instead of playing, so a wrong
  pick is still undone by clicking it off. The Play button stays as an alternative (and is
  the only way to commit for anyone who would rather not double-click); Clear remains the
  way to shrink a selection that is already legal, since a re-click there commits it.
- **Where played cards appear**: under the seat that played them, from `trickHistory`
  joined to `players` on `username` — each seat shows its own latest entry for the trick.
  A pass shows as a dimmed "Passed" marker rather than an empty row, so a seat that has
  acted is distinguishable from one that has not. The centre trick area keeps the full
  ordered history as well, because per-seat rows cannot show sequence: when one seat raises
  twice in a trick, only the ordered list shows what it answered.
- **Scores**: a per-player panel, and the final result shown **on the table itself**
  (requested 2026-10-28, replacing the separate result card the first version had).
- **Where the result goes.** The first version put a `.result-card` with an `<ol>` of
  standings above the hand. It is now on the table: the trick area switches to result mode
  and each seat carries its own outcome. The reason is that the standings list duplicated
  the seats — same players, same scores, same CPU and first-out tags, in a second place —
  and the seats are where the player has been reading those players all game. One layout
  to learn instead of two, and nothing moves position at the moment the game ends.
  - **The trick area** (`.trick.is-result`, the same `#fffbe6` as a winning seat) carries
    the headline, because it is the one region whose job is over: there is no current play
    to show once the game is finished. It holds the trophy headline, the `firstOutWas`
    line, and a `resultOnSeats` pointer telling the player the numbers are on the seats.
  - **The headline** reads the winner from `winnerUsername` rather than re-deriving it —
    `finish()` breaks a score tie by who went out first, so the winner is not always the
    top score. First-person (`youWinWith`) when the winner is the viewer, otherwise
    `winnerIs`, falling back to `statusText` when there is no winner at all: `leave()` and
    `cancel()` reach ABANDONED/CANCELLED without calling `finish()`, so `winnerUsername`
    stays null there and only the scores are meaningful.
  - **Each seat** gains a placing badge, a trophy on the winner and a first-out tag, on top
    of the score it already showed. `PlayerSeat` takes `place`, `winner` and `firstOut` as
    props rather than computing them, because placing is a property of the field and not of
    one seat.
  - **The placings still come from `standings`**, which is kept for exactly that: equal
    scores share a placing and the next one skips accordingly (1, 2, 2, 4), so a tie is not
    rendered as an order the scores do not support. The view reduces it to a
    `placeByUsername` map and the seats read their own number out of it.
- **Auto-play** (requested 2026-10-28): a switch under the hand, with the hand and the
  Play / Pass / Clear buttons disabled while it is on. A mode rather than an action, so it
  sits with the hand it governs. The buttons go dead because the server takes the turn on
  the next read — a click would be racing something it cannot beat — but the hand stays
  visible so the player can watch it being played. The per-seat `Auto` tag is suppressed on
  CPU seats, which play themselves regardless and would otherwise all wear it.
- **Leave, top-right** (requested 2026-10-28): out of the hand-actions row and into the
  outer card's `#extra` slot, which is where this codebase puts per-page actions
  (`StockListView`, `UserListView`, `NotificationView`, `BlogListView`, `HomeView`). The
  same corner carries "Back to lobby" once the game is over, so the way off the table is
  one place in both states instead of moving when the game ends.
- Local state in module-scope `ref()`s inside the view, no Pinia — the pattern this
  codebase uses, and `GomokuView.vue` keeps its game state in the view rather than a
  store, so a `stores/cardGame.ts` would be a departure with nothing to gain.

### The table is a fixed box (requested 2026-10-28)

The first version sized every band by its content, and the table moved constantly as a
result: a seat grew when it picked up a tag or played its first card, the trick area grew
with the owner line and each history row, and hints came and went under the hand. All of
that shifted the middle of the table while the player was reading it and clicking cards in
it. Four separate causes, three of them structural rather than just missing dimensions:

- `.seat-played` was `v-if="played"`, so it appeared at full height the moment a seat acted
  and vanished when the trick cleared — every seat at the table resizing several times a
  trick. Now rendered for the whole game, empty or not.
- `.trick` had `min-height`, and with `justify-content: center` a new history row nudged the
  current play upward. Now a fixed height, with the play in its own reserved `.trick-play`
  band and the history in a fixed band below it, stacked from the top rather than centred as
  a group.
- `.seat-list` was `flex-wrap: wrap` over content-sized seats, so seats re-wrapped as names
  and tags changed width. At the table it is now one non-wrapping row of fixed-width seats;
  the lobby keeps wrapping, having no trick area underneath to protect.
- The `clickAgainToPlay` hint and the turn-clock tag appeared and disappeared with every turn
  change, moving the buttons under the cursor reaching for them. Both now hold their space.

Sizes live in custom properties — `--seat-width`, `--seat-main-height`,
`--seat-played-height` on `PlayerSeat`, `--table-width`, `--seat-band-height`,
`--trick-height` on `.table-area` — so the view and the component agree on one set of
numbers, and each band's height budget is written out in a comment beside it (desktop:
300px trick = 40px padding + 28 head + 110 play + 98 history + two 12px gaps). Content that
overruns a box clips rather than resizing it: losing the tail of an unusually long name is
better than the whole table shifting.

### Fitting a phone screen (requested 2026-10-28)

The table has to fit a phone with no page scroll, and the arithmetic does not allow it by
shrinking alone. This view's chrome is 150px at that breakpoint, which leaves roughly 350px
on a small Android and 530px on a tall phone, against the ~840px the desktop bands need —
so more than half has to come out, and about 30% of that is available from smaller cards and
tighter padding. The rest comes out of what is on screen. **The user chose to keep every
piece of game state and drop explanatory text** (2026-10-28), so:

- **The relationship inverts.** Desktop fixes every band and lets the table be as tall as it
  needs; the phone gives the table `calc(100dvh - 150px)` and lets the trick area absorb
  whatever the fixed bands leave, with a 110px floor. The centre still does not move for a
  given device — everything around it is fixed — it is just measured from the viewport
  instead of from a constant. `dvh` with a `vh` fallback, for a collapsing address bar.
- **Cards shrink** (48x68 → 38x54, small 38x54 → 30x42), which is where most of the height
  comes from: the hand, the trick and the row under each seat are all sized off those two
  custom properties.
- **Your own seat collapses to one line** (`self` prop on `PlayerSeat`), dropping its played
  row. Those are the cards you just put down out of the hand immediately below it, so it is
  the cheapest duplicate on the table to give up.
- **The trick history and both hint paragraphs are dropped.** The history is the dimmed trail
  of plays already superseded and the only band that grows with the game; the hints are
  explanation rather than game state. Every score, count, tag and the current play stay.
- **The buttons and the auto-play switch share one row**, two rows on a desktop where the
  switch reads better under the actions it governs.
- **The view claws back the layout's frame** with a negative margin — `.app-content`
  contributes 24px of vertical margin and padding at this width, which a table fitting a
  640px screen cannot spare. Done here rather than in `MainLayout.vue`, which would affect
  every page in the app for the sake of this one.
- **A second breakpoint at `max-height: 600px`** drops the played-card row under the
  opponents too. Everything above still leaves the trick area below its floor on a ~640px
  device, and this is the same trade your own seat already makes. Keyed on viewport height,
  since that is the actual constraint.
- **Below about 568px of device height it scrolls rather than clips.** `overflow-y: auto` on
  the table, so nothing scrolls on the sizes budgeted for, and a first-generation SE gets a
  short scroll instead of a Play button cut off below the fold.

## Frontend — modified files

- `src/views/CardGameListView.vue` — set `routeName: 'card-game-70235'` on the 70235
  entry; the "coming soon" tag disappears on its own, since it renders off
  `routeName === null`.
- `src/router/index.ts` — add `mini-game/card-games/70235` → `CardGame70235View.vue`,
  named `card-game-70235`. No `requiresAdmin`.
- `src/i18n/locales/zh-CN.ts` and `en-US.ts` — extend the existing `cardGame` block with
  the lobby, table, shape names, status text, and error messages. Server messages for
  invalid plays are surfaced through the existing
  `err?.response?.data?.message || t(...)` fallback.

No menu or `keyToPath` change: the game opens from the Card Games list page, which is
already wired.

## Verification

- `mvnw.cmd -q compile` for the backend.
- `npm run build` (Vue tsc + Vite) for the frontend — the typecheck that caught the
  earlier `Role` union problem.
- A unit test for the rules engine, `CardDeckTest.java`, since the rules are where the
  bugs will be and they are pure functions: deck sizes per player count, the full
  ranking, every shape detected and rejected, `beats` across all shape pairs, the
  confirmed "lead 9 is beaten by exactly `10 J Q K A 5 3 2 0 7`" example, and the point
  totals (100 per pack / 200 for two).
- `@SpringBootTest` ITs for the behaviour that only exists once state is persisted, each
  rigging a table through the repositories and then driving the service:
  `CardGameShapeVerificationIT`, `CardGameTurnOrderVerificationIT`,
  `CardGameRefillVerificationIT`, `CardGameResultVerificationIT` (winner, first-out,
  scores, and the notification link) and `CardGameAutoPlayVerificationIT`. The pom
  configures no surefire includes for `*IT.java`, so these do **not** run under plain
  `mvn test` — they run explicitly via `-Dtest=`. Deliberately left that way.
- Auto-play specifically needs the turn clock ruled out, since a timed-out human already
  reaches the same `act()`. `CardGameAutoPlayVerificationIT` stamps `lastActionDate` to now
  everywhere and proves the difference with a control: two identically rigged tables, one
  seat armed, only the armed one moves on a read. Tests that need to observe a half-played
  table set the flag through the repository rather than the service, because going through
  the service advances the table and on a one-human table that runs the game to its end.
- Manual pass on the running app: host + 1 CPU through to a finish, then a 4-seat table
  mixing an invited follower with CPUs.

## Order of work

1. Enum, entities, repositories, migration SQL.
2. `CardDeck` + `CardDeckTest` — rules proven before anything depends on them.
3. `CardGameService`: invite/accept/decline/cancel/start.
4. `CardGameService`: play/pass, trick resolution, refill, finish.
5. CPU turns and timeouts.
6. DTOs + `CardGameResource`, then `compile`.
7. `api/cardGame.ts`, the three components, the view, router, locales, list-page flip.
8. `npm run build`, then the manual pass.

## Out of scope

Not building these, and not stubbing them either: rematch (Gomoku's rematch flow is
two-player and does not transfer to a 5-seat table), spectators, chat, persistent
cross-game statistics or achievement hooks, and animation beyond CSS transitions.

