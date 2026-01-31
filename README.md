# TrackSpace Backend 🚀

[![GitHub](https://img.shields.io/badge/GitHub-TuanHoang297-blue?logo=github)](https://github.com/TuanHoang297/trackspace-backend)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen?logo=springboot)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)](https://www.mysql.com/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## 📋 Tổng quan

TrackSpace Backend là hệ thống API monolithic được xây dựng bằng **Spring Boot** để hỗ trợ quản lý dự án, theo dõi yêu cầu và giám sát tiến độ cho ngành Kỹ Thuật Phần Mềm.

## 🏗️ Kiến trúc

- **Architecture**: Monolithic
- **Package Structure**: Flat Package Structure
- **Pattern**: Controller - Service - Repository
- **Security**: Spring Security + JWT
- **Database**: MySQL 8.0+
- **Java Version**: 17

## 📦 Tech Stack

- **Spring Boot 3.2.0**
- **Spring Data JPA**
- **Spring Security + JWT**
- **MySQL**
- **Swagger/OpenAPI**
- **Apache POI** (Excel Import)
- **iText7** (PDF Export)
- **WebFlux** (External API Calls)

## 📁 Cấu trúc thư mục

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/trackspace/
│   │   │   ├── TrackSpaceApplication.java
│   │   │   ├── common/              # Shared utilities
│   │   │   ├── auth/                # Authentication
│   │   │   ├── user/                # User Management
│   │   │   ├── classroom/           # Class & Group
│   │   │   ├── project/             # Project Management
│   │   │   ├── jira/                # Jira Integration
│   │   │   ├── github/              # GitHub Integration
│   │   │   ├── srs/                 # AI SRS Generation
│   │   │   ├── analytics/           # Contribution Tracking
│   │   │   └── notification/        # Notification System
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       └── application-prod.properties
│   └── test/
├── pom.xml
└── README.md
```

## 🚀 Cách chạy

### 1. Cài đặt MySQL

```bash
# Tạo database
mysql -u root -p
CREATE DATABASE trackspace;
```

### 2. Cấu hình application.properties

Sửa file `src/main/resources/application.properties`:

```properties
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 3. Build và Run

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Hoặc chạy với profile dev
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 4. Truy cập Swagger UI

```
http://localhost:8080/swagger-ui.html
```

## 📡 API Endpoints

### Authentication
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Register

### Users
- `GET /api/users` - Get all users
- `POST /api/users` - Create user
- `PUT /api/users/{id}` - Update user
- `DELETE /api/users/{id}` - Delete user

### Classes & Groups
- `GET /api/classes` - Get all classes
- `POST /api/classes` - Create class
- `POST /api/groups` - Create group
- `PUT /api/groups/{id}/leader` - Assign team leader

### Jira Integration
- `POST /api/jira/connection` - Connect to Jira
- `POST /api/jira/sync` - Sync Jira data
- `GET /api/sprints` - Get sprints
- `POST /api/issues` - Create issue

### GitHub Integration
- `POST /api/github/connection` - Connect to GitHub
- `POST /api/github/sync` - Sync GitHub data
- `GET /api/github/commits` - Get commits

### SRS Generation
- `POST /api/srs/generate` - Generate SRS
- `GET /api/srs/{id}/export` - Export SRS

### Contributions
- `GET /api/contributions/project/{projectId}` - Get project contributions
- `GET /api/contributions/dashboard/{projectId}` - Get contribution dashboard

## 🔐 Security

API sử dụng JWT authentication. Để truy cập protected endpoints:

1. Login để lấy JWT token
2. Thêm header: `Authorization: Bearer <token>`

## 🗄️ Database Schema

Database schema được định nghĩa trong file `database_schema.sql`. Chạy file này để tạo tables.

## 📝 Development

### Code Style
- Sử dụng Lombok để giảm boilerplate code
- Follow Java naming conventions
- Write clean code và comment khi cần

### Git Workflow
```bash
# Tạo branch cho feature mới
git checkout -b feature/feature-name

# Commit changes
git add .
git commit -m "Add feature-name"

# Push to remote
git push origin feature/feature-name

# Create Pull Request
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=TestClassName
```

## 📦 Build for Production

```bash
# Build JAR file
mvn clean package

# Run JAR file
java -jar target/trackspace-backend-1.0.0.jar --spring.profiles.active=prod
```

## 🌐 Deploy to Azure

1. Build JAR file
2. Upload to Azure App Service
3. Configure environment variables
4. Start application

## 🤝 Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Create Pull Request

## 📄 License

Copyright © 2026 TrackSpace Team

## 👥 Authors

- **TrackSpace Team**

---

**Note**: Đây là project đồ án môn học. Mọi thông tin liên hệ vui lòng qua email của team.
