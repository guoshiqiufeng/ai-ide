import axios from 'axios'
import type {
  R, ApiSettingDTO, ApiPresetDTO, PromptDTO,
  UserMessage, ChatSession, TaskDTO,
  ApiTestRequest, ApiTestResponse, ModelListResponse
} from '@/types'

const api = axios.create({
  baseURL: '/api',
  timeout: 60000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// API Settings
export const getActiveSetting = () =>
  api.get<R<ApiSettingDTO>>('/settings')

export const saveSetting = (data: ApiSettingDTO) =>
  api.post<R<ApiSettingDTO>>('/settings', data)

export const fetchModels = (data: Partial<ApiSettingDTO>) =>
  api.post<R<ModelListResponse>>('/settings/fetch-models', data)

export const testApi = (data: ApiTestRequest) =>
  api.post<R<ApiTestResponse>>('/settings/test', data)

// API Presets
export const listPresets = () =>
  api.get<R<ApiPresetDTO[]>>('/presets')

export const getPreset = (id: number) =>
  api.get<R<ApiPresetDTO>>(`/presets/${id}`)

export const createPreset = (data: ApiPresetDTO) =>
  api.post<R<ApiPresetDTO>>('/presets', data)

export const updatePreset = (id: number, data: ApiPresetDTO) =>
  api.put<R<ApiPresetDTO>>(`/presets/${id}`, data)

export const deletePreset = (id: number) =>
  api.delete<R<void>>(`/presets/${id}`)

export const activatePreset = (id: number) =>
  api.post<R<ApiPresetDTO>>(`/presets/${id}/activate`)

export const testPreset = (id: number) =>
  api.post<R<ApiTestResponse>>(`/presets/${id}/test`)

// Prompts
export const listPrompts = () =>
  api.get<R<PromptDTO[]>>('/prompts')

export const getPrompt = (id: number) =>
  api.get<R<PromptDTO>>(`/prompts/${id}`)

export const updatePrompt = (id: number, data: PromptDTO) =>
  api.put<R<PromptDTO>>(`/prompts/${id}`, data)

export const togglePrompt = (id: number) =>
  api.post<R<PromptDTO>>(`/prompts/${id}/toggle`)

// Messages
export const listMessages = () =>
  api.get<R<UserMessage[]>>('/messages')

export const listMessagesBySession = (sessionId: number) =>
  api.get<R<UserMessage[]>>(`/messages/session/${sessionId}`)

export const saveMessage = (data: UserMessage) =>
  api.post<R<UserMessage>>('/messages', data)

export const deleteMessage = (id: number) =>
  api.delete<R<void>>(`/messages/${id}`)

export const deleteAllMessages = () =>
  api.delete<R<void>>('/messages')

// Chat Sessions
export const listSessions = () =>
  api.get<R<ChatSession[]>>('/chat/sessions')

export const createSession = (title: string) =>
  api.post<R<ChatSession>>('/chat/sessions', { message: title })

export const deleteSession = (sessionId: number) =>
  api.delete<R<void>>(`/chat/sessions/${sessionId}`)

export const getTasksByMessage = (messageId: number) =>
  api.get<R<TaskDTO[]>>(`/chat/tasks/${messageId}`)

// Chat SSE
export const sendChatMessage = (sessionId: number | null, message: string): EventSource => {
  // We use fetch for SSE POST
  return null as unknown as EventSource
}

export const sendChat = async (
  sessionId: number | null,
  message: string,
  onEvent: (event: { event: string; data: any }) => void,
  onComplete: () => void,
  onError: (error: string) => void
) => {
  try {
    const response = await fetch('/api/chat/send', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ sessionId, message })
    })

    if (!response.ok) {
      onError(`HTTP error: ${response.status}`)
      return
    }

    const reader = response.body?.getReader()
    if (!reader) {
      onError('No response body')
      return
    }

    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (!trimmed) continue

        // Handle SSE format: data:...
        if (trimmed.startsWith('data:')) {
          const jsonStr = trimmed.slice(5).trim()
          if (!jsonStr) continue
          try {
            const parsed = JSON.parse(jsonStr)
            onEvent(parsed)
          } catch {
            // Try to parse as raw string event
            onEvent({ event: 'raw', data: jsonStr })
          }
        } else {
          // Try to parse as JSON directly
          try {
            const parsed = JSON.parse(trimmed)
            onEvent(parsed)
          } catch {
            // ignore non-JSON lines
          }
        }
      }
    }

    // Process remaining buffer
    if (buffer.trim()) {
      try {
        const parsed = JSON.parse(buffer.trim())
        onEvent(parsed)
      } catch {
        // ignore
      }
    }

    onComplete()
  } catch (error: any) {
    onError(error.message || 'Unknown error')
  }
}

export default api
