# 70235: results on the table, auto-play mode, leave button in the corner

Three changes to the 70235 game. (1) and (3) are presentation only. (2) needs a new column,
an endpoint and a change to the turn-advance loop.

## 1. Final results on the table, not in a separate panel

Today a `.result-card` is rendered at the top of `.table-area` when the game is over, pushing
the seats, the trick area and the hand down the page. It reads as a screen in front of the
table, and it duplicates in a list (`<ol class="standings">`) the scores the seats are already
showing.

Instead, put the result where the game was being played:

- **The centre of the table carries the outcome.** The `.trick` area is empty once the game is
  over — it shows "Waiting for the lead", which is wrong at that point. When `gameOver`, that
  area renders the trophy, the winner headline (`youWinWith` / `winnerIs`, falling back to
  `statusText` for an abandoned or cancelled game with no winner) and the `firstOutWas` line.
  The trick number / deck count / trick points header is hidden, having nothing left to say.
- **Each seat carries its own final result.** `PlayerSeat` already shows a score; it gains a
  placing and a winner marker, so the standings are read off the table itself in seat order.
- **The separate card and the `<ol>` standings list are deleted**, along with `.result-card`,
  `.result-headline`, `.standings`, `.standing-*` styles. `.result-trophy`, `.result-winner`
  and `.result-firstout` move into the trick area's styles.

The `standings` computed stays — it is what assigns the placings, including the tie handling
(equal scores share a placing, 1, 2, 2, 4). A new `placeByUsername` map derived from it is what
the seats read.

### `front/src/components/cardgame/PlayerSeat.vue`

Three new optional props:

- `place?: number | null` — final placing, rendered as a small badge before the name. Null
  during play, so nothing changes mid-game.
- `winner?: boolean` — the seat the server named as winner. Gets a trophy next to the name and
  a highlighted background (reusing the `#fffbe6` the deleted `.standings li.won` used).
- `firstOut?: boolean` — a green `firstOut` tag, the marker the deleted list carried.

The existing `out` tag is already suppressed once the game is over (the server only sets `out`
while the game is ACTIVE), so there is no collision with the placing badge.

## 2. Auto-play mode

**Server-side, per seat, per game** — a `CardGamePlayer.autoPlay` flag, not a client-side
loop. Two reasons: the strategy already exists on the server (`CardDeck.chooseLead` /
`chooseBeat`, driving both CPU seats and timed-out humans), so a client-side version would be a
second copy of the rules in TypeScript that could disagree with the authority; and a browser
playing for you stops the moment the tab closes, which is most of what anyone wants auto-play
for. The flag lives on the seat row, so it is per game and does not leak into the next table.

### Backend

- **`domain/CardGamePlayer.java`** — `@Column(name = "auto_play", nullable = false) private
  boolean autoPlay = false;` plus the one-line accessors this class uses (no Lombok here).
- **`service/CardGameService.java`**:
  - `setAutoPlay(Long id, boolean enabled)` — participant only (`requireParticipant` +
    `seatOf`, so a caller can only ever set their own seat). On a terminal game it is a no-op
    returning the current state rather than an error, matching how `play`/`pass` treat a table
    that has already moved on. Ends with `advance(...)`, so switching it on during your own
    turn plays that turn immediately instead of waiting for the next poll.
  - `private boolean isAutomatic(CardGamePlayer seat)` → `seat.isCpu() || seat.isAutoPlay()`.
  - `runCpuTurns` → **`runAutomaticTurns`**, with its break condition changing from
    `!seat.isCpu()` to `!isAutomatic(seat)`. This is the whole of the feature: `act()` is
    already shared by CPU seats and timed-out humans, so an auto-play seat takes its turn
    through exactly the same path. `MAX_CPU_STEPS` → `MAX_AUTO_STEPS` (same value, 500; it
    already bounds the loop if every seat at a table is automatic and the game runs to its end
    inside one request).
  - `toDto` passes the flag through.
- **`service/dto/CardGamePlayerDTO.java`** — `boolean autoPlay`. Visible to everyone at the
  table on purpose: it explains why a seat is answering instantly, and it is not private
  information in the way a hand is. The client reads its own state from its own seat, so no
  separate `myAutoPlay` field on `CardGameDTO`.
- **`web/rest/CardGameResource.java`** — `POST /games/{id}/auto-play` with
  `record AutoPlayRequest(@NotNull Boolean enabled)`, alongside the existing action endpoints.
- **`db/mysql/upgrade-card-game.sql`** — appended in the same style as the `trick_log` columns,
  with the same note that MySQL has no `ADD COLUMN IF NOT EXISTS` so it errors harmlessly
  (1060) once applied:
  `ALTER TABLE twsny_card_game_player ADD COLUMN auto_play bit(1) NOT NULL DEFAULT b'0' AFTER cpu;`
  Only prod needs it (`ddl-auto: none`); dev is `create-drop` and test is `update`.

### Frontend

- **`api/cardGame.ts`** — `autoPlay: boolean` on `CardGamePlayer`; `setAutoPlay(id, enabled)`.
- **The toggle** goes in the hand-actions row as an `a-switch` with a label, not in the corner
  with the leave button: it is a mode that stays on, not an action. While it is on, the hand
  and the Play / Pass / Clear buttons are disabled, because the server will have taken the turn
  before any click could land — leaving them live would offer a control that loses a race it
  cannot win. The hand stays visible so the player can watch it being played.
- **A hint** under the switch while it is on, saying turns are being taken automatically and
  that switching it off returns control at the next turn.
- **`PlayerSeat.vue`** — an `autoPlay` tag in the seat meta row, so the flag is legible to the
  other players as well as its owner.

Switching it off takes effect at that seat's next turn; nothing is rolled back, and the
existing turn clock remains the fallback for a player who leaves it off and goes idle.

One property worth stating rather than fixing: like the CPU seats, an auto-play seat only moves
when some client polls the table, since this application resolves turns lazily on read and has
no scheduler. If everybody closes their browser the table stops until someone opens it again.
That is the existing design (`advance` on every read path), not something auto-play introduces.

## 3. Leave game in the top-right corner

An `#extra` slot on the view's outer `a-card`, which is where the rest of this codebase puts
per-page actions (`StockListView`, `UserListView`, `NotificationView` and others).

- While the game is ACTIVE: the danger **Leave game** button, moved out of the hand-actions row.
- Once it is over: the primary **Back to lobby** button (`closeResult`), which needs a new home
  anyway — change (1) deletes the card it currently sits in. The corner is then consistently
  the way out of the table in both states.

The waiting-table block keeps its own Cancel table / Leave table buttons where they are. Those
are part of the pre-game flow, and the request was about the in-game Leave button.

## Tests

- **New `back/myapp/src/test/java/com/example/myapp/CardGameAutoPlayVerificationIT.java`**:
  - A human seat with auto-play on has its turn taken on a plain read (`getGame`), with the
    turn clock nowhere near lapsing — this is what separates auto-play from the timeout path
    that already existed.
  - Switching it on while it is already your turn plays that turn in the same response.
  - Switching it off returns control: the turn comes back to the human and stays there.
  - The flag is per seat — a second seat's `autoPlay` stays false, and the DTO reports both.
- Re-run the card-game suite: `-Dtest='CardDeckTest,CardGameServiceTest,CardGame*IT'`.
  (`*IT` is outside surefire's default includes in this pom, as the existing ITs are, so these
  run explicitly via `-Dtest=` and not under a plain `mvn test`. Not rewiring the build here.)
- Frontend: `vue-tsc -b --force` and the production build. There is no test runner or browser
  automation in this project, so the placing/tie logic is checked with an esbuild-compiled
  harness against the real module, as the previous rounds were.

## Files

Backend: `domain/CardGamePlayer.java`, `service/CardGameService.java`,
`service/dto/CardGamePlayerDTO.java`, `web/rest/CardGameResource.java`,
`db/mysql/upgrade-card-game.sql`, new `CardGameAutoPlayVerificationIT.java`.

Frontend: `api/cardGame.ts`, `views/CardGame70235View.vue`,
`components/cardgame/PlayerSeat.vue`, `locales/en-US.ts`, `locales/zh-CN.ts`.

Docs: `.claude/plans/card-game-70235.md` — the result card section rewritten for the on-table
layout, a new auto-play section, and the leave button's move.
