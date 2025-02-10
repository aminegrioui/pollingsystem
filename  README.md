# 🗂️ Project Name: Polling System API

## 📦 Project Setup Guide

## 🚀 Clone the Project

```bash
git clone https://github.com/aminegrioui/pollingsystem.git 
cd <project-directory>
```

## 🐳 Run PostgreSQL Container

Start the PostgreSQL container using Docker:

```bash
docker run -d \
  --name postgres-container-db \
  -e POSTGRES_DB=polling_db \
  -e POSTGRES_USER=username \
  -e POSTGRES_PASSWORD=password \
  -p 5437:5432 \
  postgres:15
```

- **Container Name:** `postgres-container-db`
- **Database:** `polling_db`
- **Username:** `username`
- **Password:** `password`
- **Port:** `5437` (local) → `5432` (container)

## ⚙️ Configure Application

Ensure your `application.properties` matches the PostgreSQL setup.

## 🏃 Run the Application

Start the Spring Boot application:

## 🔗 Test APIs

Use tools like **Postman** or **cURL** to test the APIs:

## 📥 Import Insomnia Collection

Import into your Insomnia workspace:
    - Click on **Import/Export** → **Import Data** → **From File**.
    - Select: `pollingSystem.json` file.

This collection contains pre-configured API requests for quick testing.

## 🗑️ Stop and Remove PostgreSQL Container

When done, stop and remove the container:

```bash
docker stop postgres-container-db
docker rm postgres-container-db
```

---

✅ **Project Ready to Go!**

