<script lang="ts" setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  CalendarOutlined,
  CalculatorOutlined,
  FunctionOutlined,
  CodeOutlined,
  QrcodeOutlined,
  GlobalOutlined,
} from '@ant-design/icons-vue'

const { t } = useI18n()

const tools = computed(() => [
  {
    key: 'calendar',
    label: t('tools.calendar'),
    desc: t('tools.calendarDesc'),
    url: 'https://wannianrili.bmcx.com',
    icon: CalendarOutlined,
  },
  {
    key: 'calculator',
    label: t('tools.calculator'),
    desc: t('tools.calculatorDesc'),
    url: 'https://www.23bei.com/tool-1.html',
    icon: CalculatorOutlined,
  },
  {
    key: 'base-conv',
    label: t('tools.baseConv'),
    desc: t('tools.baseConvDesc'),
    url: 'https://c.runoob.com/front-end/58/',
    icon: FunctionOutlined,
  },
  {
    key: 'json-fmt',
    label: t('tools.jsonFmt'),
    desc: t('tools.jsonFmtDesc'),
    url: 'https://www.json.cn',
    icon: CodeOutlined,
  },
  {
    key: 'qrcode',
    label: t('tools.qrcode'),
    desc: t('tools.qrcodeDesc'),
    url: 'https://cli.im/',
    icon: QrcodeOutlined,
  },
  {
    key: 'ip-lookup',
    label: t('tools.ipLookup'),
    desc: t('tools.ipLookupDesc'),
    url: 'https://www.ip138.com',
    icon: GlobalOutlined,
  },
])

function openTool(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 24px">{{ t('tools.title') }}</h2>
    <a-row :gutter="[16, 16]">
      <a-col
        v-for="tool in tools"
        :key="tool.key"
        :xs="24"
        :sm="12"
        :md="8"
        :lg="6"
      >
        <a-card hoverable @click="openTool(tool.url)" style="cursor: pointer; height: 100%">
          <template #cover>
            <div class="tool-cover">
              <component :is="tool.icon" class="tool-icon" />
            </div>
          </template>
          <a-card-meta :title="tool.label" :description="tool.desc" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.tool-cover {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 80px;
  background: #f5f5f5;
}
.tool-icon {
  font-size: 36px;
  color: #1890ff;
}
</style>
