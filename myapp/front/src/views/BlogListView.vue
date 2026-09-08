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
  DownloadOutlined,
} from '@ant-design/icons-vue'
import { posts, loading, fetchPosts, deletePost, type Post } from '../stores/blog'
import { blogApi } from '../api/blog'
import { getUser } from '../utils/auth'

const { t } = useI18n()
const router = useRouter()

const currentUsername = getUser<{ username: string }>()?.username

function canManage(post: Post) {
  return post.author === currentUsername
}

const searchText = ref('')
const selectedCategory = ref<string | undefined>(undefined)
const categories = ['Frontend', 'Backend', 'Language', 'DevOps']

const exporting = ref(false)

// Pull the download filename out of the Content-Disposition header (RFC 5987
// `filename*=` first, then plain `filename=`), falling back to a sensible default.
function filenameFromDisposition(cd?: string): string {
  const fallback = 'blogs.xlsx'
  if (!cd) return fallback
  const match = /filename\*=(?:UTF-8'')?([^;]+)/i.exec(cd) || /filename="?([^";]+)"?/i.exec(cd)
  if (!match) return fallback
  try {
    return decodeURIComponent(match[1].trim().replace(/["']/g, ''))
  } catch {
    return match[1].trim().replace(/["']/g, '')
  }
}

async function handleExport() {
  exporting.value = true
  try {
    const res = await blogApi.exportExcel()
    const contentType = (res.headers['content-type'] as string | undefined)
      || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    const blob = new Blob([res.data], { type: contentType })
    const filename = filenameFromDisposition(res.headers['content-disposition'] as string | undefined)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    message.success(t('blog.exportSuccess'))
  } catch {
    message.error(t('blog.exportFailed'))
  } finally {
    exporting.value = false
  }
}

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
  { title: t('blog.colVisibility'), dataIndex: 'visibility', key: 'visibility', width: 110 },
  { title: t('blog.colAuthor'), dataIndex: 'author', key: 'author', width: 140, ellipsis: true },
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
      try {
        await deletePost(id)
        message.success(t('blog.deleted'))
      } catch {
        message.error(t('blog.deleteFailed'))
      }
    },
  })
}
</script>

<template>
  <div class="blog-list-view">
    <a-card :bordered="false">
      <template #title>{{ t('blog.listTitle') }}</template>
      <template #extra>
        <a-space>
          <a-button :loading="exporting" @click="handleExport">
            <template #icon><DownloadOutlined /></template>
            {{ exporting ? t('blog.exporting') : t('blog.exportPost') }}
          </a-button>
          <a-button type="primary" @click="router.push('/blog/add')">
            <template #icon><PlusOutlined /></template>
            {{ t('blog.addPost') }}
          </a-button>
        </a-space>
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
        :scroll="{ x: 940 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'title'">
            <a class="post-link" @click="router.push('/blog/' + record.id)">{{ record.title }}</a>
          </template>
          <template v-else-if="column.key === 'author'">
            <a v-if="record.author" class="user-link" @click="router.push('/users/' + record.author)">{{ record.author }}</a>
            <span v-else>-</span>
          </template>
          <template v-else-if="column.key === 'tag'">
            <a-tag :color="record.tagColor">{{ record.tag }}</a-tag>
          </template>
          <template v-else-if="column.key === 'visibility'">
            <a-tag :color="record.visibility === 'PRIVATE' ? 'default' : 'green'">
              {{ record.visibility === 'PRIVATE' ? t('blog.private') : t('blog.public') }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-tooltip :title="t('blog.view')">
                <a-button size="small" @click="router.push('/blog/' + record.id)">
                  <template #icon><EyeOutlined /></template>
                </a-button>
              </a-tooltip>
              <template v-if="canManage(record)">
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
              </template>
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
.user-link { color: #1890ff; cursor: pointer; }
.user-link:hover { text-decoration: underline; }
</style>
