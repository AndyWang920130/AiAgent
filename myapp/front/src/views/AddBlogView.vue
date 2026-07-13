<script lang="ts" setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'
import { addPost } from '../stores/blog'
import { categoryOptions, fetchBlogConfig, tagColorOptions, tagOptions } from '../stores/blogConfig'

const { t } = useI18n()
const router = useRouter()

const form = reactive({
  title: '',
  category: '',
  tag: '',
  tagColor: 'blue',
  visibility: 'PUBLIC',
  content: '',
  excerpt: '',
})

const submitting = ref(false)

onMounted(() => {
  fetchBlogConfig()
})

async function handleSubmit() {
  if (!form.title.trim() || !form.category || !form.content || form.content === '<p></p>') {
    message.warning(t('addBlog.fillRequired'))
    return
  }
  submitting.value = true
  try {
    await addPost({
      title: form.title,
      category: form.category,
      tag: form.tag,
      tagColor: form.tagColor,
      visibility: form.visibility as 'PUBLIC' | 'PRIVATE',
      content: form.content,
      excerpt: form.excerpt || form.title,
    })
    message.success(t('addBlog.created'))
    router.push('/blog')
  } catch {
    message.error(t('addBlog.createFailed') || 'Failed to create post')
  } finally {
    submitting.value = false
  }
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
                <a-select-option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('addBlog.tag')">
              <a-select
                v-model:value="form.tag"
                :placeholder="t('addBlog.tagPlaceholder')"
                size="large"
                show-search
                allow-clear
              >
                <a-select-option v-for="tag in tagOptions" :key="tag" :value="tag">{{ tag }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item :label="t('addBlog.tagColor')">
              <a-select v-model:value="form.tagColor" size="large">
                <a-select-option v-for="color in tagColorOptions" :key="color" :value="color">
                  <a-tag :color="color">{{ color }}</a-tag>
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item :label="t('addBlog.visibility')">
          <a-radio-group v-model:value="form.visibility" option-type="button" button-style="solid">
            <a-radio-button value="PUBLIC">{{ t('addBlog.public') }}</a-radio-button>
            <a-radio-button value="PRIVATE">{{ t('addBlog.private') }}</a-radio-button>
          </a-radio-group>
        </a-form-item>

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
