# Team Task Manager - Backend

Spring Boot backend for the Team Task Manager application.

## API Overview

Base path: `/api`

### Auth (`/api/auth`)

- `POST /register` - Register a new user.
- `POST /login` - Log in and receive access/refresh tokens.
- `POST /refresh` - Refresh access token (body: `refreshToken`).
- `POST /logout` - Revoke refresh token (body: `refreshToken`).

### Users (`/api/users`)

- `GET /me` - Get current user profile.
- `PUT /me` - Update current user (`name`, `password`).
- `GET /` - List users (admin only, pageable).
- `PUT /{userId}/role` - Update user role (admin only, body: `role`).
- `DELETE /{userId}` - Delete user (admin only).

### Projects (`/api/projects`)

- `POST /` - Create project (admin only).
- `GET /` - List projects for current user (pageable).
- `GET /{projectId}` - Get project by id.
- `PUT /{projectId}` - Update project (admin only, body: `name`, `description`, `status`).
- `POST /{projectId}/members` - Add member (admin only, body: `userId`, `projectRole`).
- `DELETE /{projectId}/members/{userId}` - Remove member (admin only).
- `DELETE /{projectId}` - Delete project (admin only).

### Tasks (`/api/projects/{projectId}/tasks`)

- `POST /` - Create task.
- `GET /` - List tasks with filters:
  - Query params: `status`, `assigneeId`, `priority`, `overdue`, plus pagination.
- `GET /{taskId}` - Get task by id.
- `PUT /{taskId}` - Update task.
- `PATCH /{taskId}/status` - Update task status.
- `POST /{taskId}/comments` - Add comment (body: `text`).
- `DELETE /{taskId}/comments/{commentId}` - Delete comment.
- `DELETE /{taskId}` - Delete task (admin only).

### Join Requests (`/api/projects/{projectId}/join-requests`)

- `POST /` - Request to join a project (authenticated users).
- `GET /` - List join requests (project owners or system admins).
- `PATCH /{requestId}` - Approve or reject a join request (project owners or admins, body: `status`).

### Dashboard (`/api/dashboard`)

- `GET /my` - Current user dashboard.
- `GET /admin` - Admin dashboard (admin only).
- `GET /projects/{projectId}/stats` - Project stats.

## Configuration

The app uses PostgreSQL and JWT. Environment variables are read from `application.yaml`:

- `DB_URL` (default: `jdbc:postgresql://localhost:5432/team_task_manager`)
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SPRING_PROFILES_ACTIVE` (default: `dev`)

## Docker

The `Dockerfile` uses a multi-stage build to produce a small runtime image.

Build the image:

```bash
docker build -t team-task-manager-backend .
```

Run the container:

```bash
docker run --rm -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/team_task_manager \
  -e DB_USERNAME=YOUR_DB_USER \
  -e DB_PASSWORD=YOUR_DB_PASS \
  -e JWT_SECRET=YOUR_JWT_SECRET \
  team-task-manager-backend
```

## How to Run (Local)

```bash
./mvnw spring-boot:run
```

If you prefer building the jar first:

```bash
./mvnw -DskipTests package
java -jar target/*.jar
```
