# Drag-to-select cards in the 70235 hand

## What this is

Hold the left mouse button on a card and sweep across the hand to select everything the pointer
passes over, instead of clicking each card in turn. A pair is one short drag; a full house is one
longer one.

## Scope assumption, stated up front

"Items" is read as **the cards in the 70235 hand** (`CardHand.vue`). That is the only multi-select
surface in the app — I grepped `rowSelection|selectedRowKeys|v-model:checked|multiple` across
`views/` and `components/` and checked the four likeliest tables (`StockListView`, `UserListView`,
`BlogListView`, `DataIntegrationView`); none has row selection. The only other checkbox group is
the stock list's column-visibility panel (`StockListView.vue:442`), which is a settings popover
where a sweep buys nothing. If you meant a different surface, say so and I will replan — nothing
below is written yet.

Also decided as defaults, not asked:

- **Mouse only.** The request says "left mouse button". Touch keeps tap-to-select, so a finger
  drag on a phone stays free for scrolling the table.
- **The sweep paints by the origin card's state.** Starting on an unselected card adds the range;
  starting on a selected card removes it. Drag-to-deselect is the same gesture backwards, which is
  how a file manager behaves and means a wrong sweep is undone by repeating it.

## The one real hazard

`toggleCard` (`CardGame70235View.vue:573`) makes the *second* click on a selected card **play the
selection**. So a drag must never arrive as a click on a selected card, or sweeping back over where
you started would commit a play you did not ask for.

Two cases, and only the second needs code:

1. Press on card 2, sweep to card 5, release there. The browser fires `click` on the nearest common
   ancestor of press and release — the hand, not either slot — so no slot handler runs. Safe for
   free.
2. Press on card 2, sweep to card 5, come **back** to card 2, release. Press and release share a
   slot, so that slot's `click` fires, and card 2 is now selected mid-sweep. Without a guard this
   plays. So: once the pointer leaves the origin slot, the drag sets a flag that makes the next
   `click` a no-op. The flag is cleared on the next `pointerdown`, not after use, so a drag that
   ends elsewhere cannot eat an unrelated later click.

A press and release on one card with no movement past the threshold stays a plain click and keeps
its existing meaning, including play-on-second-click.

## Why a range, not "cards the pointer touched"

`pointermove` samples at the pointer's rate, not the pixel's. A fast flick across six cards reports
three or four positions and would select a sparse subset of what the hand visibly swept. So the
drag tracks only the **origin index and the current index** and selects `min..max` inclusive. Fast
and slow sweeps of the same distance select the same cards, and dragging back shrinks the range
live.

Two facts from the existing code make this sound:

- `detectShape` counts ranks into a Map (`utils/cards.ts:98-126`), so **selection order never
  affects legality**. The sweep can rebuild the array in index order and replace it wholesale.
- The hand is one sorted row, so a contiguous index range is exactly the geometric sweep.

Hit-testing uses each slot's `getBoundingClientRect()`, measured once at `pointerdown` (the hand
does not reflow mid-drag) and matched **last-match-wins**, because slots overlap by 14px and the
later sibling paints on top — so the match agrees with the card you can see under the pointer.
Vertical drift off the row keeps the last valid index rather than cancelling, so a sloppy arc still
selects the run.

## Changes

### 1. `front/src/components/cardgame/CardHand.vue`

The drag lives here because this component owns the slot geometry; the selection state stays in the
view. Added:

- `const emit = defineEmits<{ toggle: [index: number]; sweep: [indices: number[]] }>()`
- `onPointerDown(index, event)` on `.card-slot`, returning early unless
  `event.button === 0 && event.pointerType === 'mouse'` and `!props.disabled`. Calls
  `preventDefault()` to stop the native drag/text-selection ghost, snapshots `props.selected` and
  the slot rects, records the origin, and attaches `pointermove` / `pointerup` to `window`.
- `onPointerMove` resolves the index under the pointer, and once movement passes a 4px threshold or
  the index differs from the origin, marks the drag live, sets the suppress-click flag, and emits
  the recomputed selection. `event.buttons === 0` ends the drag — that is the button released
  outside the window.
- `endDrag()` removes both window listeners; also called from `onBeforeUnmount`, so a hand unmounted
  mid-drag (a deal lands, the table resets) leaves nothing attached to `window`.
- The emitted array is always `snapshot ± range`, computed from the **snapshot taken at
  pointerdown**, never from the live prop — otherwise each move would re-toggle what the previous
  move already toggled.

`onCardClick` gains one line: return early if the suppress flag is set.

### 2. `front/src/views/CardGame70235View.vue`

- `@sweep="selectedIndices = $event"` on `<CardHand>` (`:935`). A deliberately dumb handler: a sweep
  only ever changes the selection, and never calls `playSelection`. Committing stays the second
  click's job, so a drag cannot play.
- Comment on the handler recording that, since the next reader's instinct will be to make a sweep
  that lands on a legal shape play it.

### 3. Locales

`cardGame.clickAgainToPlay` (`en-US.ts:780`, `zh-CN.ts:778`) extends to mention the drag, because a
gesture with no affordance is a gesture nobody finds. The line already sits in a `min-height: 18px`
reserved block that is a floor, not a cap, so the longer text wraps on a phone the way the current
text already does and shifts nothing above it.

## Verification

Per the convention in `D:\tmp`, a new `check_drag_select.py` / `prove_drag_select.py` pair, then
registered in `prove_untouched.py` (`CardHand.vue` into `WATCHED`, the prover into `RUNNERS`).

The checks are the agreements a compiler cannot see, each one a rule a later edit could silently
break:

1. `sweep` is declared in `defineEmits` — an undeclared emit still fires, so nothing else would
   catch its removal from the type.
2. The pointerdown guard holds **both** `button === 0` and `pointerType === 'mouse'`.
3. `onCardClick` returns on the suppress flag — this is the "no accidental play" rule.
4. The suppress flag is reset in `pointerdown`.
5. The range is built with `Math.min`/`Math.max` from the origin, not from the hovered index alone.
6. The sweep recomputes from the pointerdown snapshot, not from `props.selected`.
7. `onBeforeUnmount` calls the same teardown as pointerup, and the import is present.
8. `buttons === 0` is handled in the move handler.
9. The view's `@sweep` handler exists and contains **no** `playSelection` (a rejection).
10. The drag words are in both locale files.
11. `disabled` still gates the drag, not only the click.

Each gets one mutation in the prover, including mutations that *reinstate* the forbidden thing —
`playSelection()` put into the sweep handler, the suppress-flag check deleted, the `pointerType`
guard dropped — because a rejection with no mutation is a rule nobody has watched fire.

Then `npx vue-tsc -b` and `npm run build -- --mode prod`.

## What I am not doing

- No touch/finger drag (not asked; would collide with scroll).
- No rubber-band marquee rectangle — the hand is one row, so a band is a range with extra code.
- No shift-click or ctrl-click range selection (not asked).
- No change to `playSelection`, `canPlay`, or anything server-side. The server stays the authority
  on every play; this is input only.

## Not verifiable from here

Whether the sweep feels right under a real mouse across the 14px card overlap. The geometry is
derived from the rects, so it should track the visible card, but the threshold (4px) is a judgement
I would want to feel once on a real table.
