import type { Card, CardShape } from '../api/cardGame'

/**
 * Display and legality helpers for 70235, mirroring the server's `CardDeck`. These exist only
 * so the UI can render cards and disable the Play button on a selection the server would
 * reject — the server remains the authority on every play.
 */

/** Rank characters weakest to strongest; the index is the strength rank. */
const RANK_ORDER = '4689TJQKA532X7'

/**
 * How each rank character is displayed. The joker falls back to "Joker" here; the UI prefers
 * the localised label (`cardGame.joker`), so this value only shows up outside a component.
 */
const RANK_LABELS: Record<string, string> = {
  '4': '4',
  '6': '6',
  '8': '8',
  '9': '9',
  T: '10',
  J: 'J',
  Q: 'Q',
  K: 'K',
  A: 'A',
  '5': '5',
  '3': '3',
  '2': '2',
  X: 'Joker',
  '7': '7',
}

const SUIT_SYMBOLS: Record<string, string> = {
  S: '♠',
  H: '♥',
  D: '♦',
  C: '♣',
}

export function rankChar(card: Card): string {
  return card.charAt(0)
}

export function suitChar(card: Card): string {
  return card.charAt(1)
}

export function rankOf(card: Card): number {
  return RANK_ORDER.indexOf(rankChar(card))
}

export function isJoker(card: Card): boolean {
  return rankChar(card) === 'X'
}

/** The big joker, as opposed to the little one. Cosmetic only — they rank equally. */
export function isBigJoker(card: Card): boolean {
  return isJoker(card) && suitChar(card) === 'B'
}

export function rankLabel(card: Card): string {
  return RANK_LABELS[rankChar(card)] ?? rankChar(card)
}

/** Suit symbol, or an empty string for a joker (which has no suit). */
export function suitSymbol(card: Card): string {
  return isJoker(card) ? '' : (SUIT_SYMBOLS[suitChar(card)] ?? '')
}

/** Hearts and diamonds are red; jokers are red too, by convention. */
export function isRed(card: Card): boolean {
  return isJoker(card) || suitChar(card) === 'H' || suitChar(card) === 'D'
}

/** Point value: 5 scores 5, 10 and K score 10, everything else nothing. */
export function pointsOf(card: Card): number {
  const rank = rankChar(card)
  if (rank === '5') return 5
  if (rank === 'T' || rank === 'K') return 10
  return 0
}

export function totalPoints(cards: Card[]): number {
  return cards.reduce((sum, card) => sum + pointsOf(card), 0)
}

/** A legal play: its shape, and the rank comparisons use. */
export interface DetectedPlay {
  shape: CardShape
  rank: number
}

/**
 * Identify the play a selection forms, or null if it is not a legal one. Mirrors the server:
 * a single; two, three or four of one rank; a full house (triple + a real pair, compared on the
 * triple); or four of a kind + any one kicker (compared on the quad).
 */
export function detectShape(cards: Card[]): DetectedPlay | null {
  if (!cards.length) return null
  if (cards.some(card => rankOf(card) < 0)) return null
  const counts = new Map<number, number>()
  for (const card of cards) {
    const rank = rankOf(card)
    counts.set(rank, (counts.get(rank) ?? 0) + 1)
  }
  if (cards.length === 1) {
    return { shape: 'SINGLE', rank: rankOf(cards[0]) }
  }
  // Two, three and four of one rank are each a shape in their own right. One rank across the
  // whole selection is the check; mixed ranks at these sizes are not a play.
  if (cards.length >= 2 && cards.length <= 4) {
    if (counts.size !== 1) return null
    const shape: CardShape = cards.length === 2 ? 'PAIR' : cards.length === 3 ? 'TRIPLE' : 'QUAD'
    return { shape, rank: rankOf(cards[0]) }
  }
  if (cards.length === 5) {
    // A quad takes any fifth card as a kicker.
    const quad = highestRankWithAtLeast(counts, 4)
    if (quad !== null) return { shape: 'QUAD_PLUS_ONE', rank: quad }
    // Three plus two: the two must be a genuine pair. With no rank appearing four times, five
    // cards over exactly two ranks can only be a triple and a pair, so the count is the check.
    const triple = highestRankWithAtLeast(counts, 3)
    if (triple !== null && counts.size === 2) return { shape: 'FULL_HOUSE', rank: triple }
  }
  return null
}

function highestRankWithAtLeast(counts: Map<number, number>, n: number): number | null {
  let best: number | null = null
  counts.forEach((count, rank) => {
    if (count >= n && (best === null || rank > best)) best = rank
  })
  return best
}

/**
 * Whether a selection may be played right now: a legal shape, and on a fresh trick anything
 * goes, otherwise the same shape at a strictly higher rank. Shapes never beat each other, so
 * four of a kind + one does not take a full house.
 */
export function canPlay(cards: Card[], currentShape: CardShape | null, currentRank: number | null): boolean {
  const play = detectShape(cards)
  if (!play) return false
  if (!currentShape || currentRank === null) return true
  return play.shape === currentShape && play.rank > currentRank
}

/** The rank of the play on the table, derived from its cards (the server sends the cards). */
export function rankOfPlay(cards: Card[] | null): number | null {
  if (!cards || !cards.length) return null
  return detectShape(cards)?.rank ?? null
}

/** Sort strongest first, the order hands are shown in. */
export function sortByStrength(cards: Card[]): Card[] {
  return [...cards].sort((a, b) => rankOf(b) - rankOf(a))
}
