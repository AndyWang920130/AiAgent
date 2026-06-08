<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import RichEditor from '../components/RichEditor.vue'

const router = useRouter()

const form = reactive({
  title: '',
  category: '',
  tag: '',
  tagColor: 'blue',
  content: '',
})

const categories = ['Frontend', 'Backend', 'Language', 'DevOps']
const tagColors = ['blue', 'green', 'orange', 'purple', 'red', 'cyan', 'geekblue']

const submitting = ref(false)

async function handleSubmit() {
  if (!form.title.trim() || !form.category || !form.content || form.content === '<p></p>') {
    message.warning('Please fill in all required fields.')
    return
  }
  submitting.value = true
  // TODO: call API to save the blog post
  await new Promise(r => setTimeout(r, 600))
  submitting.value = false
  message.success('Blog post created!')
  router.push('/home')
}

function handleCancel() {
  router.push('/home')
}
</script>

<template>
  <div class="add-blog-view">
    <a-page-header title="New Blog Post" sub-title="Write and publish your article" @back="handleCancel" />

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
            <a-button type="primary" html-type="submit" :loading="submitting" size="large">Publish</a-button>
            <a-button size="large" @click="handleCancel">Cancel</a-button>
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
