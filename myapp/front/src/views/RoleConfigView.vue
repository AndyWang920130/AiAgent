<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  roleConfigs,
  roleConfigLoading,
  fetchRoleConfigs,
  addRoleConfig,
  updateRoleConfig,
  removeRoleConfig,
} from '../stores/roleConfig'

const { t } = useI18n()

const addForm = reactive({ code: '', name: '', description: '' })

onMounted(() => {
  fetchRoleConfigs()
})

const columns = computed(() => [
  { title: t('roleConfig.code'), dataIndex: 'code', key: 'code', width: 200 },
  { title: t('roleConfig.name'), dataIndex: 'name', key: 'name', width: 200 },
  { title: t('roleConfig.description'), dataIndex: 'description', key: 'description' },
  { title: t('roleConfig.userCount'), dataIndex: 'userCount', key: 'userCount', width: 100 },
  { title: t('roleConfig.sortOrder'), dataIndex: 'sortOrder', key: 'sortOrder', width: 110 },
  { title: t('roleConfig.enabled'), dataIndex: 'enabled', key: 'enabled', width: 100 },
  { title: t('roleConfig.actions'), key: 'actions', width: 120 },
])

function normalize(value: string) {
  return value.trim()
}

function inputValue(event: Event) {
  return (event.target as HTMLInputElement).value
}

/** Codes become `ROLE_<CODE>` authorities, so keep them to the characters the backend accepts. */
function normalizeCode(value: string) {
  return value.trim().toUpperCase()
}

const codePattern = /^[A-Za-z0-9_]+$/

async function handleAdd() {
  const code = normalizeCode(addForm.code)
  const name = normalize(addForm.name)
  if (!code || !name) return message.warning(t('roleConfig.codeAndNameRequired'))
  if (!codePattern.test(code)) return message.warning(t('roleConfig.codeInvalid'))
  if (roleConfigs.value.some(role => role.code.toUpperCase() === code)) {
    return message.warning(t('roleConfig.duplicateCode'))
  }
  try {
    await addRoleConfig({ code, name, description: normalize(addForm.description) })
    addForm.code = ''
    addForm.name = ''
    addForm.description = ''
    message.success(t('roleConfig.saved'))
  } catch {
    message.error(t('roleConfig.saveFailed'))
  }
}

async function saveCode(id: number, raw: string) {
  const code = normalizeCode(raw)
  if (!code) return message.warning(t('roleConfig.codeAndNameRequired'))
  if (!codePattern.test(code)) {
    message.warning(t('roleConfig.codeInvalid'))
    return fetchRoleConfigs()
  }
  await save(id, { code })
}

async function saveName(id: number, name: string) {
  await save(id, { name })
}

async function saveDescription(id: number, description: string) {
  await save(id, { description })
}

async function saveSortOrder(id: number, sortOrder: number | null) {
  await save(id, { sortOrder })
}

async function saveEnabled(id: number, enabled: boolean) {
  await save(id, { enabled })
}

/**
 * The backend rejects renaming, disabling or deleting a role that accounts still hold, so a
 * refused change is reloaded rather than left showing a value the server never accepted.
 */
async function save(id: number, data: Parameters<typeof updateRoleConfig>[1]) {
  try {
    await updateRoleConfig(id, data)
    message.success(t('roleConfig.saved'))
  } catch {
    message.error(t('roleConfig.saveFailed'))
    fetchRoleConfigs()
  }
}

async function handleDelete(id: number) {
  try {
    await removeRoleConfig(id)
    message.success(t('roleConfig.deleted'))
  } catch {
    message.error(t('roleConfig.deleteFailed'))
  }
}
</script>
<template>
  <div class="role-config-view">
    <a-page-header
      :title="t('roleConfig.pageTitle')"
      :sub-title="t('roleConfig.pageSubtitle')"
    />

    <a-spin :spinning="roleConfigLoading">
      <a-card :bordered="false" class="config-card">
        <a-form layout="inline" class="config-form" @submit.prevent>
          <a-form-item>
            <a-input
              v-model:value="addForm.code"
              :placeholder="t('roleConfig.codePlaceholder')"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-input
              v-model:value="addForm.name"
              :placeholder="t('roleConfig.namePlaceholder')"
              allow-clear
            />
          </a-form-item>
          <a-form-item class="wide-field">
            <a-input
              v-model:value="addForm.description"
              :placeholder="t('roleConfig.descriptionPlaceholder')"
              allow-clear
            />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleAdd">{{ t('roleConfig.add') }}</a-button>
          </a-form-item>
        </a-form>

        <a-table
          :columns="columns"
          :data-source="roleConfigs"
          :pagination="false"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'code'">
              <span v-if="record.builtIn" class="built-in-code">
                <strong>{{ record.code }}</strong>
                <a-tag color="blue">{{ t('roleConfig.builtIn') }}</a-tag>
              </span>
              <a-input
                v-else
                :value="record.code"
                @change="saveCode(record.id, inputValue($event))"
              />
            </template>
            <template v-else-if="column.key === 'name'">
              <a-input
                :value="record.name"
                @change="saveName(record.id, inputValue($event))"
              />
            </template>
            <template v-else-if="column.key === 'description'">
              <a-input
                :value="record.description"
                @change="saveDescription(record.id, inputValue($event))"
              />
            </template>
            <template v-else-if="column.key === 'userCount'">
              {{ record.userCount }}
            </template>
            <template v-else-if="column.key === 'sortOrder'">
              <a-input-number
                :value="record.sortOrder"
                :min="0"
                @change="saveSortOrder(record.id, $event as number | null)"
              />
            </template>
            <template v-else-if="column.key === 'enabled'">
              <a-switch
                :checked="record.enabled"
                :disabled="record.builtIn"
                @change="saveEnabled(record.id, $event as boolean)"
              />
            </template>
            <template v-else-if="column.key === 'actions'">
              <a-popconfirm
                v-if="!record.builtIn"
                :title="t('roleConfig.deleteConfirm')"
                @confirm="handleDelete(record.id)"
              >
                <a-button danger type="link">{{ t('roleConfig.delete') }}</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-card>
    </a-spin>
  </div>
</template>

<style scoped>
.role-config-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.config-card {
  border-radius: 8px;
}

.config-form {
  margin-bottom: 16px;
  row-gap: 12px;
}

.wide-field {
  min-width: 320px;
}

.built-in-code {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
</style>
