<template>
  <div class="messages-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>User Messages</span>
          <el-button type="danger" @click="handleDeleteAll" :disabled="messages.length === 0">
            <el-icon><Delete /></el-icon> Clear All
          </el-button>
        </div>
      </template>

      <el-table :data="messages" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="role" label="Role" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'USER' ? 'primary' : row.role === 'ASSISTANT' ? 'success' : 'info'">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="Content" show-overflow-tooltip />
        <el-table-column prop="sessionId" label="Session" width="100" />
        <el-table-column prop="createdAt" label="Time" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="danger" @click="handleDelete(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listMessages, deleteMessage, deleteAllMessages } from '@/api'
import type { UserMessage } from '@/types'

const messages = ref<UserMessage[]>([])

const loadMessages = async () => {
  try {
    const { data } = await listMessages()
    messages.value = data.data || []
  } catch (error) {
    console.error('Failed to load messages', error)
  }
}

onMounted(loadMessages)

const handleDelete = async (id: number | undefined) => {
  if (!id) return
  try {
    await ElMessageBox.confirm('Delete this message?', 'Warning', { type: 'warning' })
    await deleteMessage(id)
    ElMessage.success('Message deleted')
    await loadMessages()
  } catch {
    // cancelled
  }
}

const handleDeleteAll = async () => {
  try {
    await ElMessageBox.confirm('Delete all messages? This cannot be undone.', 'Warning', { type: 'warning' })
    await deleteAllMessages()
    ElMessage.success('All messages deleted')
    await loadMessages()
  } catch {
    // cancelled
  }
}

const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}
</script>

<style scoped>
.messages-view {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}
</style>
