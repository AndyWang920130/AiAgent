<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import {
  MailOutlined,
  EditOutlined,
  DeleteOutlined,
  LogoutOutlined,
  LockOutlined,
  TrophyOutlined,
  FileTextOutlined,
  HeartOutlined,
  EyeOutlined,
} from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { clearAuth, getUser } from '../utils/auth'
import { deletePost, fetchMyPosts, loadingMyPosts, myPosts, type Post } from '../stores/blog'
import { blogApi } from '../api/blog'

const { t } = useI18n()
const router = useRouter()

const storedUser = getUser<{ username: string; name: string }>()

const profile = reactive({
  name: storedUser?.name || 'Administrator',
  username: storedUser?.username || 'admin',
  email: 'admin@myapp.com',
  bio: 'Full-stack developer passionate about building great products. Vue, Spring Boot, and everything in between.',
  location: 'Shanghai, China',
  website: 'https://myapp.com',
  joinDate: '2024-01-15',
})

const editMode = ref(false)
const editForm = reactive({ ...profile })
const passwordForm = reactive({ current: '', next: '', confirm: '' })
const showPasswordModal = ref(false)
const likesReceived = ref(0)

onMounted(async () => {
  await Promise.all([
    fetchMyPosts(),
    blogApi.getMyLikesReceived().then(total => {
      likesReceived.value = total
    }),
  ])
})

const activityData = computed(() => [
  { label: () => t('personal.postsWritten'), value: myPosts.value.length, icon: FileTextOutlined, color: '#1890ff' },
  { label: () => t('personal.likesReceived'), value: likesReceived.value, icon: HeartOutlined, color: '#eb2f96' },
  { label: () => t('personal.achievements'), value: 47, icon: TrophyOutlined, color: '#faad14' },
])

const blogColumns = computed(() => [
  { title: t('blog.colTitle'), dataIndex: 'title', key: 'title', ellipsis: true },
  { title: t('blog.colCategory'), dataIndex: 'category', key: 'category', width: 120 },
  { title: t('blog.colVisibility'), dataIndex: 'visibility', key: 'visibility', width: 110 },
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

function saveProfile() {
  Object.assign(profile, editForm)
  editMode.value = false
  message.success(t('personal.profileUpdated'))
}

function changePassword() {
  if (!passwordForm.current || !passwordForm.next) {
    message.warning(t('personal.fillAllFields'))
    return
  }
  if (passwordForm.next !== passwordForm.confirm) {
    message.error(t('personal.passwordsNoMatch'))
    return
  }
  showPasswordModal.value = false
  Object.assign(passwordForm, { current: '', next: '', confirm: '' })
  message.success(t('personal.passwordChanged'))
}

function logout() {
  Modal.confirm({
    title: t('personal.signOutTitle'),
    content: t('personal.signOutContent'),
    okText: t('personal.yesSignOut'),
    cancelText: t('personal.cancel'),
    onOk() {
      clearAuth()
      router.push('/login')
    },
  })
}
</script>

<template>
  <div class="personal-view">
    <a-row :gutter="[24, 24]">
      <!-- Left: Profile Card -->
      <a-col :xs="24" :lg="8">
        <a-card :bordered="false" class="profile-card">
          <div class="avatar-section">
            <a-avatar :size="96" :style="{ background: '#667eea', fontSize: '36px' }">
              {{ profile.name.charAt(0).toUpperCase() }}
            </a-avatar>
            <h2>{{ profile.name }}</h2>
            <p class="username">@{{ profile.username }}</p>
            <p class="bio">{{ profile.bio }}</p>
          </div>

          <a-divider />

          <div class="profile-meta">
            <div class="meta-item"><MailOutlined /> {{ profile.email }}</div>
            <div class="meta-item">📍 {{ profile.location }}</div>
            <div class="meta-item">🔗 {{ profile.website }}</div>
            <div class="meta-item">📅 {{ t('personal.joined') }} {{ profile.joinDate }}</div>
          </div>

          <a-divider />

          <div class="action-buttons">
            <a-button type="primary" block @click="editMode = true">
              <EditOutlined /> {{ t('personal.editProfile') }}
            </a-button>
            <a-button block @click="showPasswordModal = true" style="margin-top: 8px">
              <LockOutlined /> {{ t('personal.changePassword') }}
            </a-button>
            <a-button danger block @click="logout" style="margin-top: 8px">
              <LogoutOutlined /> {{ t('personal.signOut') }}
            </a-button>
          </div>
        </a-card>
      </a-col>

      <!-- Right: Activity & Edit -->
      <a-col :xs="24" :lg="16">
        <!-- Activity stats -->
        <a-row :gutter="[16, 16]" style="margin-bottom: 24px">
          <a-col :span="8" v-for="item in activityData" :key="item.label()">
            <a-card :bordered="false" class="activity-card" style="text-align: center">
              <component :is="item.icon" :style="{ fontSize: '28px', color: item.color }" />
              <div class="activity-value" :style="{ color: item.color }">{{ item.value }}</div>
              <div class="activity-label">{{ item.label() }}</div>
            </a-card>
          </a-col>
        </a-row>

        <!-- Edit Profile Form -->
        <a-card v-if="editMode" :title="t('personal.editProfileTitle')" :bordered="false">
          <a-form layout="vertical">
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('personal.fullName')">
                  <a-input v-model:value="editForm.name" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('personal.username')">
                  <a-input v-model:value="editForm.username" />
                </a-form-item>
              </a-col>
            </a-row>
            <a-form-item :label="t('personal.email')">
              <a-input v-model:value="editForm.email">
                <template #prefix><MailOutlined /></template>
              </a-input>
            </a-form-item>
            <a-form-item :label="t('personal.bio')">
              <a-textarea v-model:value="editForm.bio" :rows="3" />
            </a-form-item>
            <a-row :gutter="16">
              <a-col :span="12">
                <a-form-item :label="t('personal.location')">
                  <a-input v-model:value="editForm.location" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item :label="t('personal.website')">
                  <a-input v-model:value="editForm.website" />
                </a-form-item>
              </a-col>
            </a-row>
            <div style="display: flex; gap: 8px">
              <a-button type="primary" @click="saveProfile">{{ t('personal.saveChanges') }}</a-button>
              <a-button @click="editMode = false">{{ t('personal.cancel') }}</a-button>
            </div>
          </a-form>
        </a-card>

        <!-- My Blogs -->
        <a-card v-else :title="t('personal.myBlogs')" :bordered="false">
          <a-table
            :columns="blogColumns"
            :data-source="myPosts"
            :loading="loadingMyPosts"
            :row-key="(r: Post) => r.id"
            :pagination="{ pageSize: 8, showSizeChanger: false }"
            :scroll="{ x: 720 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'title'">
                <a class="post-link" @click="router.push('/blog/' + record.id)">{{ record.title }}</a>
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
      </a-col>
    </a-row>

    <!-- Change Password Modal -->
    <a-modal
      v-model:open="showPasswordModal"
      :title="t('personal.changePassword')"
      :ok-text="t('personal.updatePassword')"
      :cancel-text="t('personal.cancel')"
      @ok="changePassword"
    >
      <a-form layout="vertical" style="margin-top: 16px">
        <a-form-item :label="t('personal.currentPassword')">
          <a-input-password v-model:value="passwordForm.current" />
        </a-form-item>
        <a-form-item :label="t('personal.newPassword')">
          <a-input-password v-model:value="passwordForm.next" />
        </a-form-item>
        <a-form-item :label="t('personal.confirmNewPassword')">
          <a-input-password v-model:value="passwordForm.confirm" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.personal-view { display: flex; flex-direction: column; gap: 24px; }
.profile-card { border-radius: 12px; }
.avatar-section { text-align: center; padding-bottom: 8px; }
.avatar-section h2 { margin: 16px 0 4px; font-size: 22px; font-weight: 700; }
.username { color: #888; margin: 0 0 8px; }
.bio { color: #666; font-size: 14px; line-height: 1.6; }
.profile-meta { display: flex; flex-direction: column; gap: 10px; }
.meta-item { display: flex; align-items: center; gap: 8px; font-size: 14px; color: #555; }
.activity-card { border-radius: 10px; }
.activity-value { font-size: 28px; font-weight: 700; margin: 8px 0 4px; }
.activity-label { font-size: 13px; color: #888; }
.post-link { color: #1890ff; cursor: pointer; }
.post-link:hover { text-decoration: underline; }
</style>
