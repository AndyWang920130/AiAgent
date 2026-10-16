import http from '../utils/axios'

/** Markets the backend can quote. The code format differs per market. */
export type StockMarket = 'SH' | 'SZ' | 'HK' | 'US'

/**
 * Wire shape of a tracked stock. The price fields are the backend's cached copy of the
 * most recent successful quote, so they can be null until the first refresh succeeds.
 */
export interface StockDTO {
  id: number
  market: StockMarket
  code: string
  name?: string | null
  currency?: string | null
  latestPrice?: number | null
  previousClose?: number | null
  openPrice?: number | null
  dayHigh?: number | null
  dayLow?: number | null
  priceChange?: number | null
  changePercent?: number | null
  volume?: number | null
  /** When this app fetched the quote (ISO instant), not the exchange's own clock. */
  lastQuoteTime?: string | null
  /** The provider's market timestamp, already formatted for display. */
  marketTimeText?: string | null
  lastQuoteError?: string | null
  quoteProvider?: string | null
  historyCount?: number
  /** When the current user added this stock to their own watch list. */
  addedDate?: string
}

/** One recorded price point for a stock. */
export interface StockPriceHistoryDTO {
  id: number
  stockId: number
  price: number
  priceChange?: number | null
  changePercent?: number | null
  volume?: number | null
  recordedDate: string
  marketTimeText?: string | null
}

export interface AddStockRequest {
  market: StockMarket
  code: string
}

export interface StockHistoryPage {
  items: StockPriceHistoryDTO[]
  total: number
}

export const stockApi = {
  list: () =>
    http.get('/api/v1/stocks').then(r => r.data as StockDTO[]),

  get: (id: number) =>
    http.get(`/api/v1/stocks/${id}`).then(r => r.data as StockDTO),

  add: (data: AddStockRequest) =>
    http.post('/api/v1/stocks', data).then(r => r.data as StockDTO),

  /** Pull fresh prices for every tracked stock; the backend records each new price. */
  refreshAll: () =>
    http.post('/api/v1/stocks/refresh').then(r => r.data as StockDTO[]),

  refreshOne: (id: number) =>
    http.post(`/api/v1/stocks/${id}/refresh`).then(r => r.data as StockDTO),

  /** Price history, newest first. Total count comes back in the X-Total-Count header. */
  history: (id: number, page = 0, size = 50) =>
    http
      .get(`/api/v1/stocks/${id}/history`, { params: { page, size } })
      .then(r => ({
        items: r.data as StockPriceHistoryDTO[],
        total: Number(r.headers['x-total-count'] ?? (r.data as StockPriceHistoryDTO[]).length),
      }) as StockHistoryPage),

  remove: (id: number) =>
    http.delete(`/api/v1/stocks/${id}`),
}
