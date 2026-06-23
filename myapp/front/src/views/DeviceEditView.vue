<script lang="ts" setup>
import { reactive, ref, watchEffect } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { getDevice, updateDevice, type DeviceStatus } from '../stores/device'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const deviceId = Number(route.params.id)
const notFound = ref(false)
const submitting = ref(false)

const form = reactive({
  serialNumber: '',
  model: '',
  status: 'active' as DeviceStatus,
  outgoingDate: '',
  installationDate: '',
  warrantyYears: 1,
  contactPerson: '',
  contactPhone: '',
  latitude: null as number | null,
  longitude: null as number | null,
  address: '',
})

watchEffect(() => {
  const device = getDevice(deviceId)
  if (!device) {
    notFound.value = true
    return
  }
  form.serialNumber = device.serialNumber
  form.model = device.model
  form.status = device.status
  form.outgoingDate = device.outgoingDate
  form.installationDate = device.installationDate
  form.warrantyYears = device.warrantyYears
  form.contactPerson = device.contactPerson
  form.contactPhone = device.contactPhone
  form.latitude = device.latitude
  form.longitude = device.longitude
  form.address = device.address
})

const statusOptions: { value: DeviceStatus; labelKey: string }[] = [
  { value: 'active', labelKey: 'device.statusActive' },
  { value: 'inactive', labelKey: 'device.statusInactive' },
  { value: 'maintenance', labelKey: 'device.statusMaintenance' },
]

async function handleSubmit() {
  if (!form.serialNumber.trim() || !form.model.trim() || !form.address.trim()) {
    message.warning(t('editDevice.fillRequired'))
    return
  }
  submitting.value = true
  await new Promise(r => setTimeout(r, 400))
  updateDevice(deviceId, {
    serialNumber: form.serialNumber.trim(),
    model: form.model.trim(),
    status: form.status,
    outgoingDate: form.outgoingDate,
    installationDate: form.installationDate,
    warrantyYears: form.warrantyYears,
    contactPerson: form.contactPerson.trim(),
    contactPhone: form.contactPhone.trim(),
    latitude: form.latitude,
    longitude: form.longitude,
    address: form.address.trim(),
  })
  submitting.value = false
  message.success(t('editDevice.updated'))
  router.push('/device/' + deviceId)
}

function handleCancel() {
  router.push('/device/' + deviceId)
}
</script>

<template>
  <div class="edit-device-view">
    <template v-if="!notFound">
      <a-page-header
        :title="t('editDevice.pageTitle')"
        :sub-title="t('editDevice.pageSubtitle')"
        @back="handleCancel"
      />

      <a-card :bordered="false" class="form-card">
        <a-form layout="vertical" :model="form" @finish="handleSubmit">

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="t('addDevice.serialNumber')" required>
                <a-input
                  v-model:value="form.serialNumber"
                  :placeholder="t('addDevice.serialNumberPlaceholder')"
                  size="large"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="t('addDevice.model')" required>
                <a-input
                  v-model:value="form.model"
                  :placeholder="t('addDevice.modelPlaceholder')"
                  size="large"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item :label="t('addDevice.status')">
                <a-select v-model:value="form.status" size="large">
                  <a-select-option v-for="opt in statusOptions" :key="opt.value" :value="opt.value">
                    {{ t(opt.labelKey) }}
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('addDevice.outgoingDate')">
                <a-date-picker
                  v-model:value="form.outgoingDate"
                  value-format="YYYY-MM-DD"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('addDevice.installationDate')">
                <a-date-picker
                  v-model:value="form.installationDate"
                  value-format="YYYY-MM-DD"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item :label="t('addDevice.warrantyYears')">
                <a-input-number
                  v-model:value="form.warrantyYears"
                  :min="1"
                  :max="99"
                  :precision="0"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('addDevice.contactPerson')">
                <a-input
                  v-model:value="form.contactPerson"
                  :placeholder="t('addDevice.contactPersonPlaceholder')"
                  size="large"
                />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('addDevice.contactPhone')">
                <a-input
                  v-model:value="form.contactPhone"
                  :placeholder="t('addDevice.contactPhonePlaceholder')"
                  size="large"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-row :gutter="16">
            <a-col :span="12">
              <a-form-item :label="t('addDevice.latitude')">
                <a-input-number
                  v-model:value="form.latitude"
                  :placeholder="t('addDevice.latitudePlaceholder')"
                  :precision="6"
                  :step="0.0001"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
            <a-col :span="12">
              <a-form-item :label="t('addDevice.longitude')">
                <a-input-number
                  v-model:value="form.longitude"
                  :placeholder="t('addDevice.longitudePlaceholder')"
                  :precision="6"
                  :step="0.0001"
                  size="large"
                  style="width: 100%"
                />
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item :label="t('addDevice.address')" required>
            <a-textarea
              v-model:value="form.address"
              :placeholder="t('addDevice.addressPlaceholder')"
              :rows="3"
            />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting" size="large">
                {{ t('editDevice.update') }}
              </a-button>
              <a-button size="large" @click="handleCancel">{{ t('editDevice.cancel') }}</a-button>
            </a-space>
          </a-form-item>

        </a-form>
      </a-card>
    </template>

    <a-result
      v-else
      status="404"
      :title="t('editDevice.notFound')"
    >
      <template #extra>
        <a-button type="primary" @click="router.push('/device')">{{ t('editDevice.backToList') }}</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.edit-device-view { display: flex; flex-direction: column; gap: 16px; }
.form-card { border-radius: 10px; }
</style>
