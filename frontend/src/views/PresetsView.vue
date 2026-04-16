<template>
  <div class="presets-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>API Presets</span>
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> New Preset
          </el-button>
        </div>
      </template>

      <el-table :data="presets" stripe style="width: 100%">
        <el-table-column prop="name" label="Name" width="150" />
        <el-table-column prop="provider" label="Provider" width="160">
          <template #default="{ row }">
            <el-tag :type="row.provider === 'GOOGLE_GEMINI' ? 'success' : 'primary'">
              {{ row.provider === 'GOOGLE_GEMINI' ? 'Google Gemini' : 'OpenAI Compatible' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="modelName" label="Model" width="180" />
        <el-table-column prop="temperature" label="Temp" width="80" />
        <el-table-column prop="maxTokens" label="Max Tokens" width="100" />
        <el-table-column label="Status" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'info'">
              {{ row.isActive ? 'Active' : 'Inactive' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" fixed="right" width="280">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleActivate(row.id)" :disabled="row.isActive">
              Activate
            </el-button>
            <el-button size="small" type="warning" @click="handleTest(row.id)">Test</el-button>
            <el-button size="small" type="primary" @click="openEditDialog(row)">Edit</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create/Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? 'Edit Preset' : 'Create Preset'" width="600px">
      <el-form :model="presetForm" label-width="120px">
        <el-form-item label="Name">
          <el-input v-model="presetForm.name" placeholder="Preset name" />
        </el-form-item>
        <el-form-item label="Provider">
          <el-select v-model="presetForm.provider" style="width: 100%">
            <el-option label="OpenAI Compatible" value="OPENAI_COMPATIBLE" />
            <el-option label="Google Gemini" value="GOOGLE_GEMINI" />
          </el-select>
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="presetForm.apiKey" type="password" show-password />
        </el-form-item>
        <el-form-item label="API URL">
          <el-input v-model="presetForm.apiUrl" />
        </el-form-item>
        <el-form-item label="Model">
          <el-input v-model="presetForm.modelName" placeholder="Model name" />
        </el-form-item>
        <el-form-item label="Temperature">
          <el-slider v-model="presetForm.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="presetForm.maxTokens" :min="1" :max="128000" :step="100" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">Save</el-button>
      </template>
    </el-dialog>

    <!-- Test Result Dialog -->
    <el-dialog v-model="testDialogVisible" title="Test Result" width="500px">
      <el-alert
        v-if="testResult"
        :title="testResult.success ? 'Success' : 'Failed'"
        :type="testResult.success ? 'success' : 'error'"
        :description="testResult.response || testResult.message"
        show-icon
        :closable="false"
      />
      <div v-else style="text-align: center; padding: 20px">
        <el-icon class="is-loading" :size="24"><Loading /></el-icon>
        <p>Testing...</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listPresets, createPreset, updatePreset, deletePreset,
  activatePreset, testPreset
} from '@/api'
import type { ApiPresetDTO, ApiTestResponse } from '@/types'

const presets = ref<ApiPresetDTO[]>([])
const dialogVisible = ref(false)
const testDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const testResult = ref<ApiTestResponse | null>(null)

const presetForm = ref<ApiPresetDTO>({
  name: '',
  provider: 'OPENAI_COMPATIBLE',
  apiKey: '',
  apiUrl: 'https://api.openai.com',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096
})

const loadPresets = async () => {
  try {
    const { data } = await listPresets()
    presets.value = data.data || []
  } catch (error) {
    console.error('Failed to load presets', error)
  }
}

onMounted(loadPresets)

const openCreateDialog = () => {
  editingId.value = null
  presetForm.value = {
    name: '',
    provider: 'OPENAI_COMPATIBLE',
    apiKey: '',
    apiUrl: 'https://api.openai.com',
    modelName: '',
    temperature: 0.7,
    maxTokens: 4096
  }
  dialogVisible.value = true
}

const openEditDialog = (preset: ApiPresetDTO) => {
  editingId.value = preset.id || null
  presetForm.value = { ...preset }
  dialogVisible.value = true
}

const handleSave = async () => {
  saving.value = true
  try {
    if (editingId.value) {
      await updatePreset(editingId.value, presetForm.value)
      ElMessage.success('Preset updated')
    } else {
      await createPreset(presetForm.value)
      ElMessage.success('Preset created')
    }
    dialogVisible.value = false
    await loadPresets()
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to save preset')
  } finally {
    saving.value = false
  }
}

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('Are you sure to delete this preset?', 'Warning', { type: 'warning' })
    await deletePreset(id)
    ElMessage.success('Preset deleted')
    await loadPresets()
  } catch {
    // cancelled
  }
}

const handleActivate = async (id: number) => {
  try {
    await activatePreset(id)
    ElMessage.success('Preset activated and applied to settings')
    await loadPresets()
  } catch (error: any) {
    ElMessage.error(error.message || 'Failed to activate preset')
  }
}

const handleTest = async (id: number) => {
  testResult.value = null
  testDialogVisible.value = true
  try {
    const { data } = await testPreset(id)
    testResult.value = data.data
  } catch (error: any) {
    testResult.value = { success: false, message: error.message || 'Test failed' }
  }
}
</script>

<style scoped>
.presets-view {
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
