<template>
  <div class="chat-view">
    <!-- Sessions Sidebar -->
    <div class="sessions-panel">
      <div class="sessions-header">
        <span>Sessions</span>
        <el-button size="small" type="primary" @click="handleNewSession">
          <el-icon><Plus /></el-icon>
        </el-button>
      </div>
      <div class="sessions-list">
        <div
          v-for="session in sessions"
          :key="session.id"
          class="session-item"
          :class="{ active: currentSessionId === session.id }"
          @click="selectSession(session)"
        >
          <span class="session-title">{{ session.title }}</span>
          <el-button
            size="small"
            type="danger"
            link
            @click.stop="handleDeleteSession(session.id!)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <div v-if="sessions.length === 0" class="no-sessions">
          No sessions yet
        </div>
      </div>
    </div>

    <!-- Chat Area -->
    <div class="chat-panel">
      <div class="chat-messages" ref="messagesContainer">
        <div v-for="msg in chatMessages" :key="msg.id" class="message-wrapper">
          <div :class="['message', msg.role === 'USER' ? 'user-message' : 'assistant-message']">
            <div class="message-role">
              <el-icon v-if="msg.role === 'USER'"><User /></el-icon>
              <el-icon v-else><Monitor /></el-icon>
              {{ msg.role === 'USER' ? 'You' : 'AI Assistant' }}
            </div>
            <div class="message-content" v-html="renderMarkdown(msg.content)"></div>
          </div>
        </div>

        <!-- Streaming Status -->
        <div v-if="isProcessing" class="processing-area">
          <div v-if="currentStatus" class="status-message">
            <el-icon class="is-loading"><Loading /></el-icon>
            {{ currentStatus }}
          </div>

          <!-- Task Progress -->
          <div v-if="taskList.length > 0" class="task-progress">
            <div class="task-progress-title">Task Progress</div>
            <div v-for="task in taskList" :key="task.type" class="task-item">
              <div class="task-header">
                <el-icon v-if="task.status === 'COMPLETED'" style="color: #67c23a"><CircleCheckFilled /></el-icon>
                <el-icon v-else-if="task.status === 'IN_PROGRESS'" class="is-loading" style="color: #409eff"><Loading /></el-icon>
                <el-icon v-else-if="task.status === 'FAILED'" style="color: #f56c6c"><CircleCloseFilled /></el-icon>
                <el-icon v-else style="color: #909399"><Clock /></el-icon>
                <span class="task-type">{{ getTaskLabel(task.type) }}</span>
                <el-tag size="small" :type="getTaskTagType(task.status)">{{ task.status }}</el-tag>
              </div>
              <div v-if="task.result" class="task-result">
                <el-collapse>
                  <el-collapse-item :title="'View Result'">
                    <div v-html="renderMarkdown(task.result)"></div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>

          <!-- Streaming Response -->
          <div v-if="streamingContent" class="message assistant-message">
            <div class="message-role">
              <el-icon><Monitor /></el-icon>
              AI Assistant
            </div>
            <div class="message-content" v-html="renderMarkdown(streamingContent)"></div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input">
        <el-input
          v-model="inputMessage"
          type="textarea"
          :rows="3"
          placeholder="Type your message... (Enter to send, Shift+Enter for new line)"
          @keydown="handleKeydown"
          :disabled="isProcessing"
        />
        <el-button
          type="primary"
          @click="sendMessage"
          :loading="isProcessing"
          :disabled="!inputMessage.trim() || isProcessing"
          class="send-button"
        >
          <el-icon><Promotion /></el-icon>
          Send
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listSessions, createSession, deleteSession,
  listMessagesBySession, sendChat
} from '@/api'
import type { ChatSession, UserMessage, TaskDTO } from '@/types'

const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)
const chatMessages = ref<UserMessage[]>([])
const inputMessage = ref('')
const isProcessing = ref(false)
const currentStatus = ref('')
const streamingContent = ref('')
const taskList = ref<TaskDTO[]>([])
const messagesContainer = ref<HTMLElement>()

const loadSessions = async () => {
  try {
    const { data } = await listSessions()
    sessions.value = data.data || []
  } catch (error) {
    console.error('Failed to load sessions', error)
  }
}

onMounted(loadSessions)

const selectSession = async (session: ChatSession) => {
  currentSessionId.value = session.id || null
  try {
    const { data } = await listMessagesBySession(session.id!)
    chatMessages.value = data.data || []
    scrollToBottom()
  } catch (error) {
    console.error('Failed to load messages', error)
  }
}

const handleNewSession = () => {
  currentSessionId.value = null
  chatMessages.value = []
  taskList.value = []
}

const handleDeleteSession = async (id: number) => {
  try {
    await ElMessageBox.confirm('Delete this session?', 'Warning', { type: 'warning' })
    await deleteSession(id)
    ElMessage.success('Session deleted')
    if (currentSessionId.value === id) {
      currentSessionId.value = null
      chatMessages.value = []
    }
    await loadSessions()
  } catch {
    // cancelled
  }
}

const handleKeydown = (e: KeyboardEvent) => {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || isProcessing.value) return

  inputMessage.value = ''
  isProcessing.value = true
  currentStatus.value = ''
  streamingContent.value = ''
  taskList.value = []

  // Add user message to display immediately
  chatMessages.value.push({
    content: message,
    role: 'USER',
    sessionId: currentSessionId.value || undefined
  })
  scrollToBottom()

  await sendChat(
    currentSessionId.value,
    message,
    // onEvent
    (event) => {
      handleSSEEvent(event)
      scrollToBottom()
    },
    // onComplete
    () => {
      isProcessing.value = false
      currentStatus.value = ''

      // If we have streaming content, add it as assistant message
      if (streamingContent.value) {
        chatMessages.value.push({
          content: streamingContent.value,
          role: 'ASSISTANT',
          sessionId: currentSessionId.value || undefined
        })
        streamingContent.value = ''
      }

      loadSessions()
      scrollToBottom()
    },
    // onError
    (error) => {
      isProcessing.value = false
      currentStatus.value = ''
      ElMessage.error('Error: ' + error)
    }
  )
}

const handleSSEEvent = (event: { event: string; data: any }) => {
  switch (event.event) {
    case 'session': {
      const sessionData = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
      currentSessionId.value = sessionData.sessionId
      break
    }
    case 'status':
      currentStatus.value = typeof event.data === 'string' ? event.data : JSON.stringify(event.data)
      break
    case 'analysis':
      currentStatus.value = 'Analyzing tasks...'
      break
    case 'tasks': {
      const tasks = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
      taskList.value = tasks
      currentStatus.value = 'Processing tasks...'
      break
    }
    case 'task_start': {
      const startData = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
      const task = taskList.value.find(t => t.id === startData.taskId || t.type === startData.type)
      if (task) {
        task.status = 'IN_PROGRESS'
        currentStatus.value = `Processing ${getTaskLabel(startData.type)}...`
      }
      break
    }
    case 'task_complete': {
      const completeData = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
      const completedTask = taskList.value.find(t => t.id === completeData.taskId || t.type === completeData.type)
      if (completedTask) {
        completedTask.status = 'COMPLETED'
        completedTask.result = completeData.result
      }
      break
    }
    case 'task_error': {
      const errorData = typeof event.data === 'string' ? JSON.parse(event.data) : event.data
      const errorTask = taskList.value.find(t => t.id === errorData.taskId || t.type === errorData.type)
      if (errorTask) {
        errorTask.status = 'FAILED'
        errorTask.result = errorData.error
      }
      break
    }
    case 'response': {
      const response = typeof event.data === 'string' ? event.data : JSON.stringify(event.data)
      chatMessages.value.push({
        content: response,
        role: 'ASSISTANT',
        sessionId: currentSessionId.value || undefined
      })
      break
    }
    case 'complete':
      currentStatus.value = 'All tasks completed!'
      // Build summary from task results
      if (taskList.value.length > 0) {
        let summary = ''
        for (const task of taskList.value) {
          if (task.result) {
            summary += `## ${getTaskLabel(task.type)}\n\n${task.result}\n\n`
          }
        }
        if (summary) {
          chatMessages.value.push({
            content: summary,
            role: 'ASSISTANT',
            sessionId: currentSessionId.value || undefined
          })
        }
      }
      break
    case 'error':
      ElMessage.error(typeof event.data === 'string' ? event.data : JSON.stringify(event.data))
      break
    default:
      // Handle raw content for streaming
      if (typeof event.data === 'string') {
        streamingContent.value += event.data
      }
  }
}

const renderMarkdown = (text: string) => {
  if (!text) return ''
  // Simple markdown rendering - code blocks and basic formatting
  return text
    .replace(/```(\w*)\n([\s\S]*?)```/g, '<pre class="code-block"><code class="language-$1">$2</code></pre>')
    .replace(/`([^`]+)`/g, '<code class="inline-code">$1</code>')
    .replace(/## (.+)/g, '<h3>$1</h3>')
    .replace(/### (.+)/g, '<h4>$1</h4>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
}

const getTaskLabel = (type: string) => {
  const labels: Record<string, string> = {
    DATABASE: 'Database Task',
    API: 'API Task',
    FRONTEND: 'Frontend Task',
    BACKEND: 'Backend Task'
  }
  return labels[type] || type
}

const getTaskTagType = (status: string) => {
  const types: Record<string, string> = {
    PENDING: 'info',
    IN_PROGRESS: '',
    COMPLETED: 'success',
    FAILED: 'danger'
  }
  return (types[status] || 'info') as any
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-view {
  display: flex;
  height: calc(100vh - 40px);
  gap: 0;
  margin: -20px;
}

.sessions-panel {
  width: 240px;
  background: #fff;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.sessions-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  font-weight: bold;
}

.sessions-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.session-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f5f7fa;
}

.session-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.session-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.no-sessions {
  text-align: center;
  color: #909399;
  padding: 20px;
  font-size: 14px;
}

.chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message-wrapper {
  margin-bottom: 16px;
}

.message {
  max-width: 80%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.6;
}

.user-message {
  background: #409eff;
  color: #fff;
  margin-left: auto;
  border-bottom-right-radius: 4px;
}

.assistant-message {
  background: #fff;
  color: #333;
  margin-right: auto;
  border-bottom-left-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.message-role {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: bold;
  margin-bottom: 6px;
  opacity: 0.8;
}

.message-content {
  font-size: 14px;
  word-wrap: break-word;
}

.message-content :deep(.code-block) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 8px 0;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.message-content :deep(.inline-code) {
  background: rgba(0, 0, 0, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Fira Code', monospace;
  font-size: 13px;
}

.processing-area {
  margin-bottom: 16px;
}

.status-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #ecf5ff;
  border-radius: 8px;
  color: #409eff;
  margin-bottom: 12px;
}

.task-progress {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.task-progress-title {
  font-weight: bold;
  margin-bottom: 12px;
  font-size: 15px;
}

.task-item {
  margin-bottom: 10px;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.task-item:last-child {
  border-bottom: none;
}

.task-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-type {
  font-weight: 500;
  flex: 1;
}

.task-result {
  margin-top: 8px;
}

.chat-input {
  display: flex;
  gap: 10px;
  padding: 15px 20px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  align-items: flex-end;
}

.chat-input :deep(.el-textarea__inner) {
  resize: none;
}

.send-button {
  height: 40px;
  min-width: 100px;
}
</style>
