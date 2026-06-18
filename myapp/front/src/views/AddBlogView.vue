<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'
import { addPost } from '../stores/blog'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  title: '',
  category: '',
  tag: '',
  tagColor: 'blue',
  content: '',
  excerpt: '',
})

const categories = ['Frontend', 'Backend', 'Language', 'DevOps']
const tagColors = ['blue', 'green', 'orange', 'purple', 'red', 'cyan', 'geekblue']

const submitting = ref(false)

async function handleSubmit() {
  if (!form.title.trim() || !form.category || !form.content || form.content === '<p></p>') {
    message.warning(t('addBlog.fillRequired'))
    return
  }
  submitting.value = true
  await new Promise(r => setTimeout(r, 600))
  addPost({
    title: form.title,
    category: form.category,
    tag: form.tag,
    tagColor: form.tagColor,
    content: form.content,
    excerpt: form.excerpt || form.title,
  })
  submitting.value = false
  message.success(t('addBlog.created'))
  router.push('/blog')
}

function handleCancel() {
  router.push('/blog')
}
</script>

<template>
  <div class="add-blog-view">
    <a-page-header
      :title="t('addBlog.pageTitle')"
      :sub-title="t('addBlog.pageSubtitle')"
      @back="handleCancel"
    />

    <a-card :bordered="false" class="form-card">
      <a-form layout="vertical" :model="form" @finish="handleSubmit">
        <a-form-item :label="t('addBlog.titleLabel')" required>
          <a-input v-model:value="form.title" :placeholder="t('addBlog.titlePlaceholder')" size="large" />
        </a-form-item>

        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item :label="t('addBlog.category')" required>
              <a-select v-model:value="form.category" :placeholder="t('addBlog.categoryPlaceholder')" size="large">
                <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('addBlog.tag')">
              <a-input v-model:value="form.tag" :placeholder="t('addBlog.tagPlaceholder')" size="large" />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('addBlog.tagColor')">
              <a-select v-model:value="form.tagColor" size="large">
                <a-select-option v-for="color in tagColors" :key="color" :value="color">
                  <a-tag :color="color">{{ color }}</a-tag>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('addBlog.content')" required>
          <RichEditor v-model="form.content" :placeholder="t('addBlog.contentPlaceholder')" />
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting" size="large">
              {{ t('addBlog.publish') }}
            </a-button>
            <a-button size="large" @click="handleCancel">{{ t('addBlog.cancel') }}</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.add-blog-view {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-card {
  border-radius: 10px;
}
</style>
