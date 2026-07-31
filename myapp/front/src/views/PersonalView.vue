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
import { clearAuth, getUser, setUser } from '../utils/auth'
import { deletePost, fetchMyPosts, loadingMyPosts, myPosts, type Post } from '../stores/blog'
import { blogApi } from '../api/blog'
import { achievementApi } from '../api/achievement'
import { authApi } from '../api/auth'

const { t } = useI18n()
const router = useRouter()

const storedUser = getUser<{ username: string; name: string; email?: string }>()

// Seeded from the cached login user; the authoritative values are loaded from the backend
// on mount (loadProfile). Only name, email and bio are editable — username is immutable.
const profile = reactive({
  name: storedUser?.name || '',
  username: storedUser?.username || '',
  email: storedUser?.email || '',
  bio: '',
  joinDate: '',
})

const editMode = ref(false)
const savingProfile = ref(false)
const editForm = reactive({ name: '', email: '', bio: '' })
const passwordForm = reactive({ current: '', next: '', confirm: '' })
const showPasswordModal = ref(false)
const likesReceived = ref(0)
const achievementPoints = ref(0)
const achievementItems = ref<{ type: string; points: number }[]>([])

async function loadProfile() {
  const data = await authApi.getProfile()
  profile.name = data.name || ''
  profile.username = data.username || ''
  profile.email = data.email || ''
  profile.bio = data.bio || ''
  profile.joinDate = data.joinDate ? data.joinDate.slice(0, 10) : ''
}

onMounted(async () => {
  await Promise.all([
    loadProfile().catch(() => {}),
    fetchMyPosts(),
    blogApi.getMyLikesReceived().then(total => {
      likesReceived.value = total
    }),
    achievementApi.getMy().then(summary => {
      achievementPoints.value = summary.total
      achievementItems.value = summary.items
    }),
  ])
})

const activityData = computed(() => [
  { label: () => t('personal.postsWritten'), value: myPosts.value.length, icon: FileTextOutlined, color: '#1890ff' },
  { label: () => t('personal.likesReceived'), value: likesReceived.value, icon: HeartOutlined, color: '#eb2f96' },
  { label: () => t('personal.achievements'), value: achievementPoints.value, icon: TrophyOutlined, color: '#faad14' },
])

const achievementLabelKeys: Record<string, string> = {
  REGISTRATION: 'personal.achRegistration',
  PUBLISH_ARTICLE: 'personal.achPublish',
  RECEIVE_LIKE: 'personal.achLike',
}

function achLabel(type: string): string {
  const key = achievementLabelKeys[type]
  return key ? t(key) : type
}

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

function startEdit() {
  // Sync the form from the current (loaded) profile so it never shows stale/empty values.
  editForm.name = profile.name
  editForm.email = profile.email
  editForm.bio = profile.bio
  editMode.value = true
}

async function saveProfile() {
  if (!editForm.name.trim() || !editForm.email.trim()) {
    message.warning(t('personal.fillAllFields'))
    return
  }
  savingProfile.value = true
  try {
    const updated = await authApi.updateProfile({
      name: editForm.name.trim(),
      email: editForm.email.trim(),
      bio: editForm.bio,
    })
    profile.name = updated.name || ''
    profile.email = updated.email || ''
    profile.bio = updated.bio || ''
    // Keep the cached login user in sync so greetings elsewhere reflect the new name/email.
    const cached = getUser<Record<string, unknown>>() || {}
    setUser({ ...cached, name: profile.name, email: profile.email })
    editMode.value = false
    message.success(t('personal.profileUpdated'))
  } catch (err: any) {
    message.error(err?.response?.data?.message || t('personal.profileUpdateFailed'))
  } finally {
    savingProfile.value = false
  }
}

const changingPassword = ref(false)

async function changePassword() {
  if (!passwordForm.current || !passwordForm.next) {
    message.warning(t('personal.fillAllFields'))
    return
  }
  if (passwordForm.next.length < 6) {
    message.error(t('personal.passwordTooShort'))
    return
  }
  if (passwordForm.next !== passwordForm.confirm) {
    message.error(t('personal.passwordsNoMatch'))
    return
  }
  changingPassword.value = true
  try {
    await authApi.changePassword({
      currentPassword: passwordForm.current,
      newPassword: passwordForm.next,
    })
    showPasswordModal.value = false
    Object.assign(passwordForm, { current: '', next: '', confirm: '' })
    message.success(t('personal.passwordChanged'))
  } catch (err: any) {
    // Surface the backend message (e.g. wrong current password); keep the modal open.
    message.error(err?.response?.data?.message || t('personal.passwordChangeFailed'))
  } finally {
    changingPassword.value = false
  }
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
            <div class="meta-item" v-if="profile.joinDate">📅 {{ t('personal.joined') }} {{ profile.joinDate }}</div>
          </div>

          <a-divider />

          <div class="action-buttons">
            <a-button type="primary" block @click="startEdit">
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

        <!-- Achievement breakdown -->
        <a-card :title="t('personal.achievementsBreakdown')" :bordered="false" style="margin-bottom: 24px">
          <a-empty v-if="achievementItems.length === 0" :description="t('personal.noAchievements')" />
          <a-list v-else :data-source="achievementItems" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="ach-row">
                  <span class="ach-label">{{ achLabel(item.type) }}</span>
                  <a-tag color="gold" class="ach-points">{{ item.points }}</a-tag>
                </div>
              </a-list-item>
            </template>
          </a-list>
        </a-card>

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
                  <a-input :value="profile.username" disabled />
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
            <div style="display: flex; gap: 8px">
              <a-button type="primary" :loading="savingProfile" @click="saveProfile">{{ t('personal.saveChanges') }}</a-button>
              <a-button :disabled="savingProfile" @click="editMode = false">{{ t('personal.cancel') }}</a-button>
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
      :confirm-loading="changingPassword"
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
.ach-row { display: flex; align-items: center; justify-content: space-between; width: 100%; }
.ach-label { font-size: 14px; color: #555; }
.ach-points { margin: 0; font-weight: 600; }
</style>
