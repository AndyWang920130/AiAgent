import { onBeforeUnmount, onMounted, ref, watch, type Ref } from 'vue'

/**
 * Scale a fixed-size block down until it fits the box it is in.
 *
 * Why this exists rather than a height budget: the 70235 table is a stack of fixed-height
 * bands, and fitting it on screen was first attempted by adding those heights up and
 * subtracting them from the viewport. That is fragile in a way that is invisible in review —
 * the sum has to agree with the card's title row, the header, two layers of padding, the
 * browser's font size, the user's zoom, and whichever locale's text wraps onto a second line.
 * Get any of it wrong by a pixel and a scrollbar appears.
 *
 * So the height is measured instead. The content keeps its natural size and is scaled by
 * `min(1, available / natural)`, which cannot exceed the space available — not because the
 * arithmetic was right but because the arithmetic is the measurement. A transform does not
 * affect layout, so measuring the content after scaling it returns the same natural height
 * and there is no feedback loop.
 *
 * Scaling down is a real cost: everything gets smaller together, text included. It is only
 * ever applied when the alternative is a scrollbar or a clipped button, and the view's own
 * height tiers drop the parts of the table that duplicate information first, so the scale
 * stays near 1 on ordinary windows.
 *
 * @param viewport the box to fit into — sized by the layout, usually the window's leftovers
 * @param content  the block to scale — sized by what is in it
 */
export function useFitScale(viewport: Ref<HTMLElement | null>, content: Ref<HTMLElement | null>) {
  const scale = ref(1)

  /** Guard against a fractional layout rounding to one pixel over and re-introducing a scrollbar. */
  const SAFETY_PX = 1

  /**
   * Below this, the box is not a layout — it is a measurement taken while one was still
   * happening, and the honest answer is "not yet" rather than a scale derived from it.
   *
   * The viewport this fits into is the window's leftovers under the app header, and the
   * caller's own breakpoints stop subdividing at 595px because below that there is nothing
   * left to give up. A box reporting less than this is therefore not a small window; it is a
   * flex item measured before the height it inherits had reached it — `.table-fit` is
   * `flex: 1 1 auto; min-height: 0`, which is exactly the configuration that reports a few
   * pixels when its ancestors' heights are not resolved yet. Acting on such a reading scales
   * the board to a sliver, and nothing necessarily measures again afterwards: the observer
   * only fires when a box *changes* size, so a reading that was wrong when it was taken and
   * is never retaken stays on screen for as long as the table does.
   */
  const MIN_BOX_PX = 120

  /**
   * And a floor under the result, for whatever this reasoning has not thought of. A table at
   * 0.35 is small but still a table; the arithmetic that produces anything below that has
   * lost its argument with the layout, and `.table-fit` clips the overflow — which is the
   * same thing that happens on a window too short for the smallest tier, and reads as a
   * cropped table rather than a broken one.
   */
  const MIN_SCALE = 0.35

  function measure() {
    const box = viewport.value
    const inner = content.value
    if (!box || !inner) return

    const available = box.clientHeight - SAFETY_PX
    // offsetHeight is the unscaled layout height: transforms do not change it.
    const natural = inner.offsetHeight
    if (natural <= 0) {
      scale.value = 1
      return
    }
    // Leave the scale as it is rather than deriving one from a box this size: a later
    // measurement (the next frame, or the observer when the real height lands) replaces it.
    if (available < MIN_BOX_PX) return
    // Never scale up. A short table on a tall window should sit at its own size with the
    // slack around it, not be stretched into a poster.
    scale.value = Math.max(MIN_SCALE, Math.min(1, available / natural))
  }

  let ro: ResizeObserver | null = null
  let frame = 0

  /**
   * Measure again on the next frame.
   *
   * `flush: 'post'` below puts the elements in the DOM, which is enough to measure them but
   * not enough to know that what is measured is final: the box the board fits into inherits
   * its height from the layout shell, which turns its own immersive mode on in response to
   * the same state change that mounted the board. The observer is not a backstop for this —
   * it reports a box that *changed*, and a box that was already being reported at its final
   * size never changes again. So one more look, one frame later, with the style and layout
   * work of this change all behind it.
   */
  function remeasureNextFrame() {
    cancelAnimationFrame(frame)
    frame = requestAnimationFrame(() => {
      frame = 0
      measure()
    })
  }

  /**
   * Observe whichever boxes currently exist, and re-observe when they change.
   *
   * Both of them are usually absent when this composable mounts. The caller is typically a
   * view whose board is behind a `v-if` on state it has not loaded yet — the 70235 table
   * waits on a poll, so what is in the DOM at mount is the lobby and both refs are null.
   * Observing once at mount therefore observed nothing at all, and the only thing left
   * driving a measurement was the window `resize` listener: the scale stayed at 1 however
   * tall the board turned out to be, and two clients on the same screen size could disagree
   * about it purely according to whether either had resized their window. This watcher is
   * what makes the fit a property of the layout rather than of that history.
   *
   * `flush: 'post'` so the elements are in the DOM and measurable when it runs.
   */
  watch(
    [viewport, content],
    ([box, inner]) => {
      ro?.disconnect()
      if (!box && !inner) return
      // Both boxes are watched: the viewport changes when the window does, and the content
      // changes on its own as the game runs — a hand of five cards is taller than a hand of
      // one, the result replaces the trick, a seat picks up a tag that wraps.
      ro = new ResizeObserver(measure)
      if (box) ro.observe(box)
      if (inner) ro.observe(inner)
      measure()
      remeasureNextFrame()
    },
    { immediate: true, flush: 'post' }
  )

  onMounted(() => {
    measure()
    remeasureNextFrame()
    window.addEventListener('resize', measure)
  })

  onBeforeUnmount(() => {
    ro?.disconnect()
    ro = null
    cancelAnimationFrame(frame)
    window.removeEventListener('resize', measure)
  })

  return { scale, measure }
}
