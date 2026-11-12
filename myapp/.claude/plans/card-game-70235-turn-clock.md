# 30-second turns, and auto-play after two missed in a row

Two changes to the turn clock.

1. The per-turn budget drops from 120s to 30s.
2. A player whose clock lapses **twice in a row** has auto-play switched on for them, and it stays
   on until they switch it off.

Decided with you: the auto-enable is **per game** (it clears with the deal, like the manual switch
already does), and **nothing announces it** — no bell notification, no toast. The rule goes in the
hint text so it is findable before it fires, and the switch flipping is what the player sees.

## 1. The budget

`turn-timeout-seconds` is already a config value read in one place, so the change is the number —
in both the yaml and the `@Value` fallback, which must agree or a deployment without the yaml key
behaves differently from one with it.

- `back/.../config/application.yaml:23` — `120` → `30`, and its trailing comment.
- `CardGameService.java:137` — `@Value("${app.card-game.turn-timeout-seconds:120}")` → `:30`.

### The red threshold has to move with it

`CardGame70235View.vue:908` turns the countdown tag red at `turnRemaining <= 15`. On a 120s budget
that is the last eighth; on a 30s budget it is **half the clock red**, which makes the warning
colour the normal colour and leaves it saying nothing.

It becomes a named constant at 10s, and deliberately an absolute number rather than a fraction of
the budget: it answers "is there still time to decide", and how long that takes depends on the hand
in front of you, not on how generous the clock is. A fraction would also have been wrong in the
other direction at 120s (a third would have gone red at 40s, with over half a minute still to go).

The 30 itself is not duplicated in the client — `turnTimeoutSeconds` is already on the DTO
(`api/cardGame.ts:142`) and `turnSecondsRemaining` already drives the tag.

## 2. Auto-play after two consecutive timeouts

### Where a timeout is already observed

`resolveTimeouts` (`CardGameService.java:1157-1173`) is the one place a human's lapsed clock is
seen: it finds the seat whose turn it is and calls `act(...)`, the same brain CPU seats use. It runs
on every read path via `advance`, so with the client polling at 1500ms, "the clock lapsed" is
noticed about a second after it happens.

So the count belongs on the seat, incremented there.

### The column

`CardGamePlayer` gains `timeoutStreak` (`timeout_streak`, `int NOT NULL DEFAULT 0`) — consecutive
turns taken by the server because this seat did not answer. Per seat and per game, like `autoPlay`
and `ready` beside it.

An int rather than a boolean "has missed one": the threshold is a number with a reason, and a
boolean would have to be renamed the first time the rule changes to three.

### The rule

In `resolveTimeouts`, once the lapsed seat is found and **before** `act(...)`:

- Skip the whole thing if `isAutomatic(seat)` — a CPU, or a seat already on auto-play, has not
  missed a turn, it is being played by design. Without this guard the streak would climb on seats
  that never had a choice.
- Otherwise increment, and if it reaches 2, set `autoPlay` true. Save the seat.

Before `act(...)` rather than after, so the flag is already on when `runAutomaticTurns` picks the
table up later in the same request — which is exactly how the manual switch behaves (its javadoc at
`:608` makes the same point: turning it on while the turn is yours plays that turn in this
response).

### Where the streak resets

Resetting is what makes it *consecutive*. Four places, and each is a different claim:

| Reset | Why |
|---|---|
| `play` | They took a turn themselves. |
| `pass` | Also a turn taken themselves — declining is an answer. |
| `setAutoPlay`, **either direction** | Off is them back at the table, and they should get a fresh pair of chances, not be re-automated by the next single timeout. On is a deliberate choice that spends the count. |
| `deal` / `finish` | Per-game, like the flags beside it. |

Not reset by sending a phrase. The rule is about turns; chatting while not playing is the case it
exists for.

`deal()` (`:451`) and `finish()` (`:1001-1007`) already clear `autoPlay` for exactly the reason that
applies here, so the streak is cleared alongside it — in `finish()` that means widening the existing
`if (seat.isReady() || seat.isAutoPlay())` guard to include a non-zero streak.

### Not on the DTO

No client needs the count: with nothing announced, the only visible effect is `autoPlay` turning
true, which the client already renders. Adding it would be a field nothing reads.

## 3. The SQL migration

`upgrade-card-game.sql` gets one more guarded `ALTER`, in the same `information_schema` form as the
five before it, anchored `AFTER phrase_date`. `NOT NULL DEFAULT 0` is right for existing rows: a
seat mid-game has missed nothing the server counted.

Prod runs `ddl-auto=none`, so this is yours to apply by hand — it joins the three columns from the
phrase round still outstanding.

## 4. Text

- `autoPlayOffHint` (both locales) gains the rule. It is the one place it can be read *before* it
  fires, and it is already the line that describes what is not currently happening.
- `autoPlayOnHint` is left alone: it is true however the switch came to be on, and a player
  reading it wants to know how to take back control, which it already says.
- `clickAgainToPlay` (both locales) drops "two minutes" and takes the budget as an interpolated
  `{seconds}` from `game.turnTimeoutSeconds`, so the sentence cannot go stale against the config
  the way it just did.
- `cardGameStep6Body` (both locales, the user guide) — "two minutes" / "两分钟" → 30 seconds. Static
  text with no game in scope, so the number is written out, with the config key named.
- `.claude/plans/user-guide.md:145-146` — the same two numbers.

## 5. Verification

A new `check_turn_clock.py` / `prove_turn_clock.py` pair, registered in `prove_untouched.py`
(`CardGamePlayer.java`, `application.yaml` and `upgrade-card-game.sql` into `WATCHED`; the prover
into `RUNNERS`).

What it checks, each one an agreement no compiler sees:

1. The yaml value and the `@Value` fallback are **both 30** and **equal to each other** — parsed out
   and compared, not matched as literals. Disagreeing is a bug that only shows up on the deployment
   whose yaml lacks the key.
2. No `120` left in either, and no "two minutes" / "两分钟" in either locale.
3. The red threshold is a named constant, and is **less than half** the budget — the property that
   was violated, rather than the literal 10. Checked against the yaml value, so changing the budget
   without revisiting the threshold fails here.
4. The streak increments in `resolveTimeouts`, guarded by `isAutomatic` (a rejection: the guard
   removed must fail).
5. The threshold for auto-enabling is a named constant, and the flag is set **before** `act(...)` —
   checked by the order of the two sites within the method body, since after it is a one-turn-late
   bug that nothing else notices.
6. The seat is saved on that path (`resolveTimeouts` otherwise saves only the game).
7. All four resets present: `play`, `pass`, both directions of `setAutoPlay`, and `deal`/`finish`.
8. `finish()`'s guard includes the streak — otherwise a seat that missed one turn carries the count
   into the next game, and the "per game" decision silently does not hold.
9. The migration's guarded ALTER exists, is `NOT NULL DEFAULT 0`, and the `CREATE TABLE` body is
   left alone (the same pair of checks the phrase columns got).
10. Both locale hints mention the rule, and `clickAgainToPlay` interpolates rather than hardcodes.

Then `mvnw.cmd -q compile`, `mvnw.cmd -q test-compile`, `npx vue-tsc -b`, `npm run build`.

## What I am not doing

- No notification and no toast — your call, recorded above.
- No change to `act`, the CPU brain, or any rule of play. A timeout plays the same turn it plays
  today; the only new thing is the flag that gets set on the way.
- Not touching `invite-timeout-seconds` or `game-timeout-seconds`.
- Not running the ITs. They are `@SpringBootTest` against MySQL and still need a profile named;
  compile-only, as last time.

## Not verifiable from here

Whether 30 seconds is comfortable with five seats and a full hand — a real table is the only way to
know. The number is one config line if it turns out to be tight.
