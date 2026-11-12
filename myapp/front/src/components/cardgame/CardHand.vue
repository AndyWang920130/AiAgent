<script lang="ts" setup>
import { onBeforeUnmount, ref } from 'vue'
import type { Card } from '../../api/cardGame'
import PlayingCard from './PlayingCard.vue'

const props = withDefaults(
  defineProps<{
    cards: Card[]
    /** Indices of the selected cards. Indices, not card codes: with two packs a hand can hold
        two identical cards, and they must be selectable independently. */
    selected: number[]
    disabled?: boolean
  }>(),
  { disabled: false }
)

/**
 * `toggle` is one card, clicked. `sweep` is the whole resulting selection after a drag — the
 * component works it out here, where the slot geometry lives, and the parent only stores it.
 */
const emit = defineEmits<{ toggle: [index: number]; sweep: [indices: number[]] }>()

/* ----- Drag to select ------------------------------------------------------ */

/** Pixels the pointer must travel on one card before a press counts as a sweep, not a click.
    4 is what Windows itself uses for a drag (SM_CXDRAG), and a deliberate click rarely moves
    that far. Crossing into another slot starts a sweep whatever this says. */
const DRAG_THRESHOLD = 4

const hand = ref<HTMLElement | null>(null)

/* Snapshotted at pointerdown. The rects because the hand does not reflow mid-drag; the
   selection because every move recomputes the whole result from it — reading the live prop
   instead would re-toggle what the move before had just toggled. */
let originIndex = -1
let originX = 0
let originY = 0
let lastIndex = -1
let baseSelection: number[] = []
let slotRects: DOMRect[] = []
let dragging = false

/**
 * Set once a press has become a sweep, and read by the click handler.
 *
 * A drag that leaves the first card and comes back releases on the card it pressed, so the
 * browser fires a real `click` on that slot — and by then the card is selected, which is the
 * state the parent reads as "play this selection". Without this flag, sweeping back to where
 * you started would commit a play nobody asked for.
 *
 * Cleared on the next press rather than after one use: pointerup runs before click, so the flag
 * has to outlive the drag, and a drag that ends elsewhere must not swallow a later real click.
 */
let suppressClick = false

function onCardClick(index: number) {
  if (props.disabled || suppressClick) return
  emit('toggle', index)
}

function measureSlots(): DOMRect[] {
  const root = hand.value
  if (!root) return []
  return Array.from(root.querySelectorAll<HTMLElement>('.card-slot')).map(el =>
    el.getBoundingClientRect()
  )
}

/**
 * The card index under the pointer, or -1. Last match wins: slots overlap, and the later sibling
 * paints over the earlier one, so the last rect containing the point is the card you can actually
 * see there.
 */
function indexAt(x: number, y: number): number {
  let found = -1
  for (let i = 0; i < slotRects.length; i++) {
    const rect = slotRects[i]
    if (x >= rect.left && x <= rect.right && y >= rect.top && y <= rect.bottom) found = i
  }
  return found
}

/**
 * The selection a sweep from the first card to `current` produces.
 *
 * Every index between the two, not the cards the pointer was sampled over: pointermove reports at
 * the pointer's rate, not the pixel's, so a quick flick across six cards lands three or four
 * positions and would select a sparse subset of what the hand visibly swept. A range makes a fast
 * and a slow sweep of the same distance select the same cards, and dragging back shrinks it.
 *
 * The first card's state decides which way it paints: starting on an unselected card adds the
 * range, starting on a selected one removes it. Drag-to-deselect is then the same gesture
 * backwards, so a wrong sweep is undone by repeating it.
 */
function rangeSelection(current: number): number[] {
  const lo = Math.min(originIndex, current)
  const hi = Math.max(originIndex, current)
  const range: number[] = []
  for (let i = lo; i <= hi; i++) range.push(i)
  if (baseSelection.includes(originIndex)) {
    return baseSelection.filter(i => !range.includes(i))
  }
  const next = baseSelection.slice()
  for (const i of range) if (!next.includes(i)) next.push(i)
  return next
}

function onPointerDown(index: number, event: PointerEvent) {
  suppressClick = false
  // Left button, real mouse. Touch keeps tap-to-select: a finger drag on a phone belongs to
  // scrolling the table, and a sweep would steal it.
  if (props.disabled || event.button !== 0 || event.pointerType !== 'mouse') return
  // Stops the text-selection ghost the browser drags along. It does not stop the click that
  // follows, which is what the suppress flag is for.
  event.preventDefault()
  originIndex = index
  originX = event.clientX
  originY = event.clientY
  lastIndex = index
  baseSelection = props.selected.slice()
  slotRects = measureSlots()
  dragging = false
  window.addEventListener('pointermove', onPointerMove)
  window.addEventListener('pointerup', endDrag)
}

function onPointerMove(event: PointerEvent) {
  if (originIndex < 0) return
  // No buttons held: released outside the window, where no pointerup reaches us.
  if (event.buttons === 0) {
    endDrag()
    return
  }
  const at = indexAt(event.clientX, event.clientY)
  // Drift off the row keeps the last card swept rather than cancelling, so a sloppy arc still
  // selects the run it passed through.
  if (at >= 0) lastIndex = at
  const moved =
    Math.abs(event.clientX - originX) > DRAG_THRESHOLD ||
    Math.abs(event.clientY - originY) > DRAG_THRESHOLD
  if (!dragging && !moved && lastIndex === originIndex) return
  dragging = true
  suppressClick = true
  emit('sweep', rangeSelection(lastIndex))
}

function endDrag() {
  window.removeEventListener('pointermove', onPointerMove)
  window.removeEventListener('pointerup', endDrag)
  originIndex = -1
  lastIndex = -1
  dragging = false
  slotRects = []
  baseSelection = []
}

// A hand unmounted mid-drag — a deal lands, the table resets — must not leave listeners on the
// window holding a reference to this component.
onBeforeUnmount(endDrag)
</script>

<template>
  <div ref="hand" class="card-hand" :class="{ 'is-disabled': disabled }">
    <div
      v-for="(card, index) in cards"
      :key="`${card}-${index}`"
      class="card-slot"
      @click="onCardClick(index)"
      @pointerdown="onPointerDown(index, $event)"
    >
      <PlayingCard :card="card" :selected="selected.includes(index)" :disabled="disabled" />
    </div>
    <span v-if="!cards.length" class="empty">—</span>
  </div>
</template>

<style scoped>
.card-hand {
  display: flex;
  align-items: flex-end;
  /* Room above for the lift a selected card makes, and below for the shadow. */
  padding: 16px 0 6px;
  min-height: 92px;
}

.card-slot {
  cursor: pointer;
}

/* Cards overlap so a six-card hand fits a narrow screen; the last one shows in full. */
.card-slot + .card-slot {
  margin-inline-start: -14px;
}

.card-hand.is-disabled .card-slot {
  cursor: default;
}

.empty {
  color: rgba(0, 0, 0, 0.35);
}

/* Phones: shorter, because the cards themselves are smaller here (see PlayingCard.vue) and
   the whole table has to fit the screen. 10px of room above for a selected card's lift,
   38x54 of card, 4px below for the shadow. Cards overlap further so a six-card hand still
   fits the width. */
@media (max-width: 768px) {
  .card-hand {
    padding: 10px 0 4px;
    min-height: 68px;
  }
  .card-slot + .card-slot {
    margin-inline-start: -14px;
  }
}
</style>
