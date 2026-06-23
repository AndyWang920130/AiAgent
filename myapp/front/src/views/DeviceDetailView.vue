<script lang="ts" setup>
import { computed, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import { EditOutlined, DeleteOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons-vue'
import { getDevice, deleteDevice } from '../stores/device'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const device = computed(() => getDevice(Number(route.params.id)))

const statusColor: Record<string, string> = {
  active: 'success',
  inactive: 'default',
  maintenance: 'warning',
}

const statusLabel = computed(() => {
  if (!device.value) return ''
  if (device.value.status === 'active') return t('device.statusActive')
  if (device.value.status === 'inactive') return t('device.statusInactive')
  return t('device.statusMaintenance')
})

const phoneRevealed = ref(false)

function maskPhone(phone: string): string {
  if (phone.length <= 7) return phone
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

function handleDelete() {
  if (!device.value) return
  Modal.confirm({
    title: t('device.deleteTitle'),
    content: t('device.deleteContent', { sn: device.value.serialNumber }),
    okText: t('device.delete'),
    okType: 'danger',
    onOk() {
      deleteDevice(device.value!.id)
      message.success(t('device.deleted'))
      router.push('/device')
    },
  })
}
</script>

<template>
  <div class="device-detail-view">
    <template v-if="device">
      <a-page-header
        :title="t('device.detailTitle')"
        :sub-title="device.serialNumber"
        @back="router.push('/device')"
      >
        <template #extra>
          <a-button @click="router.push('/device/' + device.id + '/edit')">
            <template #icon><EditOutlined /></template>
            {{ t('device.edit') }}
          </a-button>
          <a-button danger @click="handleDelete">
            <template #icon><DeleteOutlined /></template>
            {{ t('device.delete') }}
          </a-button>
        </template>
        <template #tags>
          <a-tag :color="statusColor[device.status]">{{ statusLabel }}</a-tag>
        </template>
      </a-page-header>

      <a-card :bordered="false" class="detail-card">
        <a-descriptions bordered :column="{ xxl: 3, xl: 3, lg: 2, md: 2, sm: 1, xs: 1 }">
          <a-descriptions-item :label="t('device.serialNumber')">
            <span style="font-family: monospace; font-weight: 600">{{ device.serialNumber }}</span>
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.model')">{{ device.model }}</a-descriptions-item>
          <a-descriptions-item :label="t('device.status')">
            <a-tag :color="statusColor[device.status]">{{ statusLabel }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.outgoingDate')">{{ device.outgoingDate }}</a-descriptions-item>
          <a-descriptions-item :label="t('device.installationDate')">{{ device.installationDate }}</a-descriptions-item>
          <a-descriptions-item :label="t('device.warrantyYears')">
            {{ device.warrantyYears }} {{ t('device.warrantyUnit') }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.contactPerson')">{{ device.contactPerson }}</a-descriptions-item>
          <a-descriptions-item :label="t('device.contactPhone')">
            <span class="phone-cell">
              <span>{{ phoneRevealed ? device.contactPhone : maskPhone(device.contactPhone) }}</span>
              <a-button
                type="link"
                size="small"
                @click="phoneRevealed = !phoneRevealed"
              >
                <template #icon>
                  <component :is="phoneRevealed ? EyeInvisibleOutlined : EyeOutlined" />
                </template>
              </a-button>
            </span>
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.latitude')">
            {{ device.latitude ?? '—' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.longitude')">
            {{ device.longitude ?? '—' }}
          </a-descriptions-item>
          <a-descriptions-item :label="t('device.address')" :span="2">{{ device.address }}</a-descriptions-item>
        </a-descriptions>
      </a-card>
    </template>

    <a-result
      v-else
      status="404"
      :title="t('device.notFound')"
      :sub-title="t('device.notFoundDesc')"
    >
      <template #extra>
        <a-button type="primary" @click="router.push('/device')">{{ t('device.backToList') }}</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.device-detail-view { display: flex; flex-direction: column; gap: 16px; }
.detail-card { border-radius: 10px; }
.phone-cell { display: inline-flex; align-items: center; gap: 4px; font-family: monospace; }
</style>
