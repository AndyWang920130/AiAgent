<script lang="ts" setup>
// Renders any tutorial in the guide registry. The slug picks the entry; the entry's key
// prefix picks the copy out of the locale file. See guides.ts.
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { findGuide } from '../guides'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const guide = computed(() => findGuide(route.params.slug))

// A slug nobody recognises belongs back at the index rather than on a blank page. Runs on
// mount too (immediate), since the route param is already set by then.
watch(guide, g => {
  if (!g) router.replace({ name: 'user-guide' })
}, { immediate: true })

/** The step list, built from the count the entry declares. */
const steps = computed(() => {
  const g = guide.value
  if (!g) return []
  return Array.from({ length: g.steps }, (_, i) => ({
    key: i + 1,
    title: t(`guide.${g.key}Step${i + 1}Title`),
    body: t(`guide.${g.key}Step${i + 1}Body`),
  }))
})

function back() {
  router.push({ name: 'user-guide' })
}
</script>

<template>
  <div v-if="guide" class="guide-tutorial-view">
    <a-page-header :title="t(`guide.${guide.key}Title`)" @back="back" />

    <a-card :bordered="false">
      <a-alert
        type="info"
        show-icon
        class="guide-alert"
        :message="t(`guide.${guide.key}Intro`)"
      />

      <a-alert
        v-if="guide.prereq"
        type="warning"
        show-icon
        class="guide-alert"
        :message="t('guide.beforeYouStart')"
        :description="t(`guide.${guide.key}Prereq`)"
      />

      <!-- A reference list, not a wizard: current="-1" keeps every step neutral. -->
      <a-steps direction="vertical" :current="-1" class="guide-steps">
        <a-step v-for="s in steps" :key="s.key" :title="s.title" :description="s.body" />
      </a-steps>

      <a-alert
        v-if="guide.tip"
        type="success"
        show-icon
        class="guide-alert"
        :message="t(`guide.${guide.key}Tip`)"
      />

      <div class="guide-links">
        <a-button
          v-for="link in guide.links"
          :key="link.path"
          type="primary"
          ghost
          @click="router.push(link.path)"
        >
          {{ t(link.labelKey) }}
        </a-button>
        <a-button @click="back">{{ t('guide.allTutorials') }}</a-button>
      </div>
    </a-card>
  </div>
</template>

<style scoped>
.guide-alert {
  margin-bottom: 16px;
}
.guide-steps {
  margin: 24px 0;
}
/* Step bodies are full sentences, so give them room to breathe. */
.guide-steps :deep(.ant-steps-item-description) {
  padding-bottom: 20px;
  line-height: 1.7;
}
.guide-links {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
@media (max-width: 768px) {
  .guide-links {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
