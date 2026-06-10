<script lang="ts" setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  EyeOutlined,
  EditOutlined,
  DeleteOutlined,
  PlusOutlined,
} from '@ant-design/icons-vue'
import { posts, deletePost, type Post } from '../stores/blog'

const router = useRouter()

const searchText = ref('')
const selectedCategory = ref<string | undefined>(undefined)
const categories = ['Frontend', 'Backend', 'Language', 'DevOps']

const filteredPosts = computed(() =>
  posts.value.filter(p => {
    const matchSearch = !searchText.value || p.title.toLowerCase().includes(searchText.value.toLowerCase())
    const matchCategory = !selectedCategory.value || p.category === selectedCategory.value
    return matchSearch && matchCategory
  })
)

const columns = [
  { title: 'Title', dataIndex: 'title', key: 'title', ellipsis: true },
  { title: 'Category', dataIndex: 'category', key: 'category', width: 120 },
  { title: 'Tag', dataIndex: 'tag', key: 'tag', width: 110 },
  { title: 'Date', dataIndex: 'date', key: 'date', width: 120, sorter: (a: Post, b: Post) => a.date.localeCompare(b.date) },
  { title: 'Views', dataIndex: 'views', key: 'views', width: 90, sorter: (a: Post, b: Post) => a.views - b.views },
  { title: 'Actions', key: 'actions', width: 140, fixed: 'right' },
]

function handleDelete(id: number, title: string) {
  Modal.confirm({
    title: 'Delete Post',
    content: `Are you sure you want to delete "${title}"?`,
    okText: 'Delete',
    okType: 'danger',
    onOk() {
      deletePost(id)
      message.success('Post deleted')
    },
  })
}
</script>

<template>
  <div class="blog-list-view">
    <a-card :bordered="false">
      <template #title>📋 Blog List</template>
      <template #extra>
        <a-button type="primary" @click="router.push('/blog/add')">
          <template #icon><PlusOutlined /></template>
          Add Post
        </a-button>
      </template>

      <div class="filter-bar">
        <a-input-search
          v-model:value="searchText"
          placeholder="Search by title..."
          style="width: 280px"
          allow-clear
        />
        <a-select
          v-model:value="selectedCategory"
          placeholder="All categories"
          allow-clear
          style="width: 180px"
        >
          <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
        </a-select>
      </div>

      <a-table
        :columns="columns"
        :data-source="filteredPosts"
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
              <a-tooltip title="View">
                <a-button size="small" @click="router.push('/blog/' + record.id)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="Edit">
                <a-button size="small" type="primary" @click="router.push('/blog/' + record.id + '/edit')">
                  <template #icon><EditOutlined /></template>
                </a-button>
              </a-tooltip>
              <a-tooltip title="Delete">
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
