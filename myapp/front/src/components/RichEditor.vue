<script lang="ts" setup>
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import { Image } from '@tiptap/extension-image'
import { Link } from '@tiptap/extension-link'
import { TextStyle } from '@tiptap/extension-text-style'
import { Color } from '@tiptap/extension-color'
import { Underline } from '@tiptap/extension-underline'
import { TextAlign } from '@tiptap/extension-text-align'
import { Placeholder } from '@tiptap/extension-placeholder'
import { Extension } from '@tiptap/core'
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import {
  BoldOutlined,
  ItalicOutlined,
  UnderlineOutlined,
  StrikethroughOutlined,
  OrderedListOutlined,
  UnorderedListOutlined,
  AlignLeftOutlined,
  AlignCenterOutlined,
  AlignRightOutlined,
  LinkOutlined,
  PictureOutlined,
  UndoOutlined,
  RedoOutlined,
  MinusOutlined,
} from '@ant-design/icons-vue'

const props = defineProps<{ modelValue?: string; placeholder?: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

// FontSize extension
const FontSize = Extension.create({
  name: 'fontSize',
  addGlobalAttributes() {
    return [{
      types: ['textStyle'],
      attributes: {
        fontSize: {
          default: null,
          parseHTML: el => el.style.fontSize || null,
          renderHTML: attrs => attrs.fontSize ? { style: `font-size: ${attrs.fontSize}` } : {},
        },
      },
    }]
  },
})

const editor = useEditor({
  content: props.modelValue ?? '',
  extensions: [
    StarterKit,
    TextStyle,
    FontSize,
    Color,
    Underline,
    TextAlign.configure({ types: ['heading', 'paragraph'] }),
    Image.configure({ inline: false, allowBase64: true }),
    Link.configure({ openOnClick: false, autolink: true }),
    Placeholder.configure({ placeholder: props.placeholder ?? 'Write your blog content here...' }),
  ],
  onUpdate({ editor }) {
    emit('update:modelValue', editor.getHTML())
  },
})

watch(() => props.modelValue, (val) => {
  if (!editor.value) return
  if (val !== editor.value.getHTML()) {
    editor.value.commands.setContent(val ?? '', false)
  }
})

const FONT_SIZES = ['12px', '14px', '16px', '18px', '20px', '24px', '28px', '32px', '36px']
const currentFontSize = ref('16px')

function setFontSize(size: string) {
  currentFontSize.value = size
  editor.value?.chain().focus().setMark('textStyle', { fontSize: size }).run()
}

// Link modal
const linkModalVisible = ref(false)
const linkUrl = ref('')
function openLinkModal() {
  linkUrl.value = editor.value?.getAttributes('link').href ?? ''
  linkModalVisible.value = true
}
function applyLink() {
  if (!linkUrl.value) {
    editor.value?.chain().focus().unsetLink().run()
  } else {
    editor.value?.chain().focus().setLink({ href: linkUrl.value }).run()
  }
  linkModalVisible.value = false
  linkUrl.value = ''
}

// Image modal
const imageModalVisible = ref(false)
const imageUrl = ref('')
const imageFileRef = ref<HTMLInputElement | null>(null)
function openImageModal() {
  imageUrl.value = ''
  imageModalVisible.value = true
}
function applyImageUrl() {
  if (imageUrl.value) {
    editor.value?.chain().focus().setImage({ src: imageUrl.value }).run()
    imageModalVisible.value = false
    imageUrl.value = ''
  }
}
function handleImageFile(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) { message.warning('Image must be under 5 MB'); return }
  const reader = new FileReader()
  reader.onload = ev => {
    editor.value?.chain().focus().setImage({ src: ev.target?.result as string }).run()
    imageModalVisible.value = false
  }
  reader.readAsDataURL(file)
}

defineExpose({ getHTML: () => editor.value?.getHTML() ?? '' })
</script>

<template>
  <div class="rich-editor">
    <!-- Toolbar -->
    <div class="toolbar" v-if="editor">
      <!-- Headings -->
      <a-select size="small" style="width: 110px" :value="null" placeholder="Paragraph" @change="(v: string) => {
        if (v === 'p') editor?.chain().focus().setParagraph().run()
        else editor?.chain().focus().setHeading({ level: Number(v) as any }).run()
      }">
        <a-select-option value="p">Paragraph</a-select-option>
        <a-select-option value="1">Heading 1</a-select-option>
        <a-select-option value="2">Heading 2</a-select-option>
        <a-select-option value="3">Heading 3</a-select-option>
      </a-select>

      <a-divider type="vertical" />

      <!-- Font size -->
      <a-select size="small" style="width: 80px" :value="currentFontSize" @change="setFontSize">
        <a-select-option v-for="s in FONT_SIZES" :key="s" :value="s">{{ s }}</a-select-option>
      </a-select>

      <a-divider type="vertical" />

      <!-- Inline formatting -->
      <a-tooltip title="Bold">
        <a-button size="small" :type="editor.isActive('bold') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleBold().run()">
          <BoldOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Italic">
        <a-button size="small" :type="editor.isActive('italic') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleItalic().run()">
          <ItalicOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Underline">
        <a-button size="small" :type="editor.isActive('underline') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleUnderline().run()">
          <UnderlineOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Strikethrough">
        <a-button size="small" :type="editor.isActive('strike') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleStrike().run()">
          <StrikethroughOutlined />
        </a-button>
      </a-tooltip>

      <!-- Font color -->
      <a-tooltip title="Font Color">
        <label class="color-btn">
          <span class="color-icon">A</span>
          <input type="color" @input="(e) => editor?.chain().focus().setColor((e.target as HTMLInputElement).value).run()" />
        </label>
      </a-tooltip>

      <a-divider type="vertical" />

      <!-- Lists -->
      <a-tooltip title="Bullet List">
        <a-button size="small" :type="editor.isActive('bulletList') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleBulletList().run()">
          <UnorderedListOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Numbered List">
        <a-button size="small" :type="editor.isActive('orderedList') ? 'primary' : 'text'"
          @click="editor.chain().focus().toggleOrderedList().run()">
          <OrderedListOutlined />
        </a-button>
      </a-tooltip>

      <a-divider type="vertical" />

      <!-- Alignment -->
      <a-tooltip title="Align Left">
        <a-button size="small" :type="editor.isActive({ textAlign: 'left' }) ? 'primary' : 'text'"
          @click="editor.chain().focus().setTextAlign('left').run()">
          <AlignLeftOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Align Center">
        <a-button size="small" :type="editor.isActive({ textAlign: 'center' }) ? 'primary' : 'text'"
          @click="editor.chain().focus().setTextAlign('center').run()">
          <AlignCenterOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Align Right">
        <a-button size="small" :type="editor.isActive({ textAlign: 'right' }) ? 'primary' : 'text'"
          @click="editor.chain().focus().setTextAlign('right').run()">
          <AlignRightOutlined />
        </a-button>
      </a-tooltip>

      <a-divider type="vertical" />

      <!-- Divider line -->
      <a-tooltip title="Horizontal Rule">
        <a-button size="small" type="text" @click="editor.chain().focus().setHorizontalRule().run()">
          <MinusOutlined />
        </a-button>
      </a-tooltip>

      <!-- Link -->
      <a-tooltip title="Insert Link">
        <a-button size="small" :type="editor.isActive('link') ? 'primary' : 'text'" @click="openLinkModal">
          <LinkOutlined />
        </a-button>
      </a-tooltip>

      <!-- Image -->
      <a-tooltip title="Insert Image">
        <a-button size="small" type="text" @click="openImageModal">
          <PictureOutlined />
        </a-button>
      </a-tooltip>

      <a-divider type="vertical" />

      <!-- Undo / Redo -->
      <a-tooltip title="Undo">
        <a-button size="small" type="text" :disabled="!editor.can().undo()"
          @click="editor.chain().focus().undo().run()">
          <UndoOutlined />
        </a-button>
      </a-tooltip>
      <a-tooltip title="Redo">
        <a-button size="small" type="text" :disabled="!editor.can().redo()"
          @click="editor.chain().focus().redo().run()">
          <RedoOutlined />
        </a-button>
      </a-tooltip>
    </div>

    <!-- Editor content area -->
    <EditorContent :editor="editor" class="editor-content" />

    <!-- Link modal -->
    <a-modal v-model:open="linkModalVisible" title="Insert Link" width="400px" @ok="applyLink">
      <a-input v-model:value="linkUrl" placeholder="https://example.com" allow-clear />
    </a-modal>

    <!-- Image modal -->
    <a-modal v-model:open="imageModalVisible" title="Insert Image" width="440px" :footer="null">
      <a-tabs>
        <a-tab-pane key="url" tab="From URL">
          <a-space direction="vertical" style="width: 100%">
            <a-input v-model:value="imageUrl" placeholder="https://example.com/image.png" allow-clear />
            <a-button type="primary" block @click="applyImageUrl">Insert</a-button>
          </a-space>
        </a-tab-pane>
        <a-tab-pane key="upload" tab="Upload File">
          <input ref="imageFileRef" type="file" accept="image/*" style="display:none" @change="handleImageFile" />
          <a-button block @click="imageFileRef?.click()">Choose Image (max 5 MB)</a-button>
        </a-tab-pane>
      </a-tabs>
    </a-modal>
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid #d9d9d9;
  border-radius: 8px;
  overflow: hidden;
  transition: border-color 0.2s;
}
.rich-editor:focus-within {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
  padding: 6px 10px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}

.color-btn {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 24px;
  cursor: pointer;
  border-radius: 4px;
}
.color-btn:hover { background: #f0f0f0; }
.color-icon {
  font-weight: 700;
  font-size: 14px;
  line-height: 1;
  text-decoration: underline;
  text-decoration-style: solid;
}
.color-btn input[type="color"] {
  position: absolute;
  opacity: 0;
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.editor-content {
  min-height: 360px;
  padding: 16px;
  background: #fff;
}
</style>

<style>
/* Global — TipTap renders outside scoped context */
.ProseMirror {
  outline: none;
  min-height: 330px;
  line-height: 1.7;
  font-size: 15px;
}
.ProseMirror p.is-editor-empty:first-child::before {
  content: attr(data-placeholder);
  color: #bbb;
  pointer-events: none;
  float: left;
  height: 0;
}
.ProseMirror h1 { font-size: 2em; font-weight: 700; margin: 0.5em 0; }
.ProseMirror h2 { font-size: 1.5em; font-weight: 700; margin: 0.5em 0; }
.ProseMirror h3 { font-size: 1.2em; font-weight: 600; margin: 0.5em 0; }
.ProseMirror ul, .ProseMirror ol { padding-left: 1.4em; }
.ProseMirror img { max-width: 100%; border-radius: 4px; margin: 8px 0; }
.ProseMirror a { color: #1890ff; text-decoration: underline; }
.ProseMirror hr { border: none; border-top: 2px solid #f0f0f0; margin: 16px 0; }
.ProseMirror blockquote {
  border-left: 4px solid #d9d9d9;
  margin-left: 0;
  padding-left: 16px;
  color: #888;
}
.ProseMirror code { background: #f5f5f5; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }
.ProseMirror pre { background: #1e1e1e; color: #fff; padding: 16px; border-radius: 8px; overflow-x: auto; }
.ProseMirror pre code { background: none; padding: 0; }
</style>
