# TrackSpace Backend - Monolithic Architecture (Flat Package Structure)

## 📦 Tổng quan Kiến trúc

Backend sử dụng **Monolithic Architecture** với **Flat Package Structure** - đơn giản, gọn gàng, phù hợp với team nhỏ.

## 📁 Cấu trúc Thư mục (Flat Structure)

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/trackspace/
│   │   │   ├── TrackSpaceApplication.java
│   │   │   │
│   │   │   ├── common/                         # Shared utilities
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   ├── JacksonConfig.java
│   │   │   │   ├── JwtFilter.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   ├── BadRequestException.java
│   │   │   │   ├── UnauthorizedException.java
│   │   │   │   ├── ApiException.java
│   │   │   │   ├── ApiResponse.java
│   │   │   │   ├── DateUtils.java
│   │   │   │   ├── StringUtils.java
│   │   │   │   ├── EncryptionUtils.java
│   │   │   │   ├── ValidationUtils.java
│   │   │   │   └── Constants.java
│   │   │   │
│   │   │   ├── auth/                           # MODULE: Authentication
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── EmailService.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   └── AuthResponse.java
│   │   │   │
│   │   │   ├── user/                           # MODULE: User Management
│   │   │   │   ├── UserController.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── User.java
│   │   │   │   ├── CreateUserRequest.java
│   │   │   │   ├── UpdateUserRequest.java
│   │   │   │   └── UserResponse.java
│   │   │   │
│   │   │   ├── classroom/                      # MODULE: Class & Group
│   │   │   │   ├── ClassController.java
│   │   │   │   ├── GroupController.java
│   │   │   │   ├── ClassService.java
│   │   │   │   ├── GroupService.java
│   │   │   │   ├── ClassRepository.java
│   │   │   │   ├── GroupRepository.java
│   │   │   │   ├── ClassStudentRepository.java
│   │   │   │   ├── GroupMemberRepository.java
│   │   │   │   ├── Class.java
│   │   │   │   ├── Group.java
│   │   │   │   ├── ClassStudent.java
│   │   │   │   ├── GroupMember.java
│   │   │   │   ├── CreateClassRequest.java
│   │   │   │   ├── CreateGroupRequest.java
│   │   │   │   ├── AssignLeaderRequest.java
│   │   │   │   ├── ClassResponse.java
│   │   │   │   └── GroupResponse.java
│   │   │   │
│   │   │   ├── project/                        # MODULE: Project Management
│   │   │   │   ├── ProjectController.java
│   │   │   │   ├── ProjectService.java
│   │   │   │   ├── ProjectRepository.java
│   │   │   │   ├── ProjectInfoRepository.java
│   │   │   │   ├── Project.java
│   │   │   │   ├── ProjectInfo.java
│   │   │   │   ├── ProjectInfoRequest.java
│   │   │   │   ├── ProjectResponse.java
│   │   │   │   └── ProjectInfoResponse.java
│   │   │   │
│   │   │   ├── jira/                           # MODULE: Jira Integration
│   │   │   │   ├── JiraConnectionController.java
│   │   │   │   ├── SprintController.java
│   │   │   │   ├── IssueController.java
│   │   │   │   ├── JiraConnectionService.java
│   │   │   │   ├── SprintService.java
│   │   │   │   ├── IssueService.java
│   │   │   │   ├── JiraSyncService.java
│   │   │   │   ├── JiraApiClient.java
│   │   │   │   ├── JiraSyncScheduler.java
│   │   │   │   ├── JiraConfig.java
│   │   │   │   ├── JiraConnectionRepository.java
│   │   │   │   ├── JiraSprintRepository.java
│   │   │   │   ├── JiraIssueRepository.java
│   │   │   │   ├── JiraConnection.java
│   │   │   │   ├── JiraSprint.java
│   │   │   │   ├── JiraIssue.java
│   │   │   │   ├── JiraConnectionRequest.java
│   │   │   │   ├── CreateSprintRequest.java
│   │   │   │   ├── CreateIssueRequest.java
│   │   │   │   ├── UpdateIssueStatusRequest.java
│   │   │   │   ├── SprintResponse.java
│   │   │   │   └── IssueResponse.java
│   │   │   │
│   │   │   ├── github/                         # MODULE: GitHub Integration
│   │   │   │   ├── GitHubConnectionController.java
│   │   │   │   ├── RepositoryController.java
│   │   │   │   ├── CommitController.java
│   │   │   │   ├── GitHubConnectionService.java
│   │   │   │   ├── RepositoryService.java
│   │   │   │   ├── CommitService.java
│   │   │   │   ├── GitHubSyncService.java
│   │   │   │   ├── GitHubApiClient.java
│   │   │   │   ├── GitHubSyncScheduler.java
│   │   │   │   ├── GitHubConfig.java
│   │   │   │   ├── GitHubConnectionRepository.java
│   │   │   │   ├── GitHubCommitRepository.java
│   │   │   │   ├── GitHubConnection.java
│   │   │   │   ├── GitHubCommit.java
│   │   │   │   ├── GitHubConnectionRequest.java
│   │   │   │   ├── CommitFilterRequest.java
│   │   │   │   ├── RepositoryResponse.java
│   │   │   │   ├── CommitResponse.java
│   │   │   │   └── ContributorResponse.java
│   │   │   │
│   │   │   ├── srs/                            # MODULE: AI SRS Generation
│   │   │   │   ├── SRSController.java
│   │   │   │   ├── SRSService.java
│   │   │   │   ├── SRSGeneratorService.java
│   │   │   │   ├── SRSExportService.java
│   │   │   │   ├── OpenAIClient.java
│   │   │   │   ├── SRSTemplate.java
│   │   │   │   ├── PromptBuilder.java
│   │   │   │   ├── AIConfig.java
│   │   │   │   ├── SRSDocumentRepository.java
│   │   │   │   ├── SRSDocument.java
│   │   │   │   ├── GenerateSRSRequest.java
│   │   │   │   ├── UpdateSRSRequest.java
│   │   │   │   ├── ReviewSRSRequest.java
│   │   │   │   ├── SRSResponse.java
│   │   │   │   └── SRSExportResponse.java
│   │   │   │
│   │   │   ├── analytics/                      # MODULE: Analytics
│   │   │   │   ├── ContributionController.java
│   │   │   │   ├── ContributionService.java
│   │   │   │   ├── MetricsCalculator.java
│   │   │   │   ├── IssueDetectionService.java
│   │   │   │   ├── ContributionCalculator.java
│   │   │   │   ├── ContributionMetricsRepository.java
│   │   │   │   ├── ContributionMetrics.java
│   │   │   │   ├── ContributionFilterRequest.java
│   │   │   │   ├── ContributionResponse.java
│   │   │   │   ├── DashboardResponse.java
│   │   │   │   ├── HeatmapResponse.java
│   │   │   │   └── IssueDetectionResponse.java
│   │   │   │
│   │   │   └── notification/                   # MODULE: Notification
│   │   │       ├── NotificationController.java
│   │   │       ├── NotificationService.java
│   │   │       ├── NotificationRepository.java
│   │   │       ├── Notification.java
│   │   │       ├── CreateNotificationRequest.java
│   │   │       └── NotificationResponse.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       ├── static/
│   │       └── templates/
│   │           ├── welcome-email.html
│   │           ├── task-assigned-email.html
│   │           └── srs-review-email.html
│   │
│   └── test/
│       └── java/com/trackspace/
│           └── (test files)
│
├── pom.xml
└── README.md
```

---

## 📋 Tổng quan Packages

### 1. common/ - Shared Utilities
Chứa các config, exception handlers, response wrappers, và utilities dùng chung.

### 2. auth/ - Authentication & Authorization
Xử lý đăng nhập, đăng ký, JWT token, và security configuration.

### 3. user/ - User Management
Quản lý thông tin user (Admin, Lecturer, Student).

### 4. classroom/ - Class & Group Management
Quản lý lớp học, nhóm sinh viên, và phân công thành viên.

### 5. project/ - Project Management
Quản lý thông tin project và project info của từng nhóm.

### 6. jira/ - Jira Integration
Tích hợp với Jira API, quản lý Sprint và Issue, đồng bộ dữ liệu.

### 7. github/ - GitHub Integration
Tích hợp với GitHub API, theo dõi repository, commits, và contributors.

### 8. srs/ - AI SRS Generation
Tạo tài liệu SRS tự động bằng AI (OpenAI), export PDF/DOCX.

### 9. analytics/ - Contribution Tracking & Analytics
Tính toán metrics đóng góp, phát hiện vấn đề, tạo dashboard và heatmap.

### 10. notification/ - Notification System
Quản lý thông báo cho users (email, in-app notifications).

---

## 🎯 Lợi ích của Flat Package Structure

1. **Đơn giản hơn**: Không cần chia thư mục con controller/service/repository
2. **Dễ navigate**: Tất cả files của 1 module ở cùng 1 chỗ
3. **Nhanh hơn**: Ít thư mục con, dễ tìm file
4. **Phù hợp team nhỏ**: Tối ưu cho team 2-4 người
5. **Gọn gàng**: Package path ngắn hơn
6. **Dễ maintain**: Code tập trung, dễ tìm và sửa lỗi

---

## 📦 Chi tiết API Endpoints

### Auth Module
- POST /api/auth/login
- POST /api/auth/register
- POST /api/auth/refresh-token

### User Module
- GET /api/users
- POST /api/users
- PUT /api/users/{id}
- DELETE /api/users/{id}

### Classroom Module
- GET /api/classes
- POST /api/classes
- GET /api/classes/{id}/students
- POST /api/groups
- PUT /api/groups/{id}/leader

### Project Module
- GET /api/projects/{id}
- PUT /api/projects/{id}/info
- GET /api/projects/{id}/info

### Jira Module
- POST /api/jira/connection
- GET /api/jira/connection/{projectId}
- POST /api/jira/sync
- GET /api/sprints
- POST /api/sprints
- PUT /api/sprints/{id}
- DELETE /api/sprints/{id}
- GET /api/issues
- POST /api/issues
- PUT /api/issues/{id}/status

### GitHub Module
- POST /api/github/connection
- GET /api/github/connection/{projectId}
- POST /api/github/sync
- GET /api/github/repository/{projectId}
- GET /api/github/commits
- GET /api/github/contributors

### SRS Module
- POST /api/srs/generate
- GET /api/srs/{id}
- PUT /api/srs/{id}
- POST /api/srs/{id}/submit
- PUT /api/srs/{id}/review
- GET /api/srs/{id}/export

### Analytics Module
- GET /api/contributions/project/{projectId}
- GET /api/contributions/user/{userId}
- GET /api/contributions/dashboard/{projectId}
- GET /api/contributions/heatmap/{userId}
- GET /api/contributions/issues/{projectId}

### Notification Module
- GET /api/notifications
- PUT /api/notifications/{id}/read
- DELETE /api/notifications/{id}

---

## 📦 Root pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    
    <groupId>com.trackspace</groupId>
    <artifactId>trackspace-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <name>TrackSpace Backend</name>
    <description>Monolithic backend for TrackSpace</description>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>
    
    <properties>
        <java.version>17</java.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>8.0.33</version>
        </dependency>
        
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Swagger/OpenAPI -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- HTTP Client for External APIs -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>
        
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 📁 application.properties

```properties
# Server Configuration
server.port=8080
spring.application.name=trackspace

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/trackspace?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
spring.jpa.properties.hibernate.format_sql=true

# JWT Configuration
jwt.secret=trackspace-secret-key-change-this-in-production
jwt.expiration=86400000

# Swagger Configuration
springdoc.api-docs.path=/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.enabled=true

# File Upload
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Logging
logging.level.root=INFO
logging.level.com.trackspace=DEBUG
logging.level.org.springframework.web=DEBUG
logging.level.org.hibernate.SQL=DEBUG

# CORS
cors.allowed-origins=http://localhost:3000
```

---

## 🚀 Cách Build và Run

### Build project:
```bash
cd backend
mvn clean install
```

### Run application:
```bash
mvn spring-boot:run
```

### Access Swagger UI:
```
http://localhost:8080/swagger-ui.html
```

---

## 📝 Git Workflow

### Branch strategy:
```
main
├── feature/auth
├── feature/user
├── feature/classroom
├── feature/project
├── feature/jira
├── feature/github
├── feature/srs
├── feature/analytics
└── feature/notification
```

### Workflow:
1. Tạo branch từ `main` cho mỗi feature
2. Develop và test trên feature branch
3. Tạo Pull Request vào `main`
4. Review code và merge
5. Deploy từ `main` branch

---

## ✅ So sánh Nested vs Flat Structure

| Tiêu chí | Nested (controller/service/dto/) | Flat (tất cả cùng package) |
|----------|----------------------------------|----------------------------|
| **Số thư mục con** | 4-5 thư mục/module | 0 thư mục con |
| **File path** | `auth/controller/AuthController.java` | `auth/AuthController.java` |
| **Dễ navigate** | Phải click nhiều thư mục | Click 1 lần |
| **Phù hợp team** | 5+ người | 2-4 người |
| **Complexity** | Cao hơn | Thấp hơn |
| **Setup** | Phức tạp hơn | Đơn giản hơn |

**Kết luận**: Flat structure phù hợp hơn cho team nhỏ và dự án vừa!

---

## 📋 Next Steps

1. ✅ Tạo thư mục backend với cấu trúc Flat
2. ⏳ Tạo pom.xml với dependencies
3. ⏳ Tạo TrackSpaceApplication.java (Main class)
4. ⏳ Tạo application.properties
5. ⏳ Tạo sample code cho một số classes cơ bản
6. ⏳ Setup database và test connection
7. ⏳ Implement từng module theo phân công
