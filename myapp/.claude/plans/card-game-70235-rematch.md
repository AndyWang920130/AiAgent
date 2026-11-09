# 70235: the host's layout shifting, and playing again without rebuilding the table

Two reports from testing:

1. "When inviting both an AI player and a friend, the host's UI layout changes after the game
   starts, while the friend's UI remains normal."
2. "When the game ends, there is no option to ready up for the next game." — and, from the
   round this was first raised in: players should be able to ready up again, the host should be
   able to start a new game, and any player leaving should disband the room.

## 1. Two clients, two different tables

Every band of the table is a fixed height by design, so the only thing that *can* differ
between two clients on the same screen is the fit scale. It was broken twice over.

**The observer observed nothing.** `useFitScale` set up its `ResizeObserver` in `onMounted`,
and at mount the view is still showing the lobby — the table is behind a `v-if` on state a
poll has not returned yet, so `fitBox` and `tableEl` are both null. Nothing was ever observed.
The only thing left that could trigger a measurement was the window `resize` listener, so the
scale stayed at 1 however tall the board turned out to be, and two clients could disagree
about it purely according to whether either had happened to resize their window.

Fixed by watching the two refs, with `flush: 'post'` so the elements are in the DOM and
measurable, and re-observing whenever they change. The fit is now a property of the layout
rather than of the window's history.

**The scale fed back into the layout.** `tableStyle` set `width: ${100 / scale}%` alongside
the transform, so the board would fill its space sideways however far it was scaled down. That
made the table's *layout* width a function of the scale: a shorter window got a wider layout
box, which re-wrapped the button row and the seat row, which changed the natural height the
scale was measured from. A client that had measured therefore got a structurally different
table rather than the same table drawn smaller — which is exactly what was reported.

Now the transform and nothing else. One design width, drawn smaller when it has to be.

Ruled out on the way: the host is dealt six cards to everyone else's five
(`LEAD_HAND_SIZE` vs `TARGET_HAND_SIZE`), which is a real asymmetry but a height-neutral one —
`.card-hand` has `min-height: 92px`, does not wrap, and both hands fit the 960px table. The
host's `myTurn` at the first turn adds a tag to a fixed-height `overflow: hidden` status bar
and text to a `min-height: 18px` line.

## 2. Another game, same room

### The organising decision

The `CardGame` row **is** the room. There is no separate room entity: the seats, the
invitations they answered and the CPU opponents are all on this row. So a game that ends should
not take the row to a terminal status while the seats are still filled and the players still
want to play.

### `REMATCH`, a new status between played and closed

`finish()` now lands on `REMATCH` instead of `FINISHED`. The result stays on the row — winner,
first out, the scores on the seats — and is what the table displays while the players decide.
`FINISHED` is reached *from* there, by the window lapsing or the room being disbanded.

A reused `FINISHED` was the alternative and it cannot distinguish "room open, deciding" from
"room closed": resume, occupancy and the timeout would each have needed a flag of their own to
tell the two apart. `REMATCH` also falls out of the view's existing `gameOver` computed for
free, so the result display, the placings and the winner badge all keep working unchanged.

### A `ready` column, not a reset `invitationStatus`

`CardGamePlayer.ready`, reset by every deal so it always describes the game about to start.

Resetting `invitationStatus` to `PENDING` was the alternative. It would make "has not readied
up yet" and "has not answered an invitation yet" the same value — and `deal()` drops every
`DECLINED` seat and counts the `ACCEPTED` ones, so a player who was merely slow to ready up
would be at risk of being treated as one who had turned the table down.

### One status list became three

`LIVE` was used for three different questions, which had quietly diverged:

| list | means | used by |
| --- | --- | --- |
| `LIVE` | there are turns to take | `setAutoPlay` |
| `SEATED` | the player is at a table | `createTable`, the invitee reservation, `leave`'s notifications |
| `RESUMABLE` | what a reload puts you back into | `accept`'s guard, `getActiveGame` |

`PENDING` is in `SEATED` but not `RESUMABLE`: a table still filling up arrives through
`/invites`, which is also where the invitations still to answer come from.

### The lifecycle

- **`setReady(id, ready)`** — participant-scoped, the caller's own seat only, and a no-op
  outside `REMATCH`. Same late-call tolerance as `setAutoPlay`: a click that lands just after
  the host dealt is about a state the caller can no longer affect, and the response shows them
  what happened instead of an error about it.
- **`rematch(id)`** — host only, requires `REMATCH` and every other human ready, then re-deals
  this row. The host does not get a ready flag of their own: dealing *is* their readiness.
- **`leave(id)`** — `REMATCH` now joins `ACTIVE` in going to `ABANDONED`, with the other seats
  notified. The notification wording splits on which it was: a game in progress ended, or a
  room closed with no next game. A table still filling up was never a room, so `PENDING` still
  goes to `CANCELLED`.
- **`resolveTimeouts`** — a `REMATCH` branch before the `ACTIVE` guard closes an idle room to
  `FINISHED` on `lastActionDate + inviteTimeoutSeconds` (5 minutes). The clock runs from when
  the game was scored and is *not* restamped by readying up, so it bounds the whole decision
  rather than the gap between clicks — a room cannot be held open indefinitely by one player
  toggling their flag.
- **`deal()`** — clears `ready`, `winnerUsername` and `firstOutUsername`. The last two are what
  let the previous result stay readable until the moment the next game starts; a game in
  progress carrying a winner already named would read as over.

### A pre-existing bug fixed at its source

The round cap reaches `finish` from `resolveTimeouts` mid-trick, so `currentPlayUsername`
survived into the finished state and the green `.is-leading` seat background outlived its game.
A previous round patched the symptom in the view by gating `leading` on `inGame`; `finish` now
calls `clearTrick(game)` itself, which is a no-op on the ordinary `resolveTrick` path.

### What the player sees

- **The rematch bar**, in the space the hand controls had, under their own hand — where they
  were just playing and where the scores on the seats above are the reason they are deciding.
  Not in the corner with the way out.
- **The host gets the deal**, disabled until `canRematch`. **Everybody else gets a toggle**
  they can take back, because a one-way ready button commits a misclick to a game they did not
  want.
- **One line saying what the room is waiting for**, naming either the people or the host.
  "Waiting" on its own leaves the player wondering whether anything will happen at all. It has
  its own `.rematch-hint` class rather than being a `.play-hint`: three immersive rules hide
  those on a short screen, which is right for a sentence about a switch whose state is visible
  in the switch, and wrong for the only place the room says who it is waiting on.
- **A closing clock**, red under a minute. A room that vanished on its own with nothing having
  said it would is indistinguishable from one that broke. Its own DTO field rather than a reuse
  of `inviteSecondsRemaining`, which would have the client claim an invitation was expiring at a
  table that had already played a game.
- **The corner button means "leave" in all three states**: it ends a game being played, closes
  an open room for everyone in it, and once there is nothing left to end it just dismisses the
  result. The first two reach the other players and are the danger colour; the third touches
  nothing but this screen.
- **`REMATCH` polls like a live game.** It is the one status where the table changes with no
  action from this client — another seat readying up, the host dealing, somebody leaving — and
  all three have to arrive on their own or the room looks frozen to everyone but whoever acted
  last.

### Two details that would have read as bugs

**CPU seats and the host report `ready: true`.** Neither has anything to decide. The ticks on
screen then mean "waiting on nobody here", and the waiting-for line is built from the seats
that are genuinely outstanding — a host is not waiting on themselves.

**An abandoned room says the room closed, not that the game ended.** A winner on an `ABANDONED`
row means the game had been played out and it was the room somebody walked out of; nobody lost
a game in progress. The server only names a winner when it scores a game and a deal clears the
name again, so this distinguishes the two with no field of its own. The same reasoning gates the
winner headline: without it, an abandoned table would announce a winner.

## Files

| file | change |
| --- | --- |
| `front/src/utils/fitScale.ts` | watch the refs instead of observing once at mount |
| `front/src/views/CardGame70235View.vue` | `tableStyle` loses the width override; `inRoom`, `iAmReady`, `notReadyNames`, `rematchHint`; winner gated on status; abandoned-room wording; the rematch bar and its CSS; `REMATCH` polled; corner button |
| `front/src/api/cardGame.ts` | `REMATCH`, `ready`, `canRematch`, `rematchSecondsRemaining`, `setReady`, `rematch` |
| `front/src/locales/{en-US,zh-CN}.ts` | 10 keys each |
| `back/.../enumeration/CardGameStatus.java` | `REMATCH` |
| `back/.../domain/CardGamePlayer.java` | `ready` column |
| `back/.../service/CardGameService.java` | the status split, `setReady`, `rematch`, `othersReady`, `finish`, `deal`, `leave`, `resolveTimeouts`, `toDto` |
| `back/.../repository/CardGameRepository.java` | `findActiveForUser` deleted — no callers, and its ACTIVE-only filter contradicted the new resume rule |
| `back/.../dto/CardGame{,Player}DTO.java` | `canRematch`, `rematchSecondsRemaining`, `ready` |
| `back/.../web/rest/CardGameResource.java` | `POST /games/{id}/ready`, `POST /games/{id}/rematch` |
| `back/.../db/mysql/upgrade-card-game.sql` | `ALTER TABLE ... ADD COLUMN ready` |

## Verification

`mvnw compile`, `vue-tsc -b` and `npm run build` all pass. There is no test runner or browser
automation in this repo, so the behavioural rules are checked by source analysis:

- `D:\tmp\check_rematch.py` — 24 checks over the view, the API client, the service, the DTOs,
  the resource, the entity and the migration, plus locale parity in both directions and every
  `cardGame.*` key the view renders.
- `D:\tmp\prove_rematch.py` — reverts each of those changes in turn and asserts the checker
  notices. **18/18 caught**, every file restored. A check that cannot fail is not a check.
- The earlier rounds' checkers still pass: `check_immersive_css.py`, `prove_checks.py` (8/8),
  `check_guide_keys.py`, `check_keytopath.py`. `check_immersive_final.py`'s height model has the
  game-over comment updated: `my-area` used to be an overestimate there by the 50px of hidden
  hand controls, and the rematch bar now takes that space to within 4px, so the same number
  stands for both states. The finished table is still 106–386px shorter than the running one in
  every tier, so the bar cannot bring back a scrollbar.

Not done: a real-device check of game over on a five-seat table with long names, where the
waiting-for line is a list of them.

---

# Follow-up: the host's one-line table, and auto-play outliving its game

Two things reported after the round above.

## 1. The host's table drew every row on one line

Reported first as a layout that changed for the host and not the guest once the game started,
then more sharply as "all elements displayed in a single row" with a single invited friend.

The cause was a coupling between the fit scale and the board's *layout* width. `.table-area`
carried `width: 100 / scale %` alongside its transform, so the board would fill its space
sideways however far it had been scaled down. That makes the layout width a function of the
scale: a scale of 0.5 gave the board a 1920px layout box, and every row on the table that is
meant to wrap -- `.my-head`, `.hand-actions`, `.control-rows` -- had room to sit on one line
instead. Scaled back down to fit, the result is a table whose rows are all single lines. Two
players at the same table on the same screen size could therefore be looking at
differently-shaped boards, decided by nothing more than whether either had resized their
window, because the window `resize` listener was the only thing that drove a measurement at
all (see below).

The override is gone; the scale reaches the board as a transform and nothing else. One design
width, drawn smaller when it has to be.

Two further gaps in the measurement, both of which could leave the first reading on screen for
as long as the table was:

- **A box measured mid-layout was scaled from.** `measure()` only rejected `available <= 0`, and
  `.table-fit` is `flex: 1 1 auto; min-height: 0` -- the configuration that reports a few pixels
  while its ancestors' heights are still resolving. A small positive reading yielded an
  arbitrarily tiny scale. Now a box under `MIN_BOX_PX` (120px) is treated as "not yet laid out"
  and leaves the scale alone, and `MIN_SCALE` (0.35) floors the result for whatever this
  reasoning has not thought of. The caller's own breakpoints stop subdividing at 595px, so a box
  smaller than 120px is not a small window.
- **Nothing necessarily measured twice.** The `ResizeObserver` reports a box that *changed*
  size; a box already being reported at its final size never changes again. The view mounts the
  board in response to the same state change that turns the layout shell's immersive mode on, so
  the first reading can be taken before the height lock has reached it. `remeasureNextFrame()`
  now takes one more look a frame later, from the watcher and from `onMounted`, cancelled on
  unmount.

The window `resize` listener is no longer the only thing driving a measurement, which was the
state of affairs that made the scale a property of each client's resize history rather than of
its layout.

## 2. Auto-play outlived the game it was turned on for

Handing your turns to the server is about the game in front of you. A player who did that to
get through a game they had lost interest in has not asked to sit out every game the room plays
afterwards -- and left on, the next deal would be played without them ever touching it, with
the switch to find on a table whose cards are already gone.

`finish()` now clears `autoPlay` in the same loop that clears `ready`, both flags having
described the game that just ended. The loop's condition widened to `isReady() || isAutoPlay()`
so a seat with auto-play on and no ready flag is still saved. `deal()` clears it too, which is
what covers a seat that joined the room since the last `finish()`. CPU seats are unaffected
either way: `isAutomatic()` reads `isCpu()` for them, not this flag. `setAutoPlay` was already
gated on `LIVE`, so the switch cannot be turned back on between games -- and the view only
renders it under `v-if="inGame"`.

No frontend change was needed: the switch is bound to `me.autoPlay` from the server with no
local mirror, so the reset arrives with the next response.

## Files

| file | change |
| --- | --- |
| `front/src/utils/fitScale.ts` | `MIN_BOX_PX`, `MIN_SCALE`, `remeasureNextFrame()` |
| `front/src/views/CardGame70235View.vue` | the `width: 100 / scale %` override removed from `tableStyle` |
| `back/.../service/CardGameService.java` | `finish()` and `deal()` clear `autoPlay` |

## Verification

`mvnw compile`, `vue-tsc -b` and `npm run build` all pass.

- `D:\tmp\check_autoplay_fit.py` -- 18 checks: the two backend resets and the gate that keeps
  them from being undone, the four properties of the measurement, and the regression guard that
  the board's layout width is never a function of the scale. The guard matches against the view
  with comments stripped, since the comments in it discuss the coupling being rejected.
- `D:\tmp\prove_autoplay_fit.py` -- **15/15 mutations caught**, every file restored, including
  re-adding the width coupling in both the `100 / scale` form and as a `zoom`, and flipping the
  board to `flex-direction: row`.
- Every earlier checker still passes: `check_rematch.py` (24), `prove_rematch.py` (18/18),
  `prove_checks.py` (8/8), `check_guide_keys.py`, `check_keytopath.py`, and the five
  `check_immersive_*.py`.

Not verified: that the host's table now matches the guest's on a real pair of browsers. The
width coupling is proved absent from the source and from the built CSS, and the measurement is
proved to reject a half-laid-out box, but neither is the same as seeing the two side by side.
Worth noting the built CSS bundle is content-hashed, so a stale `index.html` is the one way a
browser could still be running the old rule.


# Round 4 -- the host readies up like everyone else

## Why

The question that started it was whether the other players can see anything when the host asks
for another game. Under the original design they could not see a decision, because the host did
not make one: the host had a *Play again* button and everybody else had a *Ready* toggle, so the
host's intent only ever became visible as the next game appearing. Worse, the button was gated on
`canRematch`, which required every other seat to be ready first -- so a host who clicked early
clicked a disabled button, with the reason sitting in a hint line rather than in the control.

That is two separate things to understand (my toggle, the host's button) for one decision that is
the same for everybody. So the host now has the same toggle, and the deal is not an action anyone
takes: whoever is last to ready up triggers it.

## What changed

- `setReady` is the only path to another game. When the flag it just set makes every human seat
  ready, it deals in the same request and returns the dealt game. `rematch(Long id)` is deleted.
- `othersReady(host, seats)` becomes `allReady(seats)`: every non-CPU seat, host included. A room
  of host-plus-CPUs therefore needs exactly one click, from the only player in it.
- `toDto` no longer reports the host's seat as ready on the host's behalf -- `rematch && (seat
  .isCpu() || seat.isReady())`. CPU seats still read as ready because they have nothing to decide.
- `canRematch` is gone from `CardGameDTO`, the service, the api client and the view. Nobody clicks
  a deal button, so there is no enabled state to report. What the table draws instead is the
  per-seat `ready` flags.
- `POST /games/{id}/rematch` is deleted. One endpoint, `/ready`, whose response to the last ready
  is the next game already dealt.
- The view's two-button block becomes one toggle. `rematchHint` drops to two branches -- your own
  answer until you give one, then who the room is still waiting on. There is no third "everybody
  ready" branch, because that is not a state a client can be polled in: the last ready deals in
  the same request, so a client seeing every seat ticked is already looking at the next game.
- `playAgain`, `rematchReady` and `rematchWaitingHost` are removed from both locales, and
  `rematchAsk` reworded to say the next game starts once everybody is ready.

## What the other player actually sees

Still polling, not push: `GAME_INTERVAL = 1500`, and neither `setReady` nor the re-deal calls
`notify`. So a seat readying up, and the deal itself, land on the other clients within about a
second and a half. That was already true of the old flow; what is new is that the readying is
visible as readying, rather than only as its result.

## Files

| file | change |
| --- | --- |
| `back/.../service/CardGameService.java` | `setReady` deals on the last ready; `allReady` replaces `othersReady`; `rematch()` deleted; host special case out of `toDto`; `canRematch` computation removed |
| `back/.../service/dto/CardGameDTO.java` | `canRematch` removed |
| `back/.../service/dto/CardGamePlayerDTO.java` | `ready` javadoc: every human seat reports its own answer |
| `back/.../web/rest/CardGameResource.java` | `/rematch` endpoint deleted |
| `front/src/api/cardGame.ts` | `canRematch` and `rematch()` removed |
| `front/src/views/CardGame70235View.vue` | one shared toggle; `rematchHint` to two branches; `startRematch()` deleted |
| `front/src/locales/{en-US,zh-CN}.ts` | three keys removed, `rematchAsk` reworded |

## Verification

`mvnw -q -o compile`, `npx vue-tsc -b` and `npm run build` all exit 0.

- `D:\tmp\check_rematch.py` -- **27 checks**, 7 locale keys, 59 used keys. The runner now takes an
  optional fifth tuple element of *forbidden* strings, because removing a design is a change like
  any other and a checker that can only require things cannot tell that what it replaced is gone.
  Four checks are rejections: `game.canRematch`/`startRematch`/`cardGame.playAgain` in the view,
  `rematch(Long id)`/`othersReady(` in the service, `/games/{id}/rematch` in the resource, and
  `boolean canRematch,` in the DTO.
- `D:\tmp\prove_rematch.py` -- **25/25 mutations caught**, every file restored, baseline re-run
  clean. Seven are new: nothing deals the next game, readiness excluding the host, the host
  reading as ready without saying so, and one per rejection check putting the rejected thing back
  (a `rematch(Long id)` method, a `/rematch` mapping, `canRematch` in the record, a `v-if` host
  branch on the toggle). A rejection check with no mutation behind it is unproven.
- Still green: `check_autoplay_fit.py` (18), `prove_autoplay_fit.py` (15/15), `prove_checks.py`
  (8/8), `check_guide_keys.py`, `check_keytopath.py`, the five `check_immersive_*.py`.

Not verified: the flow on two real browsers -- that the second client sees the first seat tick
within the poll interval, and that the deal arrives for both. The migration from round 3 is still
outstanding and still has to be applied by hand, prod running `ddl-auto=none`:

```sql
ALTER TABLE twsny_card_game_player ADD COLUMN ready bit(1) NOT NULL DEFAULT b'0' AFTER auto_play;
```

# The one-row table was never a layout bug -- SUPERSEDED, SEE BELOW

Everything in this section is wrong except the measurements. It blamed HMR, on the grounds
that a reload rendered correctly, and concluded there was nothing in the source to fix. There
was. The real cause is in the next section; this one is kept because the reasoning that went
wrong is worth having on the record.


Four rounds chased a table that rendered its seats, trick, status bar and hand side by side in a
single row instead of stacked. Three source fixes changed nothing, because there was nothing in
the source to fix. A full page reload renders it correctly; the broken layout only ever appeared
in a tab that had been open across HMR updates.

The decisive evidence was the DOM ancestor chain of a seat box:

```
player-seat.is-active   flex column
.seat-list              flex row     (logged twice -- Chrome collapses identical lines)
.seat-list              flex row
.waiting                flex column
.ant-card-body
```

A `.seat-list` nested inside another `.seat-list`, both inside `.waiting`, with no `.table-fit`
or `.table-area` anywhere, holding a seat that carried `is-active` -- a binding that exists only
in the table branch. The outer `.seat-list` measured 1861x348 with `display: flex; flex-wrap:
wrap`, so it laid the inner seat-list (468), `.trick` (353), `.status-bar` (267) and `.my-area`
(336) out horizontally at a common line height. That is the bug, measured.

A fresh render cannot produce it. `.waiting` (`v-else-if="waitingTable"`) and `.table-fit`
(`v-else`) are siblings in one v-if chain, the template parses balanced, and a `v-else` nested
inside its own `v-if` sibling is a Vue compile error the passing build rules out. What produces it
is Vue's HMR patching the table branch's children into the lobby branch's root instead of
remounting, after the component's template structure changed underneath an open page.

The template changes came from the mutation runners, which wrote into the watched tree while Vite
was serving it -- 25 edits from `prove_rematch.py`, 15 from `prove_autoplay_fit.py`. That is now
impossible: every runner copies the files it needs into a temp mirror and points the checker at it
with `CHECK_ROOT`, and `D:\tmp\prove_untouched.py` fingerprints 14 project files by hash *and*
mtime around all three runners and reports `PROJECT UNTOUCHED`. Mtime matters -- a write-then-
restore of identical bytes leaves the hash equal and still triggers a reload.

Two things to carry forward. A mutation runner must never write to a tree a dev server is
watching; the mutations reach the browser and the symptoms they cause get attributed to the code.
And when a symptom cannot be derived from the source, stop reading the source -- four rounds of
template and media-query review could not have found this, and one ancestor chain did.

# Round 5 -- a comment between two branches

The one-row table was a real bug with a real cause: a `<!-- comment -->` sitting between
`v-if` and `v-else`.

A comment between two branches has nowhere to go. The compiler has to keep the chain
adjacent, so it absorbs the comment into the branch that follows, and that branch stops being
one element and becomes a two-child fragment -- the comment plus the div. Both swapping
branches here were in that state, which the compiled output said plainly:

```
: $setup.waitingTable ? (_openBlock(), _createElementBlock(_Fragment, { key: 1 }, [
    _createCommentVNode(" ===================== WAITING TABLE ===================== "),
    _createElementVNode("div", _hoisted_9, [ ...
  ], 2112 /* STABLE_FRAGMENT, DEV_ROOT_FRAGMENT */))
```

Two sibling multi-root fragments can be patched into each other on a switch rather than
replaced: the new branch's children get mounted into the old branch's DOM node. That is the
`.seat-list` inside `.seat-list` inside `.waiting` from the ancestor chain, and it is also why
the table overflowed its 960px cap -- `ref="fitBox"` ended up on the wrong element, so
`useFitScale` measured a box that was not `.table-fit` and the cap never applied.

The fix is two lines of structure per branch: move the comment inside the element, and key the
branch roots by name (`lobby`, `waiting`, `table`). `DEV_ROOT_FRAGMENT` in the served module
goes from 2 to 0.

Three things this cost, all avoidable:

- **The template is not wrong to read, only wrong to compile.** Four rounds of reading it, and
  of reading all 1650 lines of CSS for a `flex-direction: row` that was never there, could not
  have found this. `curl http://localhost:5173/src/views/CardGame70235View.vue` answers in one
  call what the source cannot answer at all. Ask the dev server what it is serving.
- **Three wrong diagnoses, each disproved by the user's own data**: a stuck HMR CSS swap, a
  stale Vite module graph, and a stale browser cache. Each was an inference about what the
  server ought to be serving. None survived a check against what it *was* serving, and the
  first two cost the user a restart and a reload that could not have helped.
- **Screenshot mtimes are when the file was saved, not when the shot was taken.** The clocks
  inside the images are the evidence: `120s`/`60:00 left` against `63s`/`59:04 left`, same
  hand and same Trick 1, put the broken state 56 seconds *before* the good one and identified
  the trigger as the in-place branch swap on Start rather than anything about reloading.

Same defect found in four other places by the new checker. `GomokuView.vue:700` is the one
that matters -- `<!-- In-game -->` above `<div v-else class="online-game">`, the same
lobby-to-game switch, in the view 70235 was mirrored from. Also `PersonalView.vue:421/451`,
where both branches are the same `a-card` with the same prop shape, and
`NotificationView.vue:134/140`. For component branches the comment moved *above* the `v-if`
rather than inside, so nothing is injected into a slot.

Verification:

- `D:\tmp\check_branch_roots.py` -- repo-wide rule over all 39 `.vue` files under the view and
  component trees: the line before an element branch must not end a comment. `<template
  v-else>` is exempt and the checker says so -- a template branch is a fragment whatever
  precedes it, so there is no element root to reuse and no key to misplace.
- `D:\tmp\prove_branch_roots.py` -- **3/3 mutations caught**, each putting a comment back above
  one of the fixed branches. Enumerates the `.vue` files rather than listing them, so a view
  added later lands in the sandbox without anybody remembering to add it.
- `vue-tsc -b` and `npm run build` exit 0. All 11 existing checkers still pass.
  `prove_untouched.py` still reports `PROJECT UNTOUCHED`, 25/25 + 15/15 + 8/8.

Not verified: the browser. The table rendering correctly when Start is clicked without a
reload is the thing this predicts, and only you can see it.
