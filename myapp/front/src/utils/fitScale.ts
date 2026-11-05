import { onBeforeUnmount, onMounted, ref, type Ref } from 'vue'

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

  function measure() {
    const box = viewport.value
    const inner = content.value
    if (!box || !inner) return

    const available = box.clientHeight - SAFETY_PX
    // offsetHeight is the unscaled layout height: transforms do not change it.
    const natural = inner.offsetHeight
    if (available <= 0 || natural <= 0) {
      scale.value = 1
      return
    }
    // Never scale up. A short table on a tall window should sit at its own size with the
    // slack around it, not be stretched into a poster.
    scale.value = Math.min(1, available / natural)
  }

  let ro: ResizeObserver | null = null

  onMounted(() => {
    measure()
    // Both boxes are watched: the viewport changes when the window does, and the content
    // changes on its own as the game runs — a hand of five cards is taller than a hand of
    // one, the result replaces the trick, a seat picks up a tag that wraps.
    ro = new ResizeObserver(measure)
    if (viewport.value) ro.observe(viewport.value)
    if (content.value) ro.observe(content.value)
    window.addEventListener('resize', measure)
  })

  onBeforeUnmount(() => {
    ro?.disconnect()
    ro = null
    window.removeEventListener('resize', measure)
  })

  return { scale, measure }
}
