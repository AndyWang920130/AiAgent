import { ref } from 'vue'
import {
  releaseNoteApi,
  type ReleaseNoteDTO,
  type ReleaseNoteSaveRequest,
  type ReleaseNoteStatus,
} from '../api/releaseNote'

// ----- The About section's release notes ---------------------------------------------------
//
// One list serves both roles. `fetchNotes` is told which endpoint to read, because an
// administrator's page includes drafts and everybody else's does not — the two differ only in
// where the rows come from, so they share everything below.

export const notes = ref<ReleaseNoteDTO[]>([])
export const total = ref(0)
export const loading = ref(false)
export const page = ref(1)
export const pageSize = ref(20)

/**
 * Load one page. `asAdmin` picks the endpoint; it only decides which URL is called, and the
 * server decides what it is willing to return — a non-administrator calling the admin list
 * gets a 403, not a filtered page.
 */
export async function fetchNotes(asAdmin = false) {
  loading.value = true
  try {
    const result = asAdmin
      ? await releaseNoteApi.adminList(page.value - 1, pageSize.value)
      : await releaseNoteApi.list(page.value - 1, pageSize.value)
    notes.value = result.items
    total.value = result.total
  } finally {
    loading.value = false
  }
}

/** Create or update, then reload so ordering and the resolved author name come from the server. */
export async function saveNote(id: number | null, data: ReleaseNoteSaveRequest) {
  if (id == null) {
    await releaseNoteApi.create(data)
    // A new note may sort anywhere by release date, so go back to the first page to find it.
    page.value = 1
  } else {
    await releaseNoteApi.update(id, data)
  }
  await fetchNotes(true)
}

export async function setNoteStatus(id: number, status: ReleaseNoteStatus) {
  await releaseNoteApi.setStatus(id, status)
  await fetchNotes(true)
}

export async function removeNote(id: number) {
  await releaseNoteApi.remove(id)
  // Deleting the last row of a page would otherwise leave an empty list on a page that no
  // longer exists.
  if (notes.value.length === 1 && page.value > 1) {
    page.value -= 1
  }
  await fetchNotes(true)
}
