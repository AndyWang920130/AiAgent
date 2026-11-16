<script lang="ts" setup>
// The About section's release notes. One route serves both roles: everybody reads the
// published notes as a timeline, and an administrator additionally sees drafts and the
// controls to write, publish and delete.
//
// The role is read here rather than enforced by the router, because this page is not an admin
// page — it is a public page with extra controls. `meta.requiresAdmin` would lock everybody
// else out of the thing they are meant to read. The server is the real boundary: the write
// endpoints sit under /api/v1/admin, which SecurityConfig restricts to ROLE_ADMIN, so hiding
// a button here is a courtesy and not the check that matters.
import { computed, onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Modal, message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import RichEditor from '../components/RichEditor.vue'
import type { ReleaseNoteDTO, ReleaseNoteStatus } from '../api/releaseNote'
import {
  notes,
  loading,
  page,
  pageSize,
  total,
  fetchNotes,
  removeNote,
  saveNote,
  setNoteStatus,
} from '../stores/releaseNote'
import { getUser } from '../utils/auth'

const { t } = useI18n()

const isAdmin = computed(() => getUser<{ role?: string }>()?.role === 'ADMIN')

const modalOpen = ref(false)
const saving = ref(false)
/** The note being edited, or null when the modal is creating a new one. */
const editingId = ref<number | null>(null)

const form = reactive({
  version: '',
  title: '',
  /** `YYYY-MM-DD` from the native date input; converted to an instant on save. */
  releasedDate: '',
  status: 'DRAFT' as ReleaseNoteStatus,
  content: '',
})

onMounted(() => fetchNotes(isAdmin.value))

function openCreate() {
  editingId.value = null
  form.version = ''
  form.title = ''
  form.releasedDate = todayInputValue()
  form.status = 'DRAFT'
  form.content = ''
  modalOpen.value = true
}

function openEdit(note: ReleaseNoteDTO) {
  editingId.value = note.id
  form.version = note.version
  form.title = note.title || ''
  form.releasedDate = toInputValue(note.releasedDate)
  form.status = note.status
  form.content = note.content
  modalOpen.value = true
}

async function handleSave() {
  if (!form.version.trim()) {
    return message.warning(t('releaseNote.versionRequired'))
  }
  if (!form.releasedDate) {
    return message.warning(t('releaseNote.dateRequired'))
  }
  // The editor sends `<p></p>` for an empty body, which is not an empty string. Same guard as
  // BlogEditView. The server re-checks it.
  if (!form.content || form.content === '<p></p>') {
    return message.warning(t('releaseNote.contentRequired'))
  }

  saving.value = true
  try {
    await saveNote(editingId.value, {
      version: form.version.trim(),
      title: form.title.trim() || undefined,
      content: form.content,
      releasedDate: toInstant(form.releasedDate),
      status: form.status,
    })
    message.success(editingId.value == null ? t('releaseNote.created') : t('releaseNote.updated'))
    modalOpen.value = false
  } catch (e: unknown) {
    // A duplicate version is the one failure worth naming — it is the only way a well-formed
    // save is refused, and "save failed" would send somebody looking for the wrong problem.
    const detail = (e as { response?: { data?: { errorKey?: string } } })?.response?.data?.errorKey
    message.error(detail === 'versionexists' ? t('releaseNote.versionExists') : t('releaseNote.saveFailed'))
  } finally {
    saving.value = false
  }
}

async function togglePublished(note: ReleaseNoteDTO) {
  const next: ReleaseNoteStatus = note.status === 'PUBLISHED' ? 'DRAFT' : 'PUBLISHED'
  try {
    await setNoteStatus(note.id, next)
    message.success(next === 'PUBLISHED' ? t('releaseNote.published') : t('releaseNote.withdrawn'))
  } catch {
    message.error(t('releaseNote.saveFailed'))
  }
}

function confirmDelete(note: ReleaseNoteDTO) {
  Modal.confirm({
    title: t('releaseNote.deleteTitle'),
    content: t('releaseNote.deleteContent', { version: note.version }),
    okText: t('releaseNote.delete'),
    okType: 'danger',
    cancelText: t('releaseNote.cancel'),
    onOk: async () => {
      try {
        await removeNote(note.id)
        message.success(t('releaseNote.deleted'))
      } catch {
        message.error(t('releaseNote.deleteFailed'))
      }
    },
  })
}

function onPageChange(next: number) {
  page.value = next
  fetchNotes(isAdmin.value)
}

/** A release date is a date, so it is shown as one — no time of day. */
function formatReleaseDate(value?: string | null) {
  if (!value) return ''
  return new Date(value).toLocaleDateString()
}

function formatEditedAt(value?: string | null) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}

/** `Date` → `YYYY-MM-DD` in local time, which is what a native date input wants. */
function toInputValue(value: string) {
  const d = new Date(value)
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

function todayInputValue() {
  return toInputValue(new Date().toISOString())
}

/**
 * `YYYY-MM-DD` → an ISO instant for the backend's `Instant`. Parsed as local midnight rather
 * than UTC, so the date somebody picked is the date that is stored in their own timezone.
 */
function toInstant(value: string) {
  const [year, month, day] = value.split('-').map(Number)
  return new Date(year, month - 1, day).toISOString()
}
</script>

<template>
  <div class="release-notes-view">
    <a-page-header :title="t('releaseNote.pageTitle')" :sub-title="t('releaseNote.pageSubtitle')">
      <template #extra>
        <a-button v-if="isAdmin" type="primary" @click="openCreate">
          <template #icon><PlusOutlined /></template>
          {{ t('releaseNote.newRelease') }}
        </a-button>
      </template>
    </a-page-header>

    <a-card :bordered="false" class="notes-card">
      <a-spin :spinning="loading">
        <a-empty
          v-if="!notes.length"
          :description="isAdmin ? t('releaseNote.emptyAdmin') : t('releaseNote.empty')"
          style="padding: 32px 0"
        />
        <a-timeline v-else>
          <a-timeline-item
            v-for="note in notes"
            :key="note.id"
            :color="note.status === 'PUBLISHED' ? 'blue' : 'gray'"
          >
            <div class="note-head">
              <span class="note-version">{{ note.version }}</span>
              <span class="note-date">{{ formatReleaseDate(note.releasedDate) }}</span>
              <a-tag v-if="note.status === 'DRAFT'" color="orange">
                {{ t('releaseNote.statuses.DRAFT') }}
              </a-tag>
            </div>

            <div v-if="note.title" class="note-title">{{ note.title }}</div>

            <!-- Administrator-authored HTML from the rich editor, rendered the same way blog
                 content is (BlogDetailView). Only an administrator can write one. -->
            <div class="note-body" v-html="note.content" />

            <div v-if="isAdmin" class="note-admin">
              <a-space :size="4" wrap>
                <a-button type="link" size="small" @click="openEdit(note)">
                  {{ t('releaseNote.edit') }}
                </a-button>
                <a-button type="link" size="small" @click="togglePublished(note)">
                  {{ note.status === 'PUBLISHED' ? t('releaseNote.withdraw') : t('releaseNote.publish') }}
                </a-button>
                <a-button type="link" size="small" danger @click="confirmDelete(note)">
                  {{ t('releaseNote.delete') }}
                </a-button>
              </a-space>
              <span class="note-meta">
                {{ t('releaseNote.editedBy', { name: note.authorName }) }}
                <template v-if="note.lastModifiedDate">
                  · {{ formatEditedAt(note.lastModifiedDate) }}
                </template>
              </span>
            </div>
          </a-timeline-item>
        </a-timeline>

        <div v-if="total > pageSize" class="notes-pagination">
          <a-pagination
            :current="page"
            :page-size="pageSize"
            :total="total"
            :show-size-changer="false"
            @change="onPageChange"
          />
        </div>
      </a-spin>
    </a-card>

    <!-- Editing happens in a modal rather than its own route: an entry is a handful of fields,
         and the list is where somebody is already working when they decide to change one. -->
    <a-modal
      v-model:open="modalOpen"
      :title="editingId == null ? t('releaseNote.newRelease') : t('releaseNote.editRelease')"
      :confirm-loading="saving"
      :ok-text="t('releaseNote.save')"
      :cancel-text="t('releaseNote.cancel')"
      width="760px"
      @ok="handleSave"
    >
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="t('releaseNote.version')" required>
              <a-input v-model:value="form.version" :placeholder="t('releaseNote.versionPlaceholder')" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('releaseNote.releasedDate')" required>
              <!-- A native date input: the app has no date picker anywhere yet, and
                   ant-design-vue's needs dayjs, which is only present transitively today. -->
              <a-input v-model:value="form.releasedDate" type="date" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('releaseNote.status')">
              <a-radio-group v-model:value="form.status" option-type="button" button-style="solid">
                <a-radio-button value="DRAFT">{{ t('releaseNote.statuses.DRAFT') }}</a-radio-button>
                <a-radio-button value="PUBLISHED">{{ t('releaseNote.statuses.PUBLISHED') }}</a-radio-button>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('releaseNote.title')">
          <a-input v-model:value="form.title" :placeholder="t('releaseNote.titlePlaceholder')" />
        </a-form-item>

        <a-form-item :label="t('releaseNote.content')" required>
          <RichEditor v-model="form.content" :placeholder="t('releaseNote.contentPlaceholder')" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.notes-card {
  border-radius: 10px;
}
.note-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.note-version {
  font-size: 16px;
  font-weight: 600;
  /* Version numbers line up down the timeline instead of drifting with digit widths. */
  font-variant-numeric: tabular-nums;
}
.note-date {
  font-size: 13px;
  opacity: 0.6;
}
.note-title {
  margin-top: 2px;
  font-size: 14px;
  font-weight: 500;
}
.note-body {
  margin-top: 8px;
  line-height: 1.8;
  font-size: 14px;
}
/* The body is editor HTML, so the same handful of element styles blog content needs. */
.note-body :deep(ul),
.note-body :deep(ol) {
  padding-left: 22px;
  margin-bottom: 0;
}
.note-body :deep(p) {
  margin-bottom: 6px;
}
.note-body :deep(p:last-child) {
  margin-bottom: 0;
}
.note-body :deep(code) {
  padding: 1px 5px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-size: 13px;
}
.note-body :deep(pre) {
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0.06);
  overflow-x: auto;
}
.note-body :deep(img) {
  max-width: 100%;
}
.note-admin {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 6px;
}
.note-meta {
  font-size: 12px;
  opacity: 0.5;
}
.notes-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>

