export interface R<T> {
  code: number
  message: string
  data: T
}

export interface ApiSettingDTO {
  id?: number
  provider: string
  apiKey: string
  apiUrl: string
  modelName: string
  temperature: number
  maxTokens: number
  isActive?: boolean
}

export interface ApiPresetDTO {
  id?: number
  name: string
  provider: string
  apiKey: string
  apiUrl: string
  modelName: string
  temperature: number
  maxTokens: number
  isActive?: boolean
}

export interface PromptDTO {
  id?: number
  type: string
  name: string
  content: string
  isEnabled?: boolean
}

export interface UserMessage {
  id?: number
  content: string
  role: string
  sessionId?: number
  createdAt?: string
}

export interface ChatSession {
  id?: number
  title: string
  createdAt?: string
  updatedAt?: string
}

export interface TaskDTO {
  id?: number
  type: string
  status: string
  input: string
  result: string
  sortOrder: number
}

export interface ChatRequest {
  sessionId?: number
  message: string
}

export interface ApiTestRequest {
  provider: string
  apiKey: string
  apiUrl: string
  modelName: string
  temperature: number
  maxTokens: number
  testMessage?: string
}

export interface ApiTestResponse {
  success: boolean
  message: string
  response?: string
}

export interface ModelListResponse {
  success: boolean
  models: string[]
  message: string
}

export interface SSEEvent {
  event: string
  data: string | object
}
