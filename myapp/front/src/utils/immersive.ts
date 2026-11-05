import { onUnmounted, ref, watch } from 'vue'

/**
 * Whether the current view wants the whole viewport, with nothing scrolling.
 *
 * MainLayout normally lets the document grow past the window: the outer layout is
 * `min-height: 100vh` and the footer sits below the content, so there is always a page
 * scrollbar once a view is tall enough. That is right for pages that are lists.
 *
 * A view that is a fixed board — the 70235 table being the first — needs the opposite: the
 * shell sized to the window exactly, so the view can divide up the height that is actually
 * there. While this is on, MainLayout locks itself to the viewport, drops the content frame
 * and hides the footer.
 *
 * Opt in from the view with {@link useImmersive} rather than setting this directly, so the
 * flag is always cleared on the way out.
 */
export const immersive = ref(false)

/**
 * The document itself is locked too, not just the layout inside it.
 *
 * `#app` in style.css is `min-height: 100svh` with an inline border, and the layout inside it
 * asks for the viewport height — on a phone `dvh` and `svh` differ by the height of the URL
 * bar, and any ancestor that ends up a pixel taller than the window puts a scrollbar on the
 * document that no rule inside the layout can reach. Rather than keep every ancestor's height
 * in agreement by arithmetic, the scroll is turned off at the root while a board is on screen.
 *
 * This is the same thing a modal does while it is open, and it is what makes "no scrollbar" a
 * property of the page rather than a consequence of a sum coming out right.
 */
watch(immersive, on => {
  const root = document.documentElement
  root.classList.toggle('is-immersive', on)
})

/**
 * Bind immersive mode to a component's lifetime. Call {@code set(true|false)} as the view's
 * own state changes — a lobby does not want this, a table in play does — and it is turned
 * off when the component goes away, including by browser back.
 */
export function useImmersive() {
  onUnmounted(() => {
    immersive.value = false
  })
  return {
    set(on: boolean) {
      immersive.value = on
    },
  }
}
