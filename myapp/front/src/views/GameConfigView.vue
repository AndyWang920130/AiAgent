<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  gamePrizes,
  gameParameters,
  gameConfigLoading,
  fetchGameConfig,
  addPrize,
  updatePrize,
  removePrize,
  addParameter,
  updateParameter,
  removeParameter,
} from '../stores/gameConfig'

const { t } = useI18n()

const prizeForm = reactive({ name: '', color: 'blue' })
const parameterForm = reactive({ name: '', value: '', description: '' })

onMounted(() => {
  fetchGameConfig()
})

const prizeColumns = computed(() => [
  { title: t('gameConfig.name'), dataIndex: 'name', key: 'name', width: 220 },
  { title: t('gameConfig.color'), dataIndex: 'color', key: 'color' },
  { title: t('gameConfig.actions'), key: 'actions', width: 120 },
])

const parameterColumns = computed(() => [
  { title: t('gameConfig.name'), dataIndex: 'name', key: 'name', width: 220 },
  { title: t('gameConfig.value'), dataIndex: 'value', key: 'value', width: 160 },
  { title: t('gameConfig.description'), dataIndex: 'description', key: 'description' },
  { title: t('gameConfig.actions'), key: 'actions', width: 120 },
])

function normalize(value: string) {
  return value.trim()
}

function inputValue(event: Event) {
  return (event.target as HTMLInputElement).value
}

async function handleAddPrize() {
  const name = normalize(prizeForm.name)
  const color = normalize(prizeForm.color)
  if (!name || !color) return message.warning(t('gameConfig.nameRequired'))
  if (gamePrizes.value.some(prize => prize.name.toLowerCase() === name.toLowerCase())) {
    return message.warning(t('gameConfig.duplicateName'))
  }
  try {
    await addPrize(name, color)
    prizeForm.name = ''
    prizeForm.color = 'blue'
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
  }
}

async function handleAddParameter() {
  const name = normalize(parameterForm.name)
  const value = normalize(parameterForm.value)
  if (!name || !value) return message.warning(t('gameConfig.nameRequired'))
  if (gameParameters.value.some(param => param.name.toLowerCase() === name.toLowerCase())) {
    return message.warning(t('gameConfig.duplicateName'))
  }
  try {
    await addParameter(name, value, normalize(parameterForm.description))
    parameterForm.name = ''
    parameterForm.value = ''
    parameterForm.description = ''
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
  }
}

async function savePrizeName(id: number, name: string) {
  try {
    await updatePrize(id, { name })
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
    fetchGameConfig()
  }
}

async function savePrizeColor(id: number, color: string) {
  try {
    await updatePrize(id, { color })
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
    fetchGameConfig()
  }
}

async function saveParameterName(id: number, name: string) {
  try {
    await updateParameter(id, { name })
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
    fetchGameConfig()
  }
}

async function saveParameterValue(id: number, value: string) {
  try {
    await updateParameter(id, { value })
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
    fetchGameConfig()
  }
}

async function saveParameterDescription(id: number, description: string) {
  try {
    await updateParameter(id, { description })
    message.success(t('gameConfig.saved'))
  } catch {
    message.error(t('gameConfig.saveFailed'))
    fetchGameConfig()
  }
}

async function deletePrize(id: number) {
  try {
    await removePrize(id)
    message.success(t('gameConfig.deleted'))
  } catch {
    message.error(t('gameConfig.deleteFailed'))
  }
}

async function deleteParameter(id: number) {
  try {
    await removeParameter(id)
    message.success(t('gameConfig.deleted'))
  } catch {
    message.error(t('gameConfig.deleteFailed'))
  }
}
</script>

<template>
  <div class="game-config-view">
    <a-page-header
      :title="t('gameConfig.pageTitle')"
      :sub-title="t('gameConfig.pageSubtitle')"
    />

    <a-spin :spinning="gameConfigLoading">
    <a-tabs>
      <a-tab-pane key="prizes" :tab="t('gameConfig.prizes')">
        <a-card :bordered="false" class="config-card">
          <a-form layout="inline" class="config-form" @submit.prevent>
            <a-form-item>
              <a-input
                v-model:value="prizeForm.name"
                :placeholder="t('gameConfig.prizeNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-input
                v-model:value="prizeForm.color"
                :placeholder="t('gameConfig.colorValuePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-tag :color="prizeForm.color">{{ prizeForm.name || prizeForm.color }}</a-tag>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleAddPrize">{{ t('gameConfig.add') }}</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :columns="prizeColumns"
            :data-source="gamePrizes"
            :pagination="false"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-input
                  :value="record.name"
                  @change="savePrizeName(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'color'">
                <a-space>
                  <a-input
                    :value="record.color"
                    @change="savePrizeColor(record.id, inputValue($event))"
                  />
                  <a-tag :color="record.color">{{ record.color }}</a-tag>
                </a-space>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-popconfirm
                  :title="t('gameConfig.deleteConfirm')"
                  @confirm="deletePrize(record.id)"
                >
                  <a-button danger type="link">{{ t('gameConfig.delete') }}</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="parameters" :tab="t('gameConfig.parameters')">
        <a-card :bordered="false" class="config-card">
          <a-form layout="inline" class="config-form" @submit.prevent>
            <a-form-item>
              <a-input
                v-model:value="parameterForm.name"
                :placeholder="t('gameConfig.parameterNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-input
                v-model:value="parameterForm.value"
                :placeholder="t('gameConfig.valuePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item class="wide-field">
              <a-input
                v-model:value="parameterForm.description"
                :placeholder="t('gameConfig.descriptionPlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleAddParameter">{{ t('gameConfig.add') }}</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :columns="parameterColumns"
            :data-source="gameParameters"
            :pagination="false"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-input
                  :value="record.name"
                  @change="saveParameterName(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'value'">
                <a-input
                  :value="record.value"
                  @change="saveParameterValue(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'description'">
                <a-input
                  :value="record.description"
                  @change="saveParameterDescription(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-popconfirm
                  :title="t('gameConfig.deleteConfirm')"
                  @confirm="deleteParameter(record.id)"
                >
                  <a-button danger type="link">{{ t('gameConfig.delete') }}</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>
    </a-tabs>
    </a-spin>
  </div>
</template>

<style scoped>
.game-config-view {
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
</style>
