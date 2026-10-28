<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { message, Modal } from 'ant-design-vue'
import {
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined,
  EditOutlined,
  KeyOutlined,
  TeamOutlined,
} from '@ant-design/icons-vue'
import {
  users,
  total,
  loading,
  query,
  roleFilter,
  disabledFilter,
  page,
  pageSize,
  fetchUsers,
  applyFilters,
  createUser,
  updateUser,
  setUserStatus,
  resetUserPassword,
} from '../stores/adminUser'
import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  AdminUserDTO,
  Gender,
  Role,
  UserType,
} from '../api/adminUser'
import { assignableRoles, fetchRoleConfigs, roleLabel } from '../stores/roleConfig'
import { getUser } from '../utils/auth'

const { t } = useI18n()

/** Own login, used to hide the actions the backend would refuse anyway. */
const currentLogin = getUser<{ login?: string }>()?.login ?? ''

const genders: Gender[] = ['MALE', 'FEMALE']

/** The built-in codes that ship with a translated label. */
const translatedRoles = ['USER', 'ADMIN']

/**
 * USER and ADMIN keep their translated labels so the page stays localised. A role added on
 * the Role Config page has no translation, so it shows the name configured there.
 */
function roleText(code?: string | null) {
  const role = code || 'USER'
  return translatedRoles.includes(role) ? t(`user.roles.${role}`) : roleLabel(role)
}
const userTypes: UserType[] = ['WEB', 'APP', 'MINI_PROGRAM']

const columns = computed(() => [
  { title: t('user.login'), key: 'login', width: 160, fixed: 'left' as const },
  { title: t('user.realName'), dataIndex: 'realName', key: 'realName', width: 140, ellipsis: true },
  { title: t('user.nickName'), dataIndex: 'nickName', key: 'nickName', width: 140, ellipsis: true },
  { title: t('user.email'), dataIndex: 'email', key: 'email', width: 200, ellipsis: true },
  { title: t('user.phoneNumber'), dataIndex: 'phoneNumber', key: 'phoneNumber', width: 140 },
  { title: t('user.role'), key: 'role', width: 110 },
  { title: t('user.status'), key: 'status', width: 120 },
  { title: t('user.createdDate'), key: 'createdDate', width: 180 },
  { title: t('user.actions'), key: 'actions', width: 220, fixed: 'right' as const },
])

const pagination = computed(() => ({
  current: page.value,
  pageSize: pageSize.value,
  total: total.value,
  showSizeChanger: true,
  pageSizeOptions: ['10', '20', '50'],
  showTotal: (n: number) => t('user.totalCount', { count: n }),
}))

onMounted(() => {
  load()
  // The role dropdowns are filled from the Role Config catalog, not a hard-coded list.
  fetchRoleConfigs()
})

async function load() {
  try {
    await fetchUsers()
  } catch {
    message.error(t('user.loadFailed'))
  }
}

async function handleSearch() {
  try {
    await applyFilters()
  } catch {
    message.error(t('user.loadFailed'))
  }
}

async function handleTableChange(pag: { current?: number; pageSize?: number }) {
  // A page-size change restarts at the first page, which is what the table itself expects.
  if (pag.pageSize && pag.pageSize !== pageSize.value) {
    pageSize.value = pag.pageSize
    page.value = 1
  } else {
    page.value = pag.current ?? 1
  }
  await load()
}

function resetFilters() {
  query.value = ''
  roleFilter.value = undefined
  disabledFilter.value = undefined
  handleSearch()
}

function formatInstant(value?: string | null): string {
  if (!value) return '—'
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString()
}

/** Surface the backend's own message when it refuses an action, so the rule is visible. */
function errorOf(e: unknown, fallback: string): string {
  const detail = (e as { response?: { data?: { message?: string } } })?.response?.data?.message
  return detail || fallback
}

function isSelf(record: AdminUserDTO): boolean {
  return !!currentLogin && record.login === currentLogin
}

// --- Create / edit --------------------------------------------------------

type EditableForm = {
  login: string
  realName: string
  nickName: string
  email: string
  password: string
  role: Role
  gender?: Gender
  userType?: UserType
  phoneNumber: string
  description: string
}

const formOpen = ref(false)
const saving = ref(false)
/** Null while creating, the account being changed while editing. */
const editing = ref<AdminUserDTO | null>(null)
const form = ref<EditableForm>(emptyForm())

function emptyForm(): EditableForm {
  return {
    login: '',
    realName: '',
    nickName: '',
    email: '',
    password: '',
    role: 'USER',
    gender: undefined,
    userType: 'WEB',
    phoneNumber: '',
    description: '',
  }
}

function openCreate() {
  editing.value = null
  form.value = emptyForm()
  formOpen.value = true
}

function openEdit(record: AdminUserDTO) {
  editing.value = record
  form.value = {
    login: record.login,
    realName: record.realName ?? '',
    nickName: record.nickName ?? '',
    email: record.email ?? '',
    password: '',
    role: record.role ?? 'USER',
    gender: record.gender ?? undefined,
    userType: record.userType ?? undefined,
    phoneNumber: record.phoneNumber ?? '',
    description: record.description ?? '',
  }
  formOpen.value = true
}

/** Blank optional fields are sent as undefined so the backend stores null, not "". */
function trimmed(value: string): string | undefined {
  const v = value.trim()
  return v ? v : undefined
}

async function handleSave() {
  const f = form.value
  if (!editing.value && !f.login.trim()) {
    message.warning(t('user.loginRequired'))
    return
  }
  if (!f.realName.trim()) {
    message.warning(t('user.realNameRequired'))
    return
  }
  if (!f.email.trim()) {
    message.warning(t('user.emailRequired'))
    return
  }
  if (!editing.value && f.password.length < 6) {
    message.warning(t('user.passwordTooShort'))
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      const payload: AdminUpdateUserRequest = {
        realName: f.realName.trim(),
        email: f.email.trim(),
        role: f.role,
        nickName: trimmed(f.nickName),
        gender: f.gender,
        userType: f.userType,
        phoneNumber: trimmed(f.phoneNumber),
        description: trimmed(f.description),
      }
      await updateUser(editing.value.id, payload)
      message.success(t('user.updated'))
    } else {
      const payload: AdminCreateUserRequest = {
        login: f.login.trim(),
        realName: f.realName.trim(),
        email: f.email.trim(),
        password: f.password,
        role: f.role,
        nickName: trimmed(f.nickName),
        gender: f.gender,
        userType: f.userType,
        phoneNumber: trimmed(f.phoneNumber),
        description: trimmed(f.description),
      }
      await createUser(payload)
      message.success(t('user.created'))
    }
    formOpen.value = false
  } catch (e: unknown) {
    message.error(errorOf(e, t('user.saveFailed')))
  } finally {
    saving.value = false
  }
}

// --- Reset password -------------------------------------------------------

const passwordOpen = ref(false)
const resetting = ref(false)
const passwordTarget = ref<AdminUserDTO | null>(null)
const newPassword = ref('')

function openResetPassword(record: AdminUserDTO) {
  passwordTarget.value = record
  newPassword.value = ''
  passwordOpen.value = true
}

async function handleResetPassword() {
  const target = passwordTarget.value
  if (!target) return
  if (newPassword.value.length < 6) {
    message.warning(t('user.passwordTooShort'))
    return
  }
  resetting.value = true
  try {
    await resetUserPassword(target.id, newPassword.value)
    message.success(t('user.passwordReset'))
    passwordOpen.value = false
    newPassword.value = ''
  } catch (e: unknown) {
    message.error(errorOf(e, t('user.saveFailed')))
  } finally {
    resetting.value = false
  }
}

// --- Enable / disable -----------------------------------------------------

/**
 * Accounts are disabled, not deleted: their login is referenced by value in blogs,
 * comments and watch lists.
 */
const disableOpen = ref(false)
const disabling = ref(false)
const disableTarget = ref<AdminUserDTO | null>(null)
const disableReason = ref('')

function handleToggleStatus(record: AdminUserDTO) {
  if (!record.deleted) {
    disableTarget.value = record
    disableReason.value = ''
    disableOpen.value = true
    return
  }
  Modal.confirm({
    title: t('user.enableConfirm'),
    content: t('user.enableHint'),
    okText: t('user.enable'),
    async onOk() {
      try {
        await setUserStatus(record.id, false)
        message.success(t('user.enabled'))
      } catch (e: unknown) {
        message.error(errorOf(e, t('user.saveFailed')))
      }
    },
  })
}

async function handleDisable() {
  const target = disableTarget.value
  if (!target) return
  disabling.value = true
  try {
    await setUserStatus(target.id, true, disableReason.value.trim() || undefined)
    message.success(t('user.disabled'))
    disableOpen.value = false
  } catch (e: unknown) {
    message.error(errorOf(e, t('user.saveFailed')))
  } finally {
    disabling.value = false
  }
}
</script>

<template>
  <div class="user-list-view">
    <a-card :bordered="false">
      <template #title>
        <TeamOutlined style="color: #1890ff; margin-right: 8px" />{{ t('user.title') }}
      </template>
      <template #extra>
        <a-space>
          <a-button :loading="loading" @click="load">
            <template #icon><ReloadOutlined /></template>
            {{ t('user.refresh') }}
          </a-button>
          <a-button type="primary" @click="openCreate">
            <template #icon><PlusOutlined /></template>
            {{ t('user.create') }}
          </a-button>
        </a-space>
      </template>

      <div class="filter-bar">
        <a-input
          v-model:value="query"
          :placeholder="t('user.searchPlaceholder')"
          allow-clear
          style="width: 240px"
          @press-enter="handleSearch"
        />
        <a-select
          v-model:value="roleFilter"
          :placeholder="t('user.role')"
          allow-clear
          style="width: 140px"
        >
          <a-select-option v-for="r in assignableRoles" :key="r.code" :value="r.code">
            {{ roleText(r.code) }}
          </a-select-option>
        </a-select>
        <a-select
          v-model:value="disabledFilter"
          :placeholder="t('user.status')"
          allow-clear
          style="width: 140px"
        >
          <a-select-option :value="false">{{ t('user.active') }}</a-select-option>
          <a-select-option :value="true">{{ t('user.disabledLabel') }}</a-select-option>
        </a-select>
        <a-button type="primary" @click="handleSearch">
          <template #icon><SearchOutlined /></template>
          {{ t('user.search') }}
        </a-button>
        <a-button @click="resetFilters">{{ t('user.reset') }}</a-button>
      </div>

      <a-table
        :columns="columns"
        :data-source="users"
        :loading="loading"
        :row-key="(r: AdminUserDTO) => r.id"
        :pagination="pagination"
        :scroll="{ x: 1400 }"
        @change="handleTableChange"
      >
        <template #emptyText>
          <a-empty :description="t('user.empty')" />
        </template>
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'login'">
            <a-space :size="4">
              <span class="login">{{ record.login }}</span>
              <a-tag v-if="isSelf(record)" color="blue">{{ t('user.you') }}</a-tag>
            </a-space>
          </template>
          <template v-else-if="column.key === 'role'">
            <a-tag :color="record.role === 'ADMIN' ? 'gold' : 'default'">
              {{ roleText(record.role) }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-tooltip v-if="record.deleted && record.deleteReason" :title="record.deleteReason">
              <a-tag color="red">{{ t('user.disabledLabel') }}</a-tag>
            </a-tooltip>
            <a-tag v-else-if="record.deleted" color="red">{{ t('user.disabledLabel') }}</a-tag>
            <a-tag v-else color="green">{{ t('user.active') }}</a-tag>
          </template>
          <template v-else-if="column.key === 'createdDate'">
            {{ formatInstant(record.createdDate) }}
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space :size="4">
              <a-button type="link" size="small" @click="openEdit(record)">
                <template #icon><EditOutlined /></template>
                {{ t('user.edit') }}
              </a-button>
              <a-button type="link" size="small" @click="openResetPassword(record)">
                <template #icon><KeyOutlined /></template>
                {{ t('user.resetPassword') }}
              </a-button>
              <a-button
                v-if="!isSelf(record)"
                type="link"
                size="small"
                :danger="!record.deleted"
                @click="handleToggleStatus(record)"
              >
                {{ record.deleted ? t('user.enable') : t('user.disable') }}
              </a-button>
            </a-space>
          </template>
          <template v-else>
            {{ record[column.dataIndex as keyof AdminUserDTO] || '—' }}
          </template>
        </template>
      </a-table>

      <a-modal
        v-model:open="formOpen"
        :title="editing ? t('user.editTitle') : t('user.createTitle')"
        :confirm-loading="saving"
        :ok-text="t('user.save')"
        width="560px"
        @ok="handleSave"
      >
        <a-form layout="vertical">
          <a-form-item :label="t('user.login')" required>
            <!-- A login is the identity other tables reference by value, so it is fixed after creation. -->
            <a-input v-model:value="form.login" :disabled="!!editing" :maxlength="50" />
          </a-form-item>
          <a-form-item v-if="!editing" :label="t('user.password')" required>
            <a-input-password v-model:value="form.password" :maxlength="100" />
          </a-form-item>
          <a-form-item :label="t('user.realName')" required>
            <a-input v-model:value="form.realName" :maxlength="512" />
          </a-form-item>
          <a-form-item :label="t('user.nickName')">
            <a-input v-model:value="form.nickName" :maxlength="512" />
          </a-form-item>
          <a-form-item :label="t('user.email')" required>
            <a-input v-model:value="form.email" :maxlength="512" />
          </a-form-item>
          <a-form-item :label="t('user.phoneNumber')">
            <a-input v-model:value="form.phoneNumber" :maxlength="32" />
          </a-form-item>
          <a-form-item :label="t('user.role')" required>
            <a-select v-model:value="form.role">
              <a-select-option v-for="r in assignableRoles" :key="r.code" :value="r.code">
                {{ roleText(r.code) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('user.gender')">
            <a-select v-model:value="form.gender" allow-clear>
              <a-select-option v-for="g in genders" :key="g" :value="g">
                {{ t(`user.genders.${g}`) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('user.userType')">
            <a-select v-model:value="form.userType" allow-clear>
              <a-select-option v-for="u in userTypes" :key="u" :value="u">
                {{ t(`user.userTypes.${u}`) }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="t('user.description')">
            <a-textarea v-model:value="form.description" :rows="2" :maxlength="200" show-count />
          </a-form-item>
        </a-form>
      </a-modal>

      <a-modal
        v-model:open="passwordOpen"
        :title="t('user.resetPasswordTitle', { login: passwordTarget?.login })"
        :confirm-loading="resetting"
        :ok-text="t('user.resetPassword')"
        @ok="handleResetPassword"
      >
        <a-form layout="vertical">
          <a-form-item :label="t('user.newPassword')" required>
            <a-input-password v-model:value="newPassword" :maxlength="100" />
          </a-form-item>
        </a-form>
        <a-alert :message="t('user.resetPasswordHint')" type="info" show-icon />
      </a-modal>

      <a-modal
        v-model:open="disableOpen"
        :title="t('user.disableConfirm')"
        :confirm-loading="disabling"
        :ok-text="t('user.disable')"
        ok-type="danger"
        @ok="handleDisable"
      >
        <p>{{ t('user.disableHint') }}</p>
        <a-form layout="vertical">
          <a-form-item :label="t('user.disableReason')">
            <a-input v-model:value="disableReason" :maxlength="100" />
          </a-form-item>
        </a-form>
      </a-modal>
    </a-card>
  </div>
</template>

<style scoped>
.user-list-view {
  padding: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.login {
  font-family: 'SF Mono', 'Consolas', monospace;
}
</style>
