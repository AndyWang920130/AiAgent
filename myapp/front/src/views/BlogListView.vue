<script lang="ts" setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import { posts, loading, fetchPosts, deletePost, type Post } from '../stores/blog'

const { t } = useI18n()
const router = useRouter()

const searchText = ref('')
const selectedCategory = ref<string | undefined>(undefined)
const categories = ['Frontend', 'Backend', 'Language', 'DevOps']

onMounted(() => fetchPosts())

const filteredPosts = computed(() =>
  posts.value.filter(p => {
    const matchSearch = !searchText.value || p.title.toLowerCase().includes(searchText.value.toLowerCase())
    const matchCategory = !selectedCategory.value || p.category === selectedCategory.value
    return matchSearch && matchCategory
  })
)

const columns = computed(() => [
  { title: t('blog.colTitle'), dataIndex: 'title', key: 'title', ellipsis: true },
  { title: t('blog.colCategory'), dataIndex: 'category', key: 'category', width: 120 },
  { title: t('blog.colTag'), dataIndex: 'tag', key: 'tag', width: 110 },
  { title: t('blog.colDate'), dataIndex: 'date', key: 'date', width: 120, sorter: (a: Post, b: Post) => a.date.localeCompare(b.date) },
  { title: t('blog.colViews'), dataIndex: 'views', key: 'views', width: 90, sorter: (a: Post, b: Post) => a.views - b.views },
  { title: t('blog.colActions'), key: 'actions', width: 140, fixed: 'right' },
])

function handleDelete(id: number, title: string) {
  Modal.confirm({
    title: t('blog.deleteTitle'),
    content: t('blog.deleteContent', { title }),
    okText: t('blog.delete'),
    okType: 'danger',
    async onOk() {
      await deletePost(id)
      message.success(t('blog.deleted'))
    },
  })
}
</script>

<template>
  <div class="blog-list-view">
    <a-card :bordered="false">
      <template #title>{{ t('blog.listTitle') }}</template>
      <template #extra>
        <a-button type="primary" @click="router.push('/blog/add')">
          <template #icon><PlusOutlined /></template>
          {{ t('blog.addPost') }}
        </a-button>
      </template>

      <div class="filter-bar">
        <a-input-search
          v-model:value="searchText"
          :placeholder="t('blog.searchPlaceholder')"
          style="width: 280px"
          allow-clear
        />
        <a-select
          v-model:value="selectedCategory"
          :placeholder="t('blog.allCategories')"
          allow-clear
          style="width: 180px"
        >
          <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
        </a-select>
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredPosts"
        :loading="loading"
        :row-key="(r: Post) => r.id"
        :pagination="{ pageSize: 10, showSizeChanger: false }"
        :scroll="{ x: 800 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a class="post-link" @click="router.push('/blog/' + record.id)">{{ record.title }}</a>
          </template>
          <template v-else-if="column.key === 'tag'">
            <a-tag :color="record.tagColor">{{ record.tag }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-tooltip :title="t('blog.view')">
                <a-button size="small" @click="router.push('/blog/' + record.id)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('blog.edit')">
                <a-button size="small" type="primary" @click="router.push('/blog/' + record.id + '/edit')">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip :title="t('blog.delete')">
                <a-button size="small" danger @click="handleDelete(record.id, record.title)">
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
.blog-list-view { display: flex; flex-direction: column; gap: 16px; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.post-link { color: #1890ff; cursor: pointer; }
.post-link:hover { text-decoration: underline; }
</style>
