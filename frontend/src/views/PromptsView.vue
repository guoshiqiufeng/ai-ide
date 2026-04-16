<template>
  <div class="prompts-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>Prompt Management</span>
        </div>
      </template>

      <el-table :data="prompts" stripe style="width: 100%">
        <el-table-column prop="name" label="Name" width="200" />
        <el-table-column prop="type" label="Type" width="160">
          <template #default="{ row }">
            <el-tag>{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="content" label="Content" show-overflow-tooltip />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.isEnabled"
              @change="handleToggle(row.id)"
              active-text="On"
              inactive-text="Off"
            />
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="100" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEditDialog(row)">Edit</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Edit Dialog -->
    <el-dialog v-model="dialogVisible" title="Edit Prompt" width="700px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="Name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="Type">
          <el-input v-model="editForm.type" disabled />
        </el-form-item>
        <el-form-item label="Content">
          <el-input
            v-model="editForm.content"
            type="textarea"
            :rows="15"
            placeholder="Prompt content..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { listPrompts, updatePrompt, togglePrompt } from '@/api'
import type { PromptDTO } from '@/types'

const prompts = ref<PromptDTO[]>([])
const dialogVisible = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)

const editForm = ref<PromptDTO>({
  type: '',
  name: '',
  content: ''
})

const loadPrompts = async () => {
  try {
    const { data } = await listPrompts()
    prompts.value = data.data || []
  } catch (error) {
    console.error('Failed to load prompts', error)
  }
}

onMounted(loadPrompts)

const openEditDialog = (prompt: PromptDTO) => {
  editingId.value = prompt.id || null
  editForm.value = { ...prompt }
  dialogVisible.value = true
}

const handleSave = async () => {
  if (!editingId.value) return
  saving.value = true
  try {
    await updatePrompt(editingId.value, editForm.value)
    ElMessage.success('Prompt updated')
    dialogVisible.value = false
    await loadPrompts()
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to update prompt')
  } finally {
    saving.value = false
  }
}

const handleToggle = async (id: number | undefined) => {
  if (!id) return
  try {
    await togglePrompt(id)
    await loadPrompts()
    ElMessage.success('Prompt status updated')
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to toggle prompt')
  }
}
</script>

<style scoped>
.prompts-view {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
</style>
