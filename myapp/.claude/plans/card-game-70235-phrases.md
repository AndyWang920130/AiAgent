# 70235 — predefined phrases between players

Players pick from a fixed list of phrases during a game. The phrase appears as a bubble on the
sender's seat for a few seconds, in each reader's own language.

Decisions taken with the user before planning:

| Question | Answer |
| --- | --- |
| How it reaches the others | A bubble on the sender's seat, fading after a few seconds |
| Which phrases | Courtesy and reactions — eight, nothing that can be aimed at a person |
| When | During play (`ACTIVE`) and between games (`REMATCH`) |

## Why a code and not a sentence

The column holds `NICE_PLAY`, not "Nice play!". Two reasons, and the second is the one that
matters:

- The app is bilingual. A phrase stored as text would reach a Chinese client in whatever
  language the sender's browser happened to be in. Stored as a code, each client renders it
  through its own locale file, so the same tap reads as `好牌！` to one player and `Nice play!`
  to another.
- A code is validated against an enum on the way in. That is what makes "predefined" a property
  of the server rather than of the client's UI — the picker offers eight options, but nothing
  stops a crafted POST, and a `CardGamePhrase` valueOf is what answers it. A free-text column
  with a client-side picker in front of it would be a chat feature with a dropdown, which is
  not what was asked for and carries a moderation problem this does not.

## Where the bubble goes, and why not above the seat

The obvious design — a speech bubble floating above the seat — cannot be built in this layout.
During `ACTIVE`:

- `.player-seat:not(.is-lobby)` is `overflow: hidden` (`PlayerSeat.vue:191`), so a bubble
  positioned above the seat's own box is clipped by the seat.
- `.table-area .seat-list` is `overflow-x: auto; overflow-y: hidden`
  (`CardGame70235View.vue:1190`), so it is clipped again by the band. And there is no CSS route
  out of that: per the overflow spec, when one axis is not `visible` a computed `visible` on the
  other becomes `auto`. The horizontal scroll is deliberate — it is what a narrowed window gets
  instead of the trick area moving — so it is not available to trade.

So the bubble has to live *inside* the seat box. Two candidates, and the cost is not symmetric:

**Reserve a row for it.** Matches how everything else on this table works: hold the space from
the start so nothing moves when content arrives. But the seat's height is spelled out in
pixels in six places that all have to keep agreeing — `--seat-main-height` and the `height:
calc(...)` in `PlayerSeat.vue`, the phone override, `--seat-band-height` in the view, and the
hardcoded `height: 62px` / `--seat-band-height: 68px` pairs in the phone-short tier
(`:1541`) and immersive tier 3 (`:1633`). Two scrollbar bugs in this file came from exactly
that arithmetic failing to agree across tiers. A seventh number is cheap to add and expensive
to be wrong about.

**Put it in the row that already exists.** `.seat-played` is already a fixed-height, 
content-agnostic row: it holds a play, or a pass marker, or nothing, and keeps its height
through all three. A bubble is a fourth thing it can hold. No height changes anywhere, and the
tiers that drop that row (`:1548`, `:1644`, phone `.is-self`) drop the bubble with it, which is
the right call for those tiers anyway — they are the ones with no room to spare.

Taking the second. The cost is explicit: **while a bubble shows, that seat's last play is
hidden for ~5s.** Acceptable because those cards are not the only copy — the play on the table
is in the trick area, and the raise history under it names the seat that made each play. A
phrase is also the one thing on this table the player themselves asked to see.

At `REMATCH` the constraint lifts: `.card-game-view.is-over .table-area :deep(.player-seat...)`
is `height: auto` and the seat band is `overflow: visible` (`:1143`, `:1166`). The played row is
not rendered at all there (`v-if="!lobby && !over"`), so the bubble gets its own row and the
seat simply grows by it. This is also where phrases matter most — "good game" belongs to the
moment a game ends — and it costs nothing to support.

## Server

### `CardGamePhrase` (new enum, `contants.enumeration`)

Eight values, in the order the picker shows them. Package name keeps the repo's existing
misspelling (`contants`), which is not mine to fix in this change.

```java
public enum CardGamePhrase {
    HELLO, GOOD_LUCK, NICE_PLAY, WOW, UNLUCKY, OOPS, THANKS, GOOD_GAME
}
```

An enum rather than a `Set<String>` of allowed codes: an unknown code then fails at the
boundary, in one place, as a 400, instead of being stored and becoming a client's problem to
render. The absent values are the point as much as the present ones — nothing here can be
aimed at a person, which is what keeps a fixed list a safety property rather than a convenience.

### `CardGamePlayer` — two columns

```java
/** Last phrase this seat sent, or null. A code, rendered per reader — see CardGamePhrase. */
@Enumerated(EnumType.STRING)
@Column(name = "phrase", length = 20)
private CardGamePhrase phrase;

/** When it was sent. Drives both the display window and the send cooldown. */
@Column(name = "phrase_date")
private Instant phraseDate;
```

Per seat, not a game-level log. The bubble only ever shows each seat's most recent phrase, so
the latest is the whole of what has to be stored — no trimming, no `TRICK_LOG_MAX` equivalent,
and no decode step that can fail. (A chat log would need the game-level column; that design was
offered and not taken.)

### `CardGameService`

- `PHRASE_WINDOW_SECONDS = 5` — how long a phrase is reported for.
- `PHRASE_COOLDOWN_SECONDS = 3` — minimum gap between one seat's phrases.

```java
public CardGameDTO sendPhrase(Long id, CardGamePhrase phrase)
```

Behaviour, each point matching an existing decision in this service rather than inventing one:

- **Scoped to the caller's own seat.** No parameter for whose seat, like `setAutoPlay` and
  `setReady`.
- **`ACTIVE` or `REMATCH` only; anything else is a silent no-op** returning current state, the
  way `setReady` treats a click outside `REMATCH` (`:615`). A phrase sent as the game ends is
  about a state the caller can no longer affect.
- **Inside the cooldown, also a silent no-op.** Not a 400: the client disables its own button
  during the cooldown, so a request that arrives anyway is a poll-race or a double-tap, and an
  error toast for a double-tapped "thanks" is worse than nothing happening. Same reasoning as
  `setAutoPlay`'s no-op when the flag already holds the requested value (`:593`).
- **Does not restamp `lastActionDate`.** That field is the turn clock in `ACTIVE` and the room
  clock in `REMATCH` (`:1337`). Writing it would mean a player could hold their turn open, or
  hold the room open, by saying "hello" — so the one thing a phrase must not do is buy time.
- **Does not call `notificationService.notify`.** A phrase is for people looking at the table.
  Eight taps would otherwise be eight notification rows, and the table already polls at 1500ms.
- **Cleared in `deal()`**, beside `setReady(false)` and `setAutoPlay(false)` (`:431`, `:436`).
  A "good game" from the last game must not greet the next one.

`toDto` reports the phrase only inside its window:

```java
seat.getPhrase() == null || elapsed > PHRASE_WINDOW_SECONDS ? null : seat.getPhrase().name()
```

The server decides when a phrase has expired, not the client. Every other clock in this DTO
already works that way (`turnSecondsRemaining`, `rematchSecondsRemaining`) and for the same
reason: a client comparing a server timestamp against its own clock is wrong by whatever the
two differ by, and a bubble that outstays its window on a skewed clock is a seat that looks
stuck. The client gets a code or a null and needs no arithmetic at all.

With a 5s window against a 1500ms poll, a bubble is reported on ~3 consecutive polls. The 3s
cooldown is shorter than the window on purpose: a seat can replace its own bubble before it
fades, so a reply never has to wait for a gap.

### `CardGameResource`

```java
@PostMapping("/games/{id}/phrases")
public ResponseEntity<CardGameDTO> phrase(@PathVariable Long id, @Valid @RequestBody PhraseRequest request)

public record PhraseRequest(@NotNull CardGamePhrase phrase) {}
```

Binding straight to the enum is what rejects an unknown code, before any of it reaches a
column. Plural `/phrases` to match `/plays`.

### Migration — `upgrade-card-game.sql`

Two more guarded `ALTER`s in the established shape (`information_schema` count, `PREPARE`,
`EXECUTE`, `DEALLOCATE`), appended after the `ready` block, anchored `AFTER ready`:

```sql
ALTER TABLE `twsny_card_game_player` ADD COLUMN `phrase` varchar(20) DEFAULT NULL AFTER `ready`
ALTER TABLE `twsny_card_game_player` ADD COLUMN `phrase_date` datetime(6) DEFAULT NULL AFTER `phrase`
```

Nullable with no default, unlike the `bit(1) NOT NULL DEFAULT b'0'` flags: "has not said
anything" is the absence of a phrase, not a value. Prod runs `ddl-auto=none`, so this is applied
by hand — as the `ready` column still is.

## Client

### `api/cardGame.ts`

```ts
export type CardGamePhrase =
  | 'HELLO' | 'GOOD_LUCK' | 'NICE_PLAY' | 'WOW' | 'UNLUCKY' | 'OOPS' | 'THANKS' | 'GOOD_GAME'
```

`CardGamePlayer` gains `phrase: CardGamePhrase | null`, documented as the server's decision —
non-null means show it, null means do not, with no window for the client to know about.
`cardGameApi.sendPhrase(id, phrase)` posts to `/phrases`.

### `PlayerSeat.vue`

A `phrase` bubble rendered from `player.phrase`, with no guard of its own — the same rule the
ready tick follows, and for the same reason: the server already scopes the field, so a second
condition here is a copy of that rule that can disagree with it.

- During play: inside `.seat-played`, taking the row's content when a phrase is live.
- At game over / `REMATCH`: its own row, where the seat is content-sized.
- Fade on enter and leave via a `<Transition>`, opacity only — matching `.seat-ready-*`
  (`:273`). Opacity only is not cosmetic: the row must not resize as a bubble arrives.

### `CardGame70235View.vue`

An `a-popover` holding eight small buttons in a wrapped grid, behind one button labelled from
`cardGame.say`:

- During `ACTIVE`, appended to the existing `.hand-actions` `a-space wrap` — it already wraps,
  so no new band and no height to account for.
- During `REMATCH`, appended to the `a-space` in `.rematch-rows`, beside the ready button.

A popover grid rather than a dropdown menu: eight phrases are two taps either way, but a grid
shows all eight at once, and mid-game the thing being optimised is how long the player's
attention is off the table.

The button disables itself for the cooldown, ticked down locally like `turnRemaining` already
is (`:613`), so the control reflects the server's rule instead of relying on a no-op the player
cannot see. The server's no-op stays the authority — the client guard is the same
belt-and-braces as `playSelection`'s in-flight check (`:441`).

Sending calls `applyGame` on the response, so the sender sees their own bubble immediately
rather than on their next poll. The exception is a phone's own seat, which hides its played row
(`.is-self`, `:363`) — there the cooldown on the button is the confirmation. Worth stating
plainly rather than discovering: on a phone you do not see your own bubble during play.

### Locales

`cardGame.phrases.*` keyed by code, nested like `cardGame.shapes.*` already is, plus
`cardGame.say` for the button.

| Code | en-US | zh-CN |
| --- | --- | --- |
| `HELLO` | Hello! | 你好！ |
| `GOOD_LUCK` | Good luck! | 手气好运！ |
| `NICE_PLAY` | Nice play! | 好牌！ |
| `WOW` | Wow! | 厉害！ |
| `UNLUCKY` | Unlucky | 可惜了 |
| `OOPS` | Oops | 失误了 |
| `THANKS` | Thanks | 谢谢 |
| `GOOD_GAME` | Good game | 打得好 |

## Verification

`mvnw.cmd -q compile`, `vue-tsc -b`, `npm run build` (`noUnusedLocals` is on, so an unused
import fails the build).

Then the convention in `D:\tmp` — `check_table_phrases.py` with `prove_table_phrases.py`
reverting each change in turn to assert the checker fails, registered in `prove_untouched.py`'s
`RUNNERS` so a proof run is shown not to have written to the watched tree.

The checks worth having are the agreements a compiler cannot see:

- **The code list agrees in four places** — the Java enum, the TS union, `en-US`, `zh-CN`. Each
  parsed for its own set of eight and compared; a phrase added to one place and not the others
  is the failure mode this feature invites. The existing `check_ready_tag.py` key scan reduces
  `cardGame.x.y` to its root object, so `phrases` is covered as a key but its *members* are not
  — this is the check that covers them.
- **The window and cooldown are the server's** — the client must not compare timestamps.
  Rejection check: no phrase-expiry arithmetic in the view or the seat.
- **The bubble carries no status guard** beyond `player.phrase`.
- **`lastActionDate` is not written by `sendPhrase`.** Rejection check — this is the one with a
  real consequence if it regresses (a seat holding its turn clock open by chatting).
- **`deal()` clears the phrase**, beside the `ready` and `autoPlay` resets.
- **The transition animates opacity only**, as `.seat-ready-*` does.
- **The migration is guarded** on `information_schema` and nullable, not `NOT NULL`.

Both runs reported in full, pass or fail.

## Not in this change

- **A free-text chat.** Different feature with a moderation problem attached.
- **A chat log or history.** Offered; the bubble was chosen. Storage follows that choice — a log
  would need the game-level column, so adding one later is a schema change, not a UI change.
- **Phrases at the waiting table (`PENDING`).** Offered and not taken. The people there have not
  accepted yet.
- **Push.** Poll-only, like `ready`. A phrase arrives within 1500ms, which is inside the window
  it shows for.
- **A notification row per phrase.** See above.
