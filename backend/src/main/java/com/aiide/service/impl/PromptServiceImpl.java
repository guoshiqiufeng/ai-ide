package com.aiide.service.impl;

import com.aiide.dto.*;
import com.aiide.entity.Prompt;
import com.aiide.repository.PromptRepository;
import com.aiide.service.PromptService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {

    private final PromptRepository promptRepository;

    @PostConstruct
    public void init() {
        initializeDefaultPrompts();
    }

    @Override
    public R<List<PromptDTO>> listPrompts() {
        List<PromptDTO> prompts = promptRepository.findAllByOrderByIdAsc()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return R.ok(prompts);
    }

    @Override
    public R<PromptDTO> getPrompt(Long id) {
        return promptRepository.findById(id)
                .map(this::toDTO)
                .map(R::ok)
                .orElse(R.error("Prompt not found"));
    }

    @Override
    @Transactional
    public R<PromptDTO> updatePrompt(Long id, PromptDTO dto) {
        return promptRepository.findById(id)
                .map(prompt -> {
                    prompt.setName(dto.getName());
                    prompt.setContent(dto.getContent());
                    return R.ok(toDTO(promptRepository.save(prompt)));
                })
                .orElse(R.error("Prompt not found"));
    }

    @Override
    @Transactional
    public R<PromptDTO> togglePrompt(Long id) {
        return promptRepository.findById(id)
                .map(prompt -> {
                    prompt.setIsEnabled(!prompt.getIsEnabled());
                    return R.ok(toDTO(promptRepository.save(prompt)));
                })
                .orElse(R.error("Prompt not found"));
    }

    @Override
    @Transactional
    public void initializeDefaultPrompts() {
        if (promptRepository.count() > 0) {
            return;
        }

        log.info("Initializing default prompts...");

        promptRepository.save(Prompt.builder()
                .type("TASK_ANALYSIS")
                .name("Task Analysis Prompt")
                .content("""
                        You are an intelligent task analyzer for a coding assistant. Analyze the user's message and determine if it requires coding work.

                        If the user's message requires coding work, respond with a JSON object in the following format:
                        {
                          "needsCoding": true,
                          "analysis": "Brief description of the overall task",
                          "tasks": {
                            "database": "Description of database changes needed (table creation, modification, etc.), or null if not needed",
                            "api": "Description of API endpoints needed (REST APIs, request/response formats, etc.), or null if not needed",
                            "frontend": "Description of frontend changes needed (pages, components, interactions, etc.), or null if not needed",
                            "backend": "Description of backend logic needed (business logic, services, etc.), or null if not needed"
                          }
                        }

                        If the user's message does NOT require coding work (e.g., it's a general question, greeting, etc.), respond with:
                        {
                          "needsCoding": false,
                          "response": "Your helpful response to the user"
                        }

                        Important rules:
                        1. Only return valid JSON, no markdown or extra text
                        2. Be thorough in analyzing what tasks are needed
                        3. Set a task to null if it's not needed for this request
                        4. Consider dependencies between tasks (database -> API -> frontend/backend)
                        """)
                .isEnabled(true)
                .build());

        promptRepository.save(Prompt.builder()
                .type("DATABASE_TASK")
                .name("Database Task Prompt")
                .content("""
                        You are a database design expert. Based on the following requirements, generate the complete database design including:

                        1. SQL DDL statements (CREATE TABLE) for PostgreSQL
                        2. Include proper data types, constraints, indexes, and foreign keys
                        3. Add comments explaining each table and column
                        4. Consider data integrity and normalization
                        5. Include any necessary seed data (INSERT statements)

                        Requirements:
                        {task_description}

                        Please respond with:
                        1. Complete SQL DDL statements
                        2. Entity relationship description
                        3. Any important notes about the design decisions

                        Format your response as follows:
                        ## Database Design

                        ### Tables
                        ```sql
                        -- Your DDL here
                        ```

                        ### Entity Relationships
                        - Describe relationships

                        ### Notes
                        - Design decisions and notes
                        """)
                .isEnabled(true)
                .build());

        promptRepository.save(Prompt.builder()
                .type("API_TASK")
                .name("API Task Prompt")
                .content("""
                        You are an API design expert. Based on the following requirements and the database design provided, generate a complete API specification.

                        Database Design:
                        {database_result}

                        Requirements:
                        {task_description}

                        Please design RESTful API endpoints including:
                        1. Endpoint URL and HTTP method
                        2. Request parameters and body format
                        3. Response format and status codes
                        4. Authentication requirements if any
                        5. Error handling

                        Format your response as follows:
                        ## API Design

                        ### Endpoints
                        For each endpoint:
                        - **Method** `URL`
                        - Description
                        - Request Body / Parameters
                        - Response Format
                        - Status Codes

                        ### Data Transfer Objects (DTOs)
                        ```json
                        // DTO definitions
                        ```

                        ### Notes
                        - API design decisions
                        """)
                .isEnabled(true)
                .build());

        promptRepository.save(Prompt.builder()
                .type("FRONTEND_TASK")
                .name("Frontend Task Prompt")
                .content("""
                        You are a frontend development expert specializing in Vue 3 + TypeScript. Based on the requirements, database design, and API specification provided, generate the complete frontend code.

                        Database Design:
                        {database_result}

                        API Specification:
                        {api_result}

                        Requirements:
                        {task_description}

                        Please generate:
                        1. Vue 3 components using Composition API with <script setup lang="ts">
                        2. TypeScript interfaces for data types
                        3. API service functions using axios
                        4. Router configuration if needed
                        5. State management if needed (Pinia)
                        6. Responsive and clean UI design

                        Format your response with complete code files:
                        ## Frontend Implementation

                        ### Components
                        ```vue
                        <!-- Component code -->
                        ```

                        ### Types
                        ```typescript
                        // TypeScript interfaces
                        ```

                        ### API Services
                        ```typescript
                        // API call functions
                        ```

                        ### Notes
                        - Implementation decisions
                        """)
                .isEnabled(true)
                .build());

        promptRepository.save(Prompt.builder()
                .type("BACKEND_TASK")
                .name("Backend Task Prompt")
                .content("""
                        You are a backend development expert specializing in Spring Boot 3 + Java. Based on the requirements, database design, and API specification provided, generate the complete backend code.

                        Database Design:
                        {database_result}

                        API Specification:
                        {api_result}

                        Requirements:
                        {task_description}

                        Please generate:
                        1. Entity classes with JPA annotations
                        2. Repository interfaces
                        3. Service interfaces and implementations
                        4. REST Controller classes
                        5. DTO classes
                        6. Configuration classes if needed
                        7. Exception handling

                        Format your response with complete code files:
                        ## Backend Implementation

                        ### Entities
                        ```java
                        // Entity classes
                        ```

                        ### Repositories
                        ```java
                        // Repository interfaces
                        ```

                        ### Services
                        ```java
                        // Service implementations
                        ```

                        ### Controllers
                        ```java
                        // REST Controllers
                        ```

                        ### Notes
                        - Implementation decisions
                        """)
                .isEnabled(true)
                .build());

        log.info("Default prompts initialized successfully");
    }

    private PromptDTO toDTO(Prompt entity) {
        return PromptDTO.builder()
                .id(entity.getId())
                .type(entity.getType())
                .name(entity.getName())
                .content(entity.getContent())
                .isEnabled(entity.getIsEnabled())
                .build();
    }
}
