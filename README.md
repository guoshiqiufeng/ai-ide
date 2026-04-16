# AI IDE

An AI-powered coding assistant that helps generate code through an interactive chat interface. The system intelligently splits coding tasks into database, API, frontend, and backend subtasks and processes them sequentially and in parallel using AI.

## Tech Stack

- **Backend**: Spring Boot 3.4 + Spring AI + PostgreSQL
- **Frontend**: Vue 3 + TypeScript + Element Plus

## Features

### API Settings
- Support for multiple API providers: OpenAI Compatible, Google Gemini
- Configure API key, base URL, model, temperature, and max tokens
- Fetch available models from API provider
- Test API connectivity

### API Presets
- Create, edit, delete API configuration presets
- Activate presets to quickly switch between configurations
- Test presets before activating

### Prompt Management
- View and edit system prompts for each task type
- Enable/disable prompts
- Pre-initialized prompts for: Task Analysis, Database, API, Frontend, Backend

### User Message Management
- View message history across all sessions
- Delete individual messages or clear all

### Interactive Chat Window
- Real-time chat with AI assistant
- Automatic task analysis and splitting:
  1. **Database Task** - Processed first (DDL, schema design)
  2. **API Task** - Processed after database task (endpoint design)
  3. **Frontend Task** - Processed in parallel (Vue components, UI)
  4. **Backend Task** - Processed in parallel (Spring Boot services)
- Visual task progress tracking
- Session management

## Prerequisites

- Java 17+
- Node.js 18+
- PostgreSQL 18
- Maven 3.9+

## Getting Started

### Database Setup

```sql
CREATE DATABASE ai_ide;
```

### Backend

```bash
cd backend
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The frontend will start on `http://localhost:5173`.

## Configuration

Edit `backend/src/main/resources/application.yml` to configure:
- Database connection (PostgreSQL)
- Default AI API settings

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/settings` | Get active API setting |
| POST | `/api/settings` | Save API setting |
| POST | `/api/settings/fetch-models` | Fetch available models |
| POST | `/api/settings/test` | Test API connection |
| GET | `/api/presets` | List all presets |
| POST | `/api/presets` | Create preset |
| PUT | `/api/presets/{id}` | Update preset |
| DELETE | `/api/presets/{id}` | Delete preset |
| POST | `/api/presets/{id}/activate` | Activate preset |
| POST | `/api/presets/{id}/test` | Test preset |
| GET | `/api/prompts` | List all prompts |
| PUT | `/api/prompts/{id}` | Update prompt |
| POST | `/api/prompts/{id}/toggle` | Toggle prompt enabled/disabled |
| GET | `/api/messages` | List all messages |
| DELETE | `/api/messages/{id}` | Delete message |
| DELETE | `/api/messages` | Delete all messages |
| GET | `/api/chat/sessions` | List chat sessions |
| POST | `/api/chat/sessions` | Create session |
| DELETE | `/api/chat/sessions/{id}` | Delete session |
| POST | `/api/chat/send` | Send chat message (SSE stream) |
| GET | `/api/chat/tasks/{messageId}` | Get tasks for a message |
