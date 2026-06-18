<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'
import { getPost, updatePost } from '../stores/blog'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const post = computed(() => getPost(Number(route.params.id)))

const form = reactive({
  title: '',
  category: '',
  tag: '',
  tagColor: 'blue',
  content: '',
})

watch(post, (p) => {
  if (p) {
    form.title = p.title
    form.category = p.category
    form.tag = p.tag
    form.tagColor = p.tagColor
    form.content = p.content
  }
}, { immediate: true })

const categories = ['Frontend', 'Backend', 'Language', 'DevOps']
const tagColors = ['blue', 'green', 'orange', 'purple', 'red', 'cyan', 'geekblue']
const submitting = ref(false)

async function handleSubmit() {
  if (!form.title.trim() || !form.category || !form.content || form.content === '<p></p>') {
    message.warning(t('editBlog.fillRequired'))
    return
  }
  submitting.value = true
  await new Promise(r => setTimeout(r, 400))
  updatePost(Number(route.params.id), { ...form })
  submitting.value = false
  message.success(t('editBlog.updated'))
  router.push('/blog/' + route.params.id)
}
</script>

<template>
  <div class="blog-edit-view">
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
                  <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('editBlog.tag')">
                <a-input v-model:value="form.tag" :placeholder="t('editBlog.tagPlaceholder')" size="large" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item :label="t('editBlog.tagColor')">
                <a-select v-model:value="form.tagColor" size="large">
                  <a-select-option v-for="color in tagColors" :key="color" :value="color">
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

    <a-result v-else status="404" :title="t('editBlog.notFound')">
      <template #extra>
        <a-button type="primary" @click="router.push('/blog')">{{ t('editBlog.backToList') }}</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.blog-edit-view { display: flex; flex-direction: column; gap: 16px; }
.form-card { border-radius: 10px; }
</style>
