<script lang="ts" setup>
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { guides } from '../guides'

const { t } = useI18n()
const router = useRouter()

function openGuide(slug: string) {
  router.push({ name: 'guide-tutorial', params: { slug } })
}
</script>

<template>
  <div class="user-guide-view">
    <a-page-header :title="t('guide.title')" :sub-title="t('guide.subtitle')" />

    <a-row :gutter="[16, 16]">
      <a-col v-for="g in guides" :key="g.slug" :xs="24" :sm="12" :md="8" :lg="6">
        <a-card hoverable class="guide-card" @click="openGuide(g.slug)">
          <template #cover>
            <div class="guide-cover">
              <component :is="g.icon" class="guide-icon" />
            </div>
          </template>
          <a-card-meta :title="t(`guide.${g.key}Title`)" :description="t(`guide.${g.key}Desc`)" />
          <template #actions>
            <a-button type="link">{{ t('guide.read') }}</a-button>
          </template>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.guide-card {
  cursor: pointer;
  height: 100%;
}
.guide-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  background: #f5f5f5;
}
.guide-icon {
  font-size: 36px;
  color: #1890ff;
}
</style>
