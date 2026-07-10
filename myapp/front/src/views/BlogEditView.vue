<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'
import { getPost, updatePost, type Post } from '../stores/blog'
import { categoryOptions, fetchBlogConfig, tagColorOptions, tagOptions } from '../stores/blogConfig'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const post = ref<Post | undefined>(undefined)
const loadingPost = ref(false)

const form = reactive({
  title: '',
  category: '',
  tag: '',
  tagColor: 'blue',
  content: '',
  excerpt: '',
})

onMounted(async () => {
  loadingPost.value = true
  try {
    await fetchBlogConfig()
    const found = await getPost(Number(route.params.id))
    if (found) {
      post.value = found
      form.title = found.title
      form.category = found.category
      form.tag = found.tag
      form.tagColor = found.tagColor
      form.content = found.content
      form.excerpt = found.excerpt
    }
  } finally {
    loadingPost.value = false
  }
})

const submitting = ref(false)

async function handleSubmit() {
  if (!form.title.trim() || !form.category || !form.content || form.content === '<p></p>') {
    message.warning(t('editBlog.fillRequired'))
    return
  }
  submitting.value = true
  try {
    await updatePost(Number(route.params.id), {
      title: form.title,
      category: form.category,
      tag: form.tag,
      tagColor: form.tagColor,
      content: form.content,
      excerpt: form.excerpt,
    })
    message.success(t('editBlog.updated'))
    router.push('/blog/' + route.params.id)
  } catch {
    message.error(t('editBlog.updateFailed') || 'Failed to update post')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="blog-edit-view">
    <a-spin :spinning="loadingPost">
      <template v-if="post">
        <a-page-header
          :title="t('editBlog.pageTitle')"
          :sub-title="t('editBlog.pageSubtitle')"
          @back="router.push('/blog/' + post.id)"
        />
        <a-card :bordered="false" class="form-card">
          <a-form layout="vertical" :model="form" @finish="handleSubmit">
            <a-form-item :label="t('editBlog.titleLabel')" required>
              <a-input v-model:value="form.title" :placeholder="t('editBlog.titlePlaceholder')" size="large" />
            </a-form-item>

            <a-row :gutter="16">
              <a-col :span="8">
                <a-form-item :label="t('editBlog.category')" required>
                  <a-select v-model:value="form.category" :placeholder="t('editBlog.categoryPlaceholder')" size="large">
                    <a-select-option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('editBlog.tag')">
                  <a-select
                    v-model:value="form.tag"
                    :placeholder="t('editBlog.tagPlaceholder')"
                    size="large"
                    show-search
                    allow-clear
                  >
                    <a-select-option v-for="tag in tagOptions" :key="tag" :value="tag">{{ tag }}</a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
              <a-col :span="8">
                <a-form-item :label="t('editBlog.tagColor')">
                  <a-select v-model:value="form.tagColor" size="large">
                    <a-select-option v-for="color in tagColorOptions" :key="color" :value="color">
                      <a-tag :color="color">{{ color }}</a-tag>
                    </a-select-option>
                  </a-select>
                </a-form-item>
              </a-col>
            </a-row>

            <a-form-item :label="t('editBlog.content')" required>
              <RichEditor v-model="form.content" :placeholder="t('editBlog.contentPlaceholder')" />
            </a-form-item>

            <a-form-item>
              <a-space>
                <a-button type="primary" html-type="submit" :loading="submitting" size="large">
                  {{ t('editBlog.update') }}
                </a-button>
                <a-button size="large" @click="router.push('/blog/' + post.id)">
                  {{ t('editBlog.cancel') }}
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </template>

      <a-result v-else-if="!loadingPost" status="404" :title="t('editBlog.notFound')">
        <template #extra>
          <a-button type="primary" @click="router.push('/blog')">{{ t('editBlog.backToList') }}</a-button>
        </template>
      </a-result>
    </a-spin>
  </div>
</template>

<style scoped>
.blog-edit-view { display: flex; flex-direction: column; gap: 16px; }
.form-card { border-radius: 10px; }
</style>
