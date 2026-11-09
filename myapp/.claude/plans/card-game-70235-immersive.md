# 70235: the whole table on one screen, no scrollbar

Request: "Can we remove the scrollbars once the game starts so that the entire game interface
fits on the screen without scrolling".

## The actual cause

Two separate things, and only the second one is about the card table.

**The shell always exceeds the window.** `MainLayout` sets `min-height: 100vh` on the outer
layout and puts a footer below the content, so the document is taller than the viewport on
every page in the app. That is right for a list page — it is what lets a long list scroll —
and wrong for a board that wants to be the window. No amount of arithmetic inside the view
fixes it, because the view is not what is overflowing.

**The table needs a 1085px viewport.** Measured from the band heights in
`CardGame70235View.vue` and in `PlayerSeat.vue` / `CardHand.vue` / `PlayingCard.vue`:

```
64  layout header                     387  .my-area
24  .app-content padding               ├─   9  padding + border
56  card head                          ├─ 132  your own seat, with its played row
48  card body padding                  ├─  92  hand
134  seat band                         ├─  18  reserved second-click hint
24  status bar                         ├─  70  actions row + auto-play row
387  .my-area                          ├─  18  auto-play hint
300  trick band                        └─  48  five 12px gaps
48  three 16px gaps
────
1085px
```

A maximised 1080p browser gives roughly 940px of viewport. So the page scrolled on every
screen anybody actually has — more on a laptop, and the previous round's phone rules left a
little scroll too, because they subtracted a hand-counted 150px of chrome that was short by
the footer's ~70px.

## What this does

### 1. An opt-in immersive mode — `front/src/utils/immersive.ts` (new)

A module-level `ref`, following `utils/theme.ts` rather than `stores/` (this is UI state, not
API-backed state). `useImmersive()` returns a setter and registers an `onUnmounted` that
clears the flag, so a view cannot leave the shell locked behind it.

`MainLayout` reads the ref: drops `min-height: 100vh` for `height: 100dvh; overflow: hidden`,
hides the footer, and turns `.app-content` into a `flex: 1 1 auto; min-height: 0` box with its
vertical padding halved to 6px and the margin and radius dropped. The 16px at the sides stays
— the table is a centred box narrower than the window, so side padding is free.

Nothing outside a live table is affected: every other page keeps the growing document.

The view arms it on `[inGame, gameOver]`, not on entering the route. The lobby is a list that
should be allowed to scroll, a waiting table would just be stretched, and the result state
keeps it because the result is drawn inside the same box as the trick.

### 2. The height reaches the table

Each box between the view and `.table-area` becomes a column flex item that can shrink: the
view, the card, and `.ant-card-body` (which needs `:deep`, scoped styles do not reach it).
Every step needs `min-height: 0`, or a flex item in a column stops at its content height and
the overflow reappears at the bottom of the window.

`.table-area` then gets `height: 100%` and `justify-content: space-between`. The bands around
the trick keep the fixed heights that stop the centre of the table moving mid-game; the trick
band is the one that gives, with `flex: 0 1 var(--trick-height)` — it shrinks from its natural
300px on a short window and never grows past it on a tall one. Slack on a tall window goes
into the gaps instead, which puts the seats at the top of the screen and the hand at the
bottom.

This replaces the mobile `calc(100dvh - 150px)`, so the residual phone scroll goes with it.

### 3. Height tiers, because the constraint is height

The compressions that make the table fit already existed — in the `max-width: 768px` block.
A 1366x768 laptop is a desktop by width and shorter than a phone held in landscape, so it
missed every one of them. The phone block stays keyed on width and owns everything below
769px; three new `min-width: 769px` blocks are keyed on height and own everything above it.

Each tier gives up the next-cheapest thing, and **each breakpoint is set to that tier's own
measured threshold**, so a tier is never selected for a window it cannot fit:

| tier | gives up | fixed | trick floor | needs |
|---|---|---|---|---|
| 1 | your own seat's played row; the auto-play hint; the two control rows share a line | 457px | 230px | **827px** |
| 2 | the raise history; the second-click hint | 409px | 190px | **739px** |
| 3 | the played row on the opponents' seats; a few px everywhere | 311px | 144px | **595px** |

Tier 1's cuts are close to free. Your own played row is a duplicate of the hand 92px below it;
the auto-play hint is a sentence about a switch whose state is visible in the switch; the
switch reads fine beside the buttons it governs. Tier 2 gives up the dimmed trail of plays
already superseded, keeping the current play and who made it. Tier 3 gives up which card on
the table came from which seat — recoverable from the current play, still in the middle.

Result:

```
window                 tier  trick  history  outcome
1440p maximised           1    300       98  fits
1080p maximised           1    300       98  fits
1080p windowed            1    300       98  fits
MacBook Pro 14"           1    245       43  fits
MacBook Air 13"           2    211        -  fits
1280x800 laptop           3    209        -  fits
1366x768 laptop           3    177        -  fits
half-height window        3    144        -  scrolls by 55px
```

Below 595px of viewport `.table-area` scrolls rather than clips, the same trade the phone
rules make below a first-generation SE: a short scroll is much better than a Play button cut
off below the fold.

## CSS specificity

The base immersive rules are not inside a media query, so they outrank the phone block at
equal specificity by being... equal and earlier. Two consequences, both handled:

- the phone block re-asserts `.trick` as `.trick, .card-game-view.is-immersive .trick` so its
  measured 110px floor and `flex: 1 1 auto` win there;
- the phone block resets its own negative margin under `.is-immersive`, because immersive mode
  sets the content padding itself and the reclaim would pull the card off the screen.

## Round 2: the budget was the wrong tool

The arrangement above still showed a scrollbar in play and clipped content at game over. Two
causes, and the first one explains why the title row mattered.

### The scrollbar inside the trick area

`.history-cards` is `transform: scale(0.72)`, and **a transform does not affect layout**. Each
history row therefore reserved the unscaled 54px of a small card while drawing 39px of it.
Three rows — `HISTORY_LIMIT` — needed 189px of content inside a band fixed at 98px with
`overflow-y: auto`. That is a scrollbar on any trick with more than one earlier play, and the
comment calling the scroll "a fallback for a row that wraps, not the normal case" was simply
wrong about its own code.

It is also why removing the title row appeared to fix things: there were two scrollbars. The
outer one was marginal, so the title row's ~44px was enough to tip it; the band's was not, and
at game over the band is replaced by the result block, which is why that one vanished there.

Fixed by giving `.history-cards` and `.history-row` the height they actually draw (39px) and
sizing the band to hold exactly three of them (144px, `overflow: hidden`). `--trick-height`
goes to 346px to match.

### The budget itself

Fitting the table by subtracting band heights from the viewport requires the sum to agree with
the card's title row, the layout header, two layers of padding, the browser's font size, the
user's zoom and whichever locale's text wraps. It was wrong twice. It is now replaced:

- **`utils/fitScale.ts`** measures the space with a `ResizeObserver` and scales the table by
  `min(1, available / natural)`. `offsetHeight` is the unscaled layout height, so measuring
  after scaling returns the same number and there is no feedback loop. The guarantee is
  structural, not arithmetic.
- `.table-fit` is the measured box, `.table-area` keeps its natural height inside it. Every
  `overflow-y: auto` in the immersive table is gone, so a scrollbar has nowhere to come from.
- The root document is locked (`html.is-immersive` in `style.css`). `#app` is `min-height:
  100svh` and the layout asked for `100dvh`; on a phone those differ by the URL bar's height,
  which put a scrollbar on the document that no rule inside the layout could reach. The layout
  now asks for `svh` too, and the root lock makes it moot either way.
- The height tiers stay, with a changed job: they decide how much is on the table so the scale
  stays near 1, rather than dividing up the viewport. Breakpoints are where the tier above
  would have to scale past ~0.9.

Resulting scale: 1.0 from 1440p down to a 1080p maximised window (0.996), 1.0 on a 14" MBP,
13" Air, 1280x800 and 1366x768, 0.879 on a half-height window, 0.571 on a 400px one. Phones
are 1.0 down to an iPhone 12, 0.873 on an SE 2/3, 0.374 in landscape. Nothing scrolls or clips
at any of them.

### The clipping at game over

Three bands have a fixed height with their overflow clipped, which is right during play — it
is what stops a seat picking up a tag and moving the middle of the table under the cursor —
and wrong at the moment the game ends, because that is when those bands get *more* in them:
the seats gain a placing and the winner a trophy, the status line gains a sentence about who
won, and the trick area is replaced by the result block. `.is-over` releases the fixed heights
on the seat row, the seats themselves, the status bar and the result box. Nothing is moving any
more, and the scale absorbs the growth.

## Verified

- `npx vue-tsc -b` — exit 0.
- `npm run build` — exit 0.
- `\tmp\check_immersive_final.py` — the scale each window size gets, per tier. It no longer
  answers "does it fit", since the scale makes that unconditional; it answers how much the
  board is shrunk to get there, which is what the tiers exist to keep near 1.
- `\tmp\check_immersive_css.py` — structural, because built CSS is unreadable on this machine
  (`dist/` files come back encrypted). Asserts: brace balance; that no box in the immersive
  table has `overflow-y: auto`; that `.table-fit` clips and `.table-area` keeps its natural
  height; that every `.is-over` rule releases a height rather than fixing one; that the three
  tiers exist at their own thresholds with `--trick-height` `[346, 150, 190, 144]` in source
  order and no leftover shrink floors; that the layout uses `svh` not `dvh`; that the root
  lock is in `style.css` and toggled from `immersive.ts`; that `fitScale.ts` measures
  `offsetHeight` (not `scrollHeight`, which the clip affects) and never scales up.
- `\tmp\compile_scoped.mjs` — compiles the view's scoped CSS through the real `@vue/compiler-sfc`
  and prints the selectors. Written to test whether a `:deep()` rule for the card head was
  compiling to something that could not match; it was not, but the tool now guards every
  `:deep()` in the file as part of the check above.

## Not verified

Rendered appearance. There is no browser automation in this repo. What has changed is that the
no-scroll property no longer depends on these numbers being right — it comes from a runtime
measurement and a clip. The numbers only predict the scale.

Worth a real look:

- a maximised 1080p window, where the scale is 0.996 — i.e. the table is very close to needing
  no scaling at all, and a browser with a slightly taller toolbar will shrink it a few percent;
- a phone in landscape, the worst case at 0.374;
- game over on a 5-seat table with long names, which is the case the `.is-over` releases exist
  for and the one most likely to find another clipped band.

The tier boundaries only re-evaluate on resize, which is correct — but a window dragged across
862px mid-game will show the history band appear or vanish.

## Round 3: the game-over display is the result, not the game

> At the end of the game, we only need to display each player's score and remaining cards,
> right? There is no need to show other information, such as whether a player was in auto-play
> mode. On the game table, we only need to display the winner and their score.

The finished table was still dressed as a running one. Every seat carried the state it had
while the game was live — who had passed, who had handed their turns to the server, the cards
they played in the last trick — and the middle of the table carried three lines where one was
being read. None of it is an outcome.

### What each seat shows now

A seat keeps its placing, avatar, name, remaining cards and score. Everything that described
how the game was being played is gated on a new `over` prop:

| Dropped at game over | Why |
| --- | --- |
| the auto-play tag | named in the request: how a seat was played, not how it finished |
| the passed / out-of-cards tag | both are read off the card count once the game is over |
| the played-cards row | the last trick's cards: a step in a finished game |

`passed` was really showing: the server clears it at the start of a trick, so whoever passed
in the last one still carried the tag. `out` was already gated on the game being active
server-side, so that half was moot.

One more piece of live state was on the seats and is not a tag: the green `is-leading`
background, which marks whoever holds the play on the table. It comes from
`currentPlayUsername`, and the normal end of a game clears the trick before scoring it — but a
game closed by the round cap is scored mid-trick (`finish` is reached from the timeout sweep,
not from `scoreTrick`), so that field survives into `FINISHED` and the highlight outlived the
game it described. Both seats now gate `leading` on `inGame`, matching `active`.

The placing number and the winner's trophy stay. They are the result — the standings the
scores are in — rather than state from the running game, and with the first-out tag gone the
placing is the only thing that reports the finishing order. Say so if they should go too.

### What the middle of the table shows now

The winner and their score. `firstOutWas` ("X went out first and collected the leftover
points") and `resultOnSeats` ("final scores and placings are shown on each seat") are gone,
along with the `firstOut` computed in the view, the `firstOut` tag, and all three locale keys
in both languages. `statusText` still renders in the no-winner branch, which is the only thing
that reports an abandoned or cancelled table.

The status bar is no longer rendered at game over at all. At `FINISHED` its text is the same
`winnerIs` sentence the result block above it carries, and both of its tags are gated on the
game running — so it was the headline repeated back into an otherwise empty bar.

### The heights had to go the other way

The `.is-over` rules were written for a display that *grew* when the result arrived: they
released each fixed height but kept a floor under it. The trimmed display shrinks instead, so
every floor was reserving space for content that no longer exists — `--seat-band-height` holds
a played row the seats do not draw, and `--trick-height` is three bands of cards where there
is now one line. All three floors are gone; `.seat-main` keeps its minimum, which the two
remaining lines actually reach.

That leaves the finished table well short of its box, so it is centred in the space rather
than sitting at the top with the slack beneath it. Gated on two things: `.is-over`, because
during play the table is near the height of the box and centring one that is briefly a little
too tall would clip its top as well as its bottom; and a new `.fits-unscaled` class, because
`transform-origin: top center` draws a scaled table from the top of its box — centring the box
would move the gap above the board instead of removing it.

Per tier, playing vs finished natural height:

| tier | playing | finished | shorter by |
| --- | --- | --- | --- |
| 1 everything shown | 803 | 417 | 386px |
| 2 no history, no hints | 599 | 375 | 224px |
| 3 seats collapsed, tighter | 455 | 349 | 106px |
| phone | 481 | 327 | 154px |

So the trim cannot bring a scrollbar back, and on a window where play was scaled to 0.946 the
result arrives at 1.0 — the board gets slightly larger as the game ends.

### Verified

- `npx vue-tsc -b` and `npm run build`: exit 0.
- `check_immersive_css.py`: the previous structural checks, plus the game-over trim. It now
  reads the raw sources as well as the style block, since most of the trim is `v-if` gates.
- `prove_checks.py` (new): reverts each piece of the trim in a copy of the source and runs the
  checker against it. 8/8 mutations caught, sources restored and re-verified. A check that
  cannot fail is not a check, and the previous round's checker passed while the bug was live.
- `compile_scoped.mjs`: all five `.is-over` rules compile with the scope attribute somewhere
  it can match, including the new `.is-over.fits-unscaled .table-fit`.
- The backend: hands are not cleared at `FINISHED`, so `handCount` is a real remaining-card
  count at game over and the seats can lean on it.

Still not verified by a browser, same as the previous rounds. The case worth a look is game
over on a 5-seat table with long names.
