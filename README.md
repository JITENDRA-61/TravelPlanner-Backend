# 🌍 Group Travel Itinerary Planner (TravelMate)

A full-stack collaborative travel planning application designed to organize group trips, discover destinations with OpenStreetMap POI search, schedule daily itineraries, manage travel companions, and track split expenses.

---

## 🚀 Live Architecture Overview

- **Frontend:** React 19, Vite, Vanilla CSS design system, responsive UI with hash-based routing & persistent session state.
- **Backend:** Java 25 / Spring Boot 4, Spring Data JPA, Hibernate ORM, OpenStreetMap Overpass API integration, RESTful API.
- **Database:** MySQL 8.4 / 9.x (`travel_planner` schema with auto-DDL update and migration SQL scripts).
- **Packaging:** Multi-stage Docker container with production JRE runtime, automated healthcheck, and container memory optimization.

---

## 🛠️ Technology Stack

| Layer | Technologies |
|---|---|
| **Frontend** | React 19, Vite, Vanilla CSS, Responsive Grid, SVG Graphics |
| **Backend** | Java 25 / 21, Spring Boot, Spring Data JPA, Jackson, RestTemplate |
| **External APIs** | OpenStreetMap (Overpass API for attractions, museums, parks, beaches) |
| **Database** | MySQL 8.4+ / MySQL Connector/J |
| **Containerization** | Docker, Docker Compose, Multi-stage builds |

---

## 📋 REST API Specification

### 1. Health Check
- `GET /api/health`
  - Returns backend and database connection status:
  - Response: `{"app":"Group Travel Itinerary Planner","database":"CONNECTED","databaseProduct":"MySQL","status":"UP"}`

### 2. Authentication
- `POST /api/auth/register`
  - Body: `{"name":"User Name","email":"user@example.com","password":"secretpassword"}`
  - Response: `"Registration successful"` or `"Email already registered"`
- `POST /api/auth/login`
  - Body: `{"email":"user@example.com","password":"secretpassword"}`
  - Response: `"Login successful"` or `"Invalid password"` / `"User not found"`

### 3. Trips
- `GET /api/trips` — List all trips.
- `POST /api/trips` — Create trip (`{tripName, startingLocation, destination, startDate, endDate, description}`).
- `GET /api/trips/{id}` — Get trip by ID.
- `DELETE /api/trips/{id}` — Delete trip and associated itinerary links.
- `GET /api/trips/{tripId}/destinations` — Get all planned destinations attached to a trip.
- `POST /api/trips/{tripId}/destinations` — Add destination to trip.
- `DELETE /api/trips/{tripId}/destinations/{destinationId}` — Remove destination from trip.

### 4. OpenStreetMap Discovery
- `GET /api/places/search?query={city}&category={all|attraction|museum|park|beach|restaurant|hotel}` — Discovers real-world points of interest via OpenStreetMap.
- `GET /api/places/nearby?latitude={lat}&longitude={lon}&category={cat}&radius={meters}` — Search nearby POIs within radius.

### 5. Destinations
- `GET /api/destinations` — List saved destinations.
- `POST /api/destinations` — Save destination to MySQL.
- `DELETE /api/destinations/{id}` — Delete destination.

### 6. Members
- `GET /api/members` — List group travel members.
- `POST /api/members` — Add group member (`{name, email, phone, role}`).
- `DELETE /api/members/{id}` — Delete member.

### 7. Itinerary & Expenses
- `GET /api/itinerary` / `POST /api/itinerary` / `DELETE /api/itinerary/{id}`
- `GET /api/expenses` / `POST /api/expenses` / `DELETE /api/expenses/{id}`

---

## ⚙️ Environment Variables Reference

| Variable | Description | Default |
|---|---|---|
| `PORT` | Backend HTTP listening port | `8080` |
| `SPRING_DATASOURCE_URL` | Full JDBC MySQL Connection URL | `jdbc:mysql://localhost:3306/travel_planner?...` |
| `SPRING_DATASOURCE_USERNAME` | Database username | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Database password | `root` |
| `DB_HOST` | MySQL hostname (if using separate components) | `localhost` |
| `DB_PORT` | MySQL port | `3306` |
| `DB_NAME` | MySQL database name | `travel_planner` |
| `VITE_API_URL` | Frontend URL to backend API | Relative `/` or `http://localhost:8080` |

---

## 🐳 Quick Local Start with Docker Compose

1. Make sure Docker is running on your machine.
2. Clone the repository:
   ```bash
   git clone https://github.com/JITENDRA-61/travelplanner.git
   cd travelplanner
   ```
3. Run with Docker Compose:
   ```bash
   docker-compose up --build
   ```
4. Open [http://localhost:8080](http://localhost:8080) in your browser.
   - MySQL database will automatically initialize using `travel_planner_init.sql`.

---

## 💻 Manual Local Development

### 1. Database Setup
Create database and import initial data:
```bash
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS travel_planner;"
mysql -u root -p travel_planner < travel_planner_init.sql
```

### 2. Backend
```bash
cd travelplanner
mvn clean package -DskipTests
java -jar target/travelplanner-0.0.1-SNAPSHOT.jar
```
Backend runs on `http://localhost:8080`.

### 3. Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend runs on `http://localhost:5173` and proxies requests to `http://localhost:8080`.

---

## ☁️ Cloud Deployment Guide

### Option 1: Render (Recommended for Spring Boot + Frontend)
1. Fork or push this repository to GitHub.
2. Sign in to [Render](https://render.com).
3. **Database:**
   - Create a free MySQL database on [Aiven](https://aiven.io), [TiDB Cloud](https://tidbcloud.com), or [Railway](https://railway.app).
   - Import the schema from `travel_planner_init.sql`.
4. **Web Service (Backend):**
   - Click **New** -> **Web Service**.
   - Select your repository.
   - Environment: `Docker` (or Java).
   - Dockerfile path: `./Dockerfile` (or `./travelplanner/Dockerfile`).
   - Add Environment Variables:
     - `SPRING_DATASOURCE_URL` = `jdbc:mysql://<HOST>:<PORT>/<DATABASE>?ssl-mode=REQUIRED`
     - `SPRING_DATASOURCE_USERNAME` = `<USERNAME>`
     - `SPRING_DATASOURCE_PASSWORD` = `<PASSWORD>`
     - `PORT` = `8080`
   - Render will build and deploy the container, providing an HTTPS URL: `https://<your-service>.onrender.com`.
   - The backend automatically serves the built React frontend at `/` with zero 404s!

### Option 2: Separate Frontend on Vercel
1. Import repository `frontend` directory into [Vercel](https://vercel.com).
2. Set Environment Variable:
   - `VITE_API_URL` = `https://<your-backend-service>.onrender.com`
3. Deploy!
