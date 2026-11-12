import { ref } from 'vue'
import {
  feedbackApi,
  type AdminFeedbackQuery,
  type FeedbackCategory,
  type FeedbackCreateRequest,
  type FeedbackDTO,
  type FeedbackReplyRequest,
  type FeedbackStatus,
} from '../api/feedback'

// ----- The submitter's own list ----------------------------------------------------------

export const myFeedbacks = ref<FeedbackDTO[]>([])
export const myTotal = ref(0)
export const myLoading = ref(false)
export const myPage = ref(1)
export const myPageSize = ref(10)

export async function fetchMyFeedbacks() {
  myLoading.value = true
  try {
    const result = await feedbackApi.listMine(myPage.value - 1, myPageSize.value)
    myFeedbacks.value = result.items
    myTotal.value = result.total
  } finally {
    myLoading.value = false
  }
}

export async function submitFeedback(data: FeedbackCreateRequest) {
  const created = await feedbackApi.submit(data)
  // A new submission is the newest row, so the list goes back to the first page to show it.
  myPage.value = 1
  await fetchMyFeedbacks()
  return created
}

export async function withdrawFeedback(id: number) {
  await feedbackApi.withdraw(id)
  await fetchMyFeedbacks()
}

// ----- The administrator's triage list ---------------------------------------------------

export const feedbacks = ref<FeedbackDTO[]>([])
export const total = ref(0)
export const loading = ref(false)
export const openCount = ref(0)

/** Current triage filters, kept here so the table and the filter bar stay in step. */
export const query = ref('')
export const statusFilter = ref<FeedbackStatus | undefined>(undefined)
export const categoryFilter = ref<FeedbackCategory | undefined>(undefined)
export const page = ref(1)
export const pageSize = ref(20)

export async function fetchFeedbacks() {
  loading.value = true
  try {
    const params: AdminFeedbackQuery = {
      query: query.value,
      status: statusFilter.value,
      category: categoryFilter.value,
      page: page.value - 1,
      size: pageSize.value,
    }
    const result = await feedbackApi.adminList(params)
    feedbacks.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** Apply a filter change: results move back to the first page. */
export async function applyFilters() {
  page.value = 1
  await fetchFeedbacks()
}

export async function fetchOpenCount() {
  openCount.value = await feedbackApi.openCount()
}

export async function replyToFeedback(id: number, data: FeedbackReplyRequest) {
  const updated = await feedbackApi.reply(id, data)
  patch(updated)
  // The reply moves the row's status, so the open-submission badge is refetched with it.
  await fetchOpenCount()
  return updated
}

export async function removeFeedback(id: number) {
  await feedbackApi.remove(id)
  feedbacks.value = feedbacks.value.filter(item => item.id !== id)
  total.value = Math.max(0, total.value - 1)
  await fetchOpenCount()
}

function patch(updated: FeedbackDTO) {
  const index = feedbacks.value.findIndex(item => item.id === updated.id)
  if (index !== -1) feedbacks.value[index] = updated
}
