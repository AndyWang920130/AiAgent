<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import {
  blogCategories,
  blogConfigLoading,
  blogTags,
  blogTagColors,
  fetchBlogConfig,
  addCategory,
  updateCategory,
  removeCategory,
  addTag,
  updateTag,
  removeTag,
  addTagColor,
  updateTagColor,
  removeTagColor,
} from '../stores/blogConfig'

const { t } = useI18n()

const categoryForm = reactive({ name: '', description: '' })
const tagForm = reactive({ name: '' })
const colorForm = reactive({ name: '', color: 'blue' })

onMounted(() => {
  fetchBlogConfig()
})

const categoryColumns = computed(() => [
  { title: t('blogConfig.name'), dataIndex: 'name', key: 'name', width: 220 },
  { title: t('blogConfig.description'), dataIndex: 'description', key: 'description' },
  { title: t('blogConfig.actions'), key: 'actions', width: 120 },
])

const tagColumns = computed(() => [
  { title: t('blogConfig.name'), dataIndex: 'name', key: 'name' },
  { title: t('blogConfig.actions'), key: 'actions', width: 120 },
])

const colorColumns = computed(() => [
  { title: t('blogConfig.name'), dataIndex: 'name', key: 'name', width: 220 },
  { title: t('blogConfig.color'), dataIndex: 'color', key: 'color' },
  { title: t('blogConfig.actions'), key: 'actions', width: 120 },
])

function normalize(value: string) {
  return value.trim()
}

function inputValue(event: Event) {
  return (event.target as HTMLInputElement).value
}

async function handleAddCategory() {
  const name = normalize(categoryForm.name)
  if (!name) return message.warning(t('blogConfig.nameRequired'))
  if (blogCategories.value.some(category => category.name.toLowerCase() === name.toLowerCase())) {
    return message.warning(t('blogConfig.duplicateName'))
  }
  try {
    await addCategory(name, normalize(categoryForm.description))
    categoryForm.name = ''
    categoryForm.description = ''
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
  }
}

async function handleAddTag() {
  const name = normalize(tagForm.name)
  if (!name) return message.warning(t('blogConfig.nameRequired'))
  if (blogTags.value.some(tag => tag.name.toLowerCase() === name.toLowerCase())) {
    return message.warning(t('blogConfig.duplicateName'))
  }
  try {
    await addTag(name)
    tagForm.name = ''
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
  }
}

async function handleAddColor() {
  const name = normalize(colorForm.name)
  const color = normalize(colorForm.color)
  if (!name || !color) return message.warning(t('blogConfig.nameRequired'))
  if (blogTagColors.value.some(item => item.color.toLowerCase() === color.toLowerCase())) {
    return message.warning(t('blogConfig.duplicateColor'))
  }
  try {
    await addTagColor(name, color)
    colorForm.name = ''
    colorForm.color = 'blue'
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
  }
}

async function saveCategoryName(id: number, name: string) {
  try {
    await updateCategory(id, { name })
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
    fetchBlogConfig()
  }
}

async function saveCategoryDescription(id: number, description: string) {
  try {
    await updateCategory(id, { description })
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
    fetchBlogConfig()
  }
}

async function saveTag(id: number, name: string) {
  try {
    await updateTag(id, name)
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
    fetchBlogConfig()
  }
}

async function saveTagColorName(id: number, name: string) {
  try {
    await updateTagColor(id, { name })
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
    fetchBlogConfig()
  }
}

async function saveTagColorValue(id: number, color: string) {
  try {
    await updateTagColor(id, { color })
    message.success(t('blogConfig.saved'))
  } catch {
    message.error(t('blogConfig.saveFailed'))
    fetchBlogConfig()
  }
}

async function deleteCategory(id: number) {
  try {
    await removeCategory(id)
    message.success(t('blogConfig.deleted'))
  } catch {
    message.error(t('blogConfig.deleteFailed'))
  }
}

async function deleteTag(id: number) {
  try {
    await removeTag(id)
    message.success(t('blogConfig.deleted'))
  } catch {
    message.error(t('blogConfig.deleteFailed'))
  }
}

async function deleteTagColor(id: number) {
  try {
    await removeTagColor(id)
    message.success(t('blogConfig.deleted'))
  } catch {
    message.error(t('blogConfig.deleteFailed'))
  }
}
</script>

<template>
  <div class="blog-config-view">
    <a-page-header
      :title="t('blogConfig.pageTitle')"
      :sub-title="t('blogConfig.pageSubtitle')"
    />

    <a-spin :spinning="blogConfigLoading">
    <a-tabs>
      <a-tab-pane key="categories" :tab="t('blogConfig.categories')">
        <a-card :bordered="false" class="config-card">
          <a-form layout="inline" class="config-form" @submit.prevent>
            <a-form-item>
              <a-input
                v-model:value="categoryForm.name"
                :placeholder="t('blogConfig.categoryNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item class="wide-field">
              <a-input
                v-model:value="categoryForm.description"
                :placeholder="t('blogConfig.descriptionPlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleAddCategory">{{ t('blogConfig.add') }}</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :columns="categoryColumns"
            :data-source="blogCategories"
            :pagination="false"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-input
                  :value="record.name"
                  @change="saveCategoryName(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'description'">
                <a-input
                  :value="record.description"
                  @change="saveCategoryDescription(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-popconfirm
                  :title="t('blogConfig.deleteConfirm')"
                  @confirm="deleteCategory(record.id)"
                >
                  <a-button danger type="link">{{ t('blogConfig.delete') }}</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="tags" :tab="t('blogConfig.tags')">
        <a-card :bordered="false" class="config-card">
          <a-form layout="inline" class="config-form" @submit.prevent>
            <a-form-item>
              <a-input
                v-model:value="tagForm.name"
                :placeholder="t('blogConfig.tagNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleAddTag">{{ t('blogConfig.add') }}</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :columns="tagColumns"
            :data-source="blogTags"
            :pagination="false"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-input
                  :value="record.name"
                  @change="saveTag(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-popconfirm
                  :title="t('blogConfig.deleteConfirm')"
                  @confirm="deleteTag(record.id)"
                >
                  <a-button danger type="link">{{ t('blogConfig.delete') }}</a-button>
                </a-popconfirm>
              </template>
            </template>
          </a-table>
        </a-card>
      </a-tab-pane>

      <a-tab-pane key="tag-colors" :tab="t('blogConfig.tagColors')">
        <a-card :bordered="false" class="config-card">
          <a-form layout="inline" class="config-form" @submit.prevent>
            <a-form-item>
              <a-input
                v-model:value="colorForm.name"
                :placeholder="t('blogConfig.colorNamePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-input
                v-model:value="colorForm.color"
                :placeholder="t('blogConfig.colorValuePlaceholder')"
                allow-clear
              />
            </a-form-item>
            <a-form-item>
              <a-tag :color="colorForm.color">{{ colorForm.name || colorForm.color }}</a-tag>
            </a-form-item>
            <a-form-item>
              <a-button type="primary" @click="handleAddColor">{{ t('blogConfig.add') }}</a-button>
            </a-form-item>
          </a-form>

          <a-table
            :columns="colorColumns"
            :data-source="blogTagColors"
            :pagination="false"
            row-key="id"
            size="middle"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'name'">
                <a-input
                  :value="record.name"
                  @change="saveTagColorName(record.id, inputValue($event))"
                />
              </template>
              <template v-else-if="column.key === 'color'">
                <a-space>
                  <a-input
                    :value="record.color"
                    @change="saveTagColorValue(record.id, inputValue($event))"
                  />
                  <a-tag :color="record.color">{{ record.color }}</a-tag>
                </a-space>
              </template>
              <template v-else-if="column.key === 'actions'">
                <a-popconfirm
                  :title="t('blogConfig.deleteConfirm')"
                  @confirm="deleteTagColor(record.id)"
                >
                  <a-button danger type="link">{{ t('blogConfig.delete') }}</a-button>
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
.blog-config-view {
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
