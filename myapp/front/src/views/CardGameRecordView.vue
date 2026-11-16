<script lang="ts" setup>
// The administrator's 70235 match record list: what was played, when it started and ended, who
// created it, and what every seat scored.
//
// Read-only on purpose. A record is written by the game itself as a match ends; there is no
// edit path to expose here, and correcting a result after the fact is not a thing to offer.
import { computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import type { CardGameRecordDTO, CardGameRecordOutcome } from '../api/cardGameRecord'
import {
  records,
  loading,
  page,
  pageSize,
  total,
  query,
  outcomeFilter,
  applyFilters,
  fetchRecords,
} from '../stores/cardGameRecord'

const { t, locale } = useI18n()

const outcomes: CardGameRecordOutcome[] = ['SCORED', 'ABANDONED']

const outcomeColor: Record<CardGameRecordOutcome, string> = {
  SCORED: 'green',
  ABANDONED: 'orange',
}

const outcomeOptions = computed(() =>
  outcomes.map(value => ({ value, label: t(`cardGameRecord.outcomes.${value}`) })),
)

const columns = computed(() => [
  { title: t('cardGameRecord.startedAt'), key: 'startedDate', width: 170 },
  { title: t('cardGameRecord.endedAt'), key: 'endedDate', width: 170 },
  { title: t('cardGameRecord.duration'), key: 'duration', width: 100 },
  { title: t('cardGameRecord.creator'), key: 'host', width: 140 },
  { title: t('cardGameRecord.scores'), key: 'scores' },
  { title: t('cardGameRecord.outcome'), key: 'outcome', width: 150 },
])

onMounted(fetchRecords)

function onTableChange(pagination: { current?: number; pageSize?: number }) {
  page.value = pagination.current || 1
  pageSize.value = pagination.pageSize || 20
  fetchRecords()
}

function resetFilters() {
  query.value = ''
  outcomeFilter.value = undefined
  applyFilters()
}

function formatDate(value?: string | null) {
  if (!value) return ''
  return new Date(value).toLocaleString()
}

/**
 * How long the match ran. Computed here rather than stored, because it is exactly the gap
 * between the two timestamps already on the row.
 *
 * <p>Shown to the minute once a match runs past one, which is every real game: seconds of
 * precision on a half-hour game is noise in a column being scanned.
 */
function formatDuration(record: CardGameRecordDTO) {
  const ms = new Date(record.endedDate).getTime() - new Date(record.startedDate).getTime()
  if (!Number.isFinite(ms) || ms < 0) return ''
  const seconds = Math.round(ms / 1000)
  if (seconds < 60) return t('cardGameRecord.durationSeconds', { count: seconds })
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return t('cardGameRecord.durationMinutes', { count: minutes })
  const hours = Math.floor(minutes / 60)
  return t('cardGameRecord.durationHours', { hours, minutes: minutes % 60 })
}

/** A seat's label: the display name, with the login kept for an administrator who needs it. */
function seatTitle(name: string, username: string) {
  return name === username ? username : `${name} (${username})`
}

/** Dates are rendered through the browser, so the column re-renders when the locale changes. */
const dateKey = computed(() => locale.value)
</script>

<template>
  <div class="card-game-record-view" :key="dateKey">
    <a-page-header
      :title="t('cardGameRecord.pageTitle')"
      :sub-title="t('cardGameRecord.pageSubtitle')"
    >
      <template #extra>
        <a-tag color="blue">{{ t('cardGameRecord.totalCount', { count: total }) }}</a-tag>
      </template>
    </a-page-header>

    <a-card :bordered="false" class="list-card">
      <a-form layout="inline" class="filter-form" @submit.prevent>
        <a-form-item>
          <a-input
            v-model:value="query"
            :placeholder="t('cardGameRecord.searchPlaceholder')"
            style="width: 220px"
            allow-clear
            @press-enter="applyFilters"
          />
        </a-form-item>
        <a-form-item>
          <a-select
            v-model:value="outcomeFilter"
            :options="outcomeOptions"
            :placeholder="t('cardGameRecord.anyOutcome')"
            style="width: 160px"
            allow-clear
            @change="applyFilters"
          />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="applyFilters">
            {{ t('cardGameRecord.search') }}
          </a-button>
        </a-form-item>
        <a-form-item>
          <a-button @click="resetFilters">{{ t('cardGameRecord.reset') }}</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :data-source="records"
        :loading="loading"
        :pagination="{ current: page, pageSize, total, showSizeChanger: true }"
        row-key="id"
        size="middle"
        :scroll="{ x: 1000 }"
        @change="onTableChange"
      >
        <template #emptyText>
          <div class="empty-hint">{{ t('cardGameRecord.empty') }}</div>
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'startedDate'">
            <span class="timestamp">{{ formatDate(record.startedDate) }}</span>
          </template>
          <template v-else-if="column.key === 'endedDate'">
            <span class="timestamp">{{ formatDate(record.endedDate) }}</span>
          </template>
          <template v-else-if="column.key === 'duration'">
            {{ formatDuration(record as CardGameRecordDTO) }}
          </template>
          <template v-else-if="column.key === 'host'">
            <a-tooltip :title="record.hostUsername">{{ record.hostName }}</a-tooltip>
          </template>
          <template v-else-if="column.key === 'scores'">
            <!-- Seats come back highest score first, so this reads as the standings. -->
            <div class="score-list">
              <a-tooltip
                v-for="player in (record as CardGameRecordDTO).players"
                :key="player.username"
                :title="seatTitle(player.name, player.username)"
              >
                <a-tag :color="player.winner ? 'gold' : 'default'">
                  <span class="seat-name">{{ player.name }}</span>
                  <span class="seat-score">{{ player.score }}</span>
                </a-tag>
              </a-tooltip>
            </div>
          </template>
          <template v-else-if="column.key === 'outcome'">
            <a-tag :color="outcomeColor[record.outcome as CardGameRecordOutcome]">
              {{ t(`cardGameRecord.outcomes.${record.outcome}`) }}
            </a-tag>
            <div v-if="record.leftByName" class="sub-note">
              {{ t('cardGameRecord.leftBy', { name: record.leftByName }) }}
            </div>
            <div v-else-if="record.trickCount" class="sub-note">
              {{ t('cardGameRecord.trickCount', { count: record.trickCount }) }}
            </div>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.card-game-record-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.list-card {
  border-radius: 8px;
}

.filter-form {
  margin-bottom: 16px;
  row-gap: 12px;
}

.timestamp,
.sub-note,
.empty-hint {
  color: var(--ant-color-text-tertiary, rgba(0, 0, 0, 0.45));
  font-size: 12px;
}

.score-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* The score is the thing being compared down the column, so it is the part set apart. */
.seat-score {
  margin-left: 6px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.seat-name {
  opacity: 0.85;
}

.empty-hint {
  padding: 24px 0;
}
</style>
