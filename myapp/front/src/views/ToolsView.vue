<script lang="ts" setup>
import {
  CalendarOutlined,
  CalculatorOutlined,
  FunctionOutlined,
  CodeOutlined,
  QrcodeOutlined,
  GlobalOutlined,
} from '@ant-design/icons-vue'

const tools = [
  {
    key: 'calendar',
    label: 'Calendar',
    desc: '万年历 — 日历查询',
    url: 'https://wannianrili.bmcx.com',
    icon: CalendarOutlined,
  },
  {
    key: 'calculator',
    label: 'Calculator',
    desc: '在线计算器',
    url: 'https://www.23bei.com/tool-1.html',
    icon: CalculatorOutlined,
  },
  {
    key: 'base-conv',
    label: 'Scientific / Base Conversion',
    desc: '进制转换',
    url: 'https://c.runoob.com/front-end/58/',
    icon: FunctionOutlined,
  },
  {
    key: 'json-fmt',
    label: 'JSON Formatter',
    desc: 'JSON 格式化 / 校验',
    url: 'https://www.json.cn',
    icon: CodeOutlined,
  },
  {
    key: 'qrcode',
    label: 'QR Code Generator',
    desc: '草料二维码生成',
    url: 'https://cli.im/',
    icon: QrcodeOutlined,
  },
  {
    key: 'ip-lookup',
    label: 'IP Lookup',
    desc: 'IP 地址查询',
    url: 'https://www.ip138.com',
    icon: GlobalOutlined,
  },
]

function openTool(url: string) {
  window.open(url, '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div>
    <h2 style="margin-bottom: 24px">Tools</h2>
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
