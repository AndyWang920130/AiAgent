<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import { devices, deleteDevice, type Device } from '../stores/device'

const { t } = useI18n()
const router = useRouter()

const searchText = ref('')

const statusColor: Record<string, string> = {
  active: 'success',
  inactive: 'default',
  maintenance: 'warning',
}

const statusLabel = (s: string) => {
  if (s === 'active') return t('device.statusActive')
  if (s === 'inactive') return t('device.statusInactive')
  return t('device.statusMaintenance')
}

const filteredDevices = computed(() =>
  devices.value.filter(d => {
    if (!searchText.value) return true
    const kw = searchText.value.toLowerCase()
    return d.serialNumber.toLowerCase().includes(kw) || d.address.toLowerCase().includes(kw)
  })
)

const columns = computed(() => [
  { title: t('device.colSerialNumber'), dataIndex: 'serialNumber', key: 'serialNumber', width: 160 },
  { title: t('device.colModel'), dataIndex: 'model', key: 'model', width: 180 },
  { title: t('device.colStatus'), dataIndex: 'status', key: 'status', width: 110 },
  { title: t('device.colInstallationDate'), dataIndex: 'installationDate', key: 'installationDate', width: 130, sorter: (a: Device, b: Device) => a.installationDate.localeCompare(b.installationDate) },
  { title: t('device.colWarrantyYears'), dataIndex: 'warrantyYears', key: 'warrantyYears', width: 100, sorter: (a: Device, b: Device) => a.warrantyYears - b.warrantyYears },
  { title: t('device.colAddress'), dataIndex: 'address', key: 'address', ellipsis: true },
  { title: t('device.colActions'), key: 'actions', width: 140, fixed: 'right' },
])

function handleDelete(id: number, sn: string) {
  Modal.confirm({
    title: t('device.deleteTitle'),
    content: t('device.deleteContent', { sn }),
    okText: t('device.delete'),
    okType: 'danger',
    onOk() {
      deleteDevice(id)
      message.success(t('device.deleted'))
    },
  })
}
</script>

<template>
  <div class="device-list-view">
    <a-card :bordered="false">
      <template #title>{{ t('device.listTitle') }}</template>
      <template #extra>
        <a-button type="primary" @click="router.push('/device/add')">
          <template #icon><PlusOutlined /></template>
          {{ t('device.addDevice') }}
        </a-button>
      </template>

      <div class="filter-bar">
        <a-input-search
          v-model:value="searchText"
          :placeholder="t('device.searchPlaceholder')"
          style="width: 320px"
          allow-clear
        />
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredDevices"
        :row-key="(r: Device) => r.id"
        :pagination="{ pageSize: 10, showSizeChanger: false }"
        :scroll="{ x: 1200 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'serialNumber'">
            <a class="sn-link" @click="router.push('/device/' + record.id)">{{ record.serialNumber }}</a>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tag :color="statusColor[record.status]">{{ statusLabel(record.status) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'warrantyYears'">
            {{ record.warrantyYears }} {{ t('device.warrantyUnit') }}
          </template>
          <template v-else-if="column.key === 'address'">
            <a-tooltip :title="record.address">
              <span class="address-cell">{{ record.address }}</span>
            </a-tooltip>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-tooltip :title="t('device.view')">
                <a-button size="small" @click="router.push('/device/' + record.id)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('device.edit')">
                <a-button size="small" type="primary" @click="router.push('/device/' + record.id + '/edit')">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('device.delete')">
                <a-button size="small" danger @click="handleDelete(record.id, record.serialNumber)">
                  <template #icon><DeleteOutlined /></template>
                </a-button>
              </a-tooltip>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<style scoped>
.device-list-view { display: flex; flex-direction: column; gap: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; }
.sn-link { color: #1890ff; cursor: pointer; font-family: monospace; }
.sn-link:hover { text-decoration: underline; }
.address-cell { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
