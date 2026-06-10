import { ref } from 'vue'

export interface Post {
  id: number
  title: string
  excerpt: string
  category: string
  date: string
  views: number
  likes: number
  comments: number
  tag: string
  tagColor: string
  content: string
}

export const posts = ref<Post[]>([
  {
    id: 1,
    title: 'Getting Started with Vue 3 Composition API',
    excerpt: 'The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic...',
    category: 'Frontend',
    date: '2025-06-01',
    views: 1240,
    likes: 89,
    comments: 23,
    tag: 'Vue',
    tagColor: 'green',
    content: '<p>The Composition API is one of the most exciting features in Vue 3. It provides a flexible way to organize component logic.</p><p>By separating concerns into composable functions, you can create more maintainable and reusable code. Key benefits include better TypeScript support, improved code organization, and easier testing.</p>',
  },
  {
    id: 2,
    title: 'Building Scalable APIs with Spring Boot',
    excerpt: 'Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration...',
    category: 'Backend',
    date: '2025-05-28',
    views: 980,
    likes: 64,
    comments: 15,
    tag: 'Java',
    tagColor: 'orange',
    content: '<p>Spring Boot makes it easy to create stand-alone, production-grade Spring applications with minimal configuration.</p><p>With embedded servers and auto-configuration, you can get a REST API running in minutes. The framework handles dependency injection, transaction management, and security out of the box.</p>',
  },
  {
    id: 3,
    title: 'TypeScript Best Practices in 2025',
    excerpt: 'TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most...',
    category: 'Language',
    date: '2025-05-22',
    views: 2100,
    likes: 142,
    comments: 37,
    tag: 'TypeScript',
    tagColor: 'blue',
    content: '<p>TypeScript has become the go-to choice for large-scale JavaScript applications. Here are the patterns that matter most in 2025.</p><ul><li>Always use strict mode</li><li>Prefer type inference over explicit annotations</li><li>Use utility types like Partial, Required, and Pick</li><li>Leverage template literal types for string manipulation</li></ul>',
  },
  {
    id: 4,
    title: 'Docker & Kubernetes: A Practical Guide',
    excerpt: 'Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and K8s together...',
    category: 'DevOps',
    date: '2025-05-15',
    views: 1560,
    likes: 98,
    comments: 29,
    tag: 'DevOps',
    tagColor: 'purple',
    content: '<p>Containerization has revolutionized how we deploy applications. Learn how to effectively use Docker and Kubernetes together.</p><p>Start with a solid Dockerfile, define resource limits in your K8s manifests, and use Helm charts to manage complex deployments across environments.</p>',
  },
])

let nextId = 5

export function addPost(post: Omit<Post, 'id' | 'views' | 'likes' | 'comments' | 'date'>) {
  posts.value.push({
    ...post,
    id: nextId++,
    views: 0,
    likes: 0,
    comments: 0,
    date: new Date().toISOString().slice(0, 10),
  })
}

export function updatePost(id: number, data: Partial<Omit<Post, 'id'>>) {
  const idx = posts.value.findIndex(p => p.id === id)
  if (idx !== -1) {
    posts.value[idx] = { ...posts.value[idx], ...data }
  }
}

export function deletePost(id: number) {
  posts.value = posts.value.filter(p => p.id !== id)
}

export function getPost(id: number): Post | undefined {
  return posts.value.find(p => p.id === id)
}
