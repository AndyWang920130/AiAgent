<script lang="ts" setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'
import { getPost, updatePost } from '../stores/blog'

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
    message.warning('Please fill in all required fields.')
    return
  }
  submitting.value = true
  await new Promise(r => setTimeout(r, 400))
  updatePost(Number(route.params.id), { ...form })
  submitting.value = false
  message.success('Post updated!')
  router.push('/blog/' + route.params.id)
}
</script>

<template>
  <div class="blog-edit-view">
    <template v-if="post">
      <a-page-header
        title="Edit Blog Post"
        sub-title="Update your article"
        @back="router.push('/blog/' + post.id)"
      />
      <a-card :bordered="false" class="form-card">
        <a-form layout="vertical" :model="form" @finish="handleSubmit">
          <a-form-item label="Title" required>
            <a-input v-model:value="form.title" placeholder="Enter blog title" size="large" />
          </a-form-item>

          <a-row :gutter="16">
            <a-col :span="8">
              <a-form-item label="Category" required>
                <a-select v-model:value="form.category" placeholder="Select category" size="large">
                  <a-select-option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="Tag">
                <a-input v-model:value="form.tag" placeholder="e.g. Vue, Java" size="large" />
              </a-form-item>
            </a-col>
            <a-col :span="8">
              <a-form-item label="Tag Color">
                <a-select v-model:value="form.tagColor" size="large">
                  <a-select-option v-for="color in tagColors" :key="color" :value="color">
                    <a-tag :color="color">{{ color }}</a-tag>
                  </a-select-option>
                </a-select>
              </a-form-item>
            </a-col>
          </a-row>

          <a-form-item label="Content" required>
            <RichEditor v-model="form.content" placeholder="Write your blog content here..." />
          </a-form-item>

          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" :loading="submitting" size="large">Update</a-button>
              <a-button size="large" @click="router.push('/blog/' + post.id)">Cancel</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </a-card>
    </template>

    <a-result v-else status="404" title="Post not found">
      <template #extra>
        <a-button type="primary" @click="router.push('/blog')">Back to List</a-button>
      </template>
    </a-result>
  </div>
</template>

<style scoped>
.blog-edit-view { display: flex; flex-direction: column; gap: 16px; }
.form-card { border-radius: 10px; }
</style>
