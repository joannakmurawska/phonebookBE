# Phonebook Application – Backend Documentation

## Overview

Spring Boot–based backend for a **digital phonebook application**.  
The backend integrates with the **Google Gemini API** to interpret natural language commands and manages **CRUD** operations on contacts stored in a **PostgreSQL** database.

**Tech Stack:**
- Java 17
- Spring Boot
- Maven
- PostgreSQL
- Google Gemini API
- OkHttp (HTTP client)
- Gson (JSON processing)
- Lombok

## Features

### 1. Natural Language Processing (NLP)
- Supports user commands in  **English**
- Automatically detects operations: **CREATE**, **READ**, **UPDATE**, **DELETE**
- Maintains context of existing contacts for improved interpretation accuracy

### 2. CRUD Operations
- **Create** – Add new contacts
- **Read** – Retrieve contacts by **ID** or **name**
- **Update** – Modify existing contact details
- **Delete** – Remove contacts from the database

### 3. API Endpoints
| Method | Endpoint | Description |
|:-------|:----------|:-------------|
| **POST** | `/api/gemini/prompt` | Send a direct prompt to Gemini |
| **POST** | `/api/gemini/user-crud` | Process natural language and execute corresponding CRUD actions |
| **GET** | `/api/users` | Retrieve all contacts |
| **GET** | `/api/users/{id}` | Retrieve a specific contact by ID |
| **POST** | `/api/users` | Create a new contact |
| **PUT** | `/api/users/{id}` | Update an existing contact |
| **DELETE** | `/api/users/{id}` | Delete a contact |

## Setup & Installation & Run Locally

### Requirements
- **Java 17+**
- **Maven 3.6+**

### Clone the Repository

https://github.com/joannakmurawska/phonebookBE.git
- git checkout to branch master (not main)

##### Create .env file in your root project with:

- DATABASE_URL= ??? 
- DB_USERNAME= ??? 
- DB_PASSWORD= ???
- GEMINI_API_KEY= ??? 
- gemini.api.url= ??

*** FOR DATA ASK JOANNA MURAWSKA ***

- ./mvnw clean install
- ./mvnw spring-boot:run
- And you can see users on: http://localhost:8080/api/users

You can check: 
```
   curl -X POST http://localhost:8080/api/contacts/command 
  -H "Content-Type: application/json" 
  -d '{"command": "Add John with phone number 123456789"}'
```

## Production url: 

```
https://phonebookbe-do1g.onrender.com/
```
