<script lang="ts" setup>
// What this app is, plus the version it is currently on.
//
// The version comes from the newest published release note rather than from a build constant:
// pom.xml is still 0.0.1-SNAPSHOT, package.json is 0.0.0, and there is no actuator or
// build-info endpoint, so neither would tell a reader anything true. The release notes are the
// one version record somebody actually maintains.
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { FileTextOutlined } from '@ant-design/icons-vue'
import { releaseNoteApi } from '../api/releaseNote'

const { t } = useI18n()
const router = useRouter()

const version = ref<string | null>(null)
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    version.value = await releaseNoteApi.currentVersion()
  } catch {
    // Not worth an error banner on an informational page — the row just reads "unreleased".
    version.value = null
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="about-view">
    <a-page-header :title="t('about.pageTitle')" :sub-title="t('about.pageSubtitle')" />

    <a-card :bordered="false" class="about-card">
      <p class="about-intro">{{ t('about.intro') }}</p>

      <a-descriptions :column="1" bordered size="middle">
        <a-descriptions-item :label="t('about.currentVersion')">
          <a-spin v-if="loading" size="small" />
          <span v-else-if="version" class="version-value">{{ version }}</span>
          <span v-else class="version-unknown">{{ t('about.noVersion') }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="t('about.frontendStack')">
          {{ t('about.frontendStackValue') }}
        </a-descriptions-item>
        <a-descriptions-item :label="t('about.backendStack')">
          {{ t('about.backendStackValue') }}
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-button type="primary" ghost @click="router.push('/about/release-notes')">
        <template #icon><FileTextOutlined /></template>
        {{ t('about.viewReleaseNotes') }}
      </a-button>
    </a-card>
  </div>
</template>

<style scoped>
.about-card {
  border-radius: 10px;
}
.about-intro {
  margin-bottom: 20px;
  line-height: 1.8;
  font-size: 14px;
}
.version-value {
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.version-unknown {
  opacity: 0.5;
}
</style>
