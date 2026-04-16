<template>
  <div class="settings-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>API Settings</span>
        </div>
      </template>

      <el-form :model="form" label-width="140px" label-position="right">
        <el-form-item label="API Provider">
          <el-select v-model="form.provider" placeholder="Select Provider" style="width: 100%">
            <el-option label="OpenAI Compatible" value="OPENAI_COMPATIBLE" />
            <el-option label="Google Gemini" value="GOOGLE_GEMINI" />
          </el-select>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="Enter API Key" />
        </el-form-item>

        <el-form-item label="API URL">
          <el-input v-model="form.apiUrl" placeholder="Enter API Base URL" />
        </el-form-item>

        <el-form-item label="Model">
          <div style="display: flex; gap: 10px; width: 100%">
            <el-select
              v-model="form.modelName"
              placeholder="Select Model"
              filterable
              allow-create
              style="flex: 1"
            >
              <el-option v-for="m in models" :key="m" :label="m" :value="m" />
            </el-select>
            <el-button type="primary" @click="handleFetchModels" :loading="fetchingModels">
              Fetch Models
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="Temperature">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" show-input />
        </el-form-item>

        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="1" :max="128000" :step="100" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">Save Settings</el-button>
          <el-button type="success" @click="showTestDialog = true">Test API</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Test Dialog -->
    <el-dialog v-model="showTestDialog" title="API Test" width="600px">
      <el-input
        v-model="testMessage"
        type="textarea"
        :rows="3"
        placeholder="Enter test message..."
      />
      <div v-if="testResult" class="test-result" style="margin-top: 15px">
        <el-alert
          :title="testResult.success ? 'Success' : 'Failed'"
          :type="testResult.success ? 'success' : 'error'"
          :description="testResult.response || testResult.message"
          show-icon
          :closable="false"
        />
      </div>
      <template #footer>
        <el-button @click="showTestDialog = false">Close</el-button>
        <el-button type="primary" @click="handleTestApi" :loading="testing">Send Test</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getActiveSetting, saveSetting, fetchModels, testApi } from '@/api'
import type { ApiSettingDTO, ApiTestResponse } from '@/types'

const form = ref<ApiSettingDTO>({
  provider: 'OPENAI_COMPATIBLE',
  apiKey: '',
  apiUrl: 'https://api.openai.com',
  modelName: '',
  temperature: 0.7,
  maxTokens: 4096
})

const models = ref<string[]>([])
const fetchingModels = ref(false)
const saving = ref(false)
const testing = ref(false)
const showTestDialog = ref(false)
const testMessage = ref('Hello, this is a test message. Please respond briefly.')
const testResult = ref<ApiTestResponse | null>(null)

onMounted(async () => {
  try {
    const { data } = await getActiveSetting()
    if (data.data) {
      form.value = { ...form.value, ...data.data }
    }
  } catch (error) {
    console.error('Failed to load settings', error)
  }
})

const handleFetchModels = async () => {
  if (!form.value.apiKey || !form.value.apiUrl) {
    ElMessage.warning('Please enter API Key and URL first')
    return
  }
  fetchingModels.value = true
  try {
    const { data } = await fetchModels({
      provider: form.value.provider,
      apiKey: form.value.apiKey,
      apiUrl: form.value.apiUrl
    })
    if (data.data?.success) {
      models.value = data.data.models
      ElMessage.success(`Fetched ${models.value.length} models`)
    } else {
      ElMessage.error(data.data?.message || 'Failed to fetch models')
    }
  } catch (error: any) {
    ElMessage.error('Failed to fetch models: ' + (error.message || 'Unknown error'))
  } finally {
    fetchingModels.value = false
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    await saveSetting(form.value)
    ElMessage.success('Settings saved successfully')
  } catch (error: any) {
    ElMessage.error('Failed to save settings: ' + (error.message || 'Unknown error'))
  } finally {
    saving.value = false
  }
}

const handleTestApi = async () => {
  testing.value = true
  testResult.value = null
  try {
    const { data } = await testApi({
      provider: form.value.provider,
      apiKey: form.value.apiKey,
      apiUrl: form.value.apiUrl,
      modelName: form.value.modelName,
      temperature: form.value.temperature,
      maxTokens: form.value.maxTokens,
      testMessage: testMessage.value
    })
    testResult.value = data.data
  } catch (error: any) {
    testResult.value = { success: false, message: error.message || 'Unknown error' }
  } finally {
    testing.value = false
  }
}
</script>

<style scoped>
.settings-view {
  max-width: 800px;
  margin: 0 auto;
}
.card-header {
  font-size: 18px;
  font-weight: bold;
}
</style>
