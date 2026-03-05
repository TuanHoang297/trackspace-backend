TRACKSPACE - CÔNG CỤ HỖ TRỢ QUẢN LÝ DỰ ÁN
==========================================


1. TỔNG QUAN DỰ ÁN (PROJECT CONTEXT)
------------------------------------

1.1 Giới Thiệu

TrackSpace là nền tảng web hỗ trợ quản lý dự án, theo dõi yêu cầu và giám sát tiến độ dành cho ngành Kỹ Thuật Phần Mềm. Hệ thống tích hợp Jira và GitHub để tự động hóa tạo tài liệu, phân tích đóng góp và cung cấp thông tin dự án theo thời gian thực.


1.2 Mục Tiêu

• Thực hành chuyên nghiệp: Giúp sinh viên làm quen với công cụ và quy trình chuẩn công nghiệp
• Trách nhiệm: Theo dõi minh bạch đóng góp cá nhân và nhóm
• Kỹ năng tài liệu: Tự động tạo tài liệu SRS chuyên nghiệp
• Tích hợp công cụ: Kết nối Jira và GitHub vào một nền tảng quản lý duy nhất 


1.3 Phạm Vi Thực Hiện

• Đối tượng sử dụng: Giảng viên và Sinh viên 
• Quy mô: Hỗ trợ nhiều lớp học và nhóm dự án 
• Tích hợp: Jira Cloud + GitHub
• Đầu ra: Tài liệu SRS, báo cáo tiến độ, phân tích đóng góp


2. VẤN ĐỀ CẦN GIẢI QUYẾT (PROBLEM STATEMENT)
---------------------------------------------

Trong quá trình thực hiện đồ án môn học, sinh viên thường gặp khó khăn khi tạo các tài liệu và báo cáo:

• Tạo tài liệu SRS thủ công: Việc tạo tài liệu Đặc tả Yêu cầu Phần mềm (SRS) còn thủ công, tốn thời gian và thiếu tính hệ thống

• Khó tổng hợp báo cáo công việc: Khó theo dõi tình hình phân công và hoàn thành công việc của từng thành viên trong nhóm

• Thiếu công cụ đánh giá đóng góp: Không có công cụ để đánh giá khách quan mức độ đóng góp của từng sinh viên dựa trên dữ liệu từ Jira và GitHub


3. GIẢI PHÁP (SOLUTION)
-----------------------

TrackSpace giải quyết các vấn đề trên thông qua:

• Tích hợp Jira và GitHub: Kết nối và đồng bộ dữ liệu từ Jira (yêu cầu, nhiệm vụ) và GitHub (commit, thay đổi code) vào một nền tảng duy nhất, giúp quản lý tập trung

• Tạo tài liệu SRS tự động: Sử dụng AI để tạo tài liệu SRS từ dữ liệu Jira, hỗ trợ xuất file PDF và DOCX

• Theo dõi tiến độ dự án: Cung cấp dashboard để Giảng viên theo dõi tiến độ nhiệm vụ từ Jira và lịch sử commit từ GitHub của các nhóm

• Thống kê đóng góp thành viên: Tổng hợp dữ liệu từ Jira (task hoàn thành) và GitHub (số commit) để thống kê mức độ đóng góp của từng sinh viên


4. KỸ THUẬT VÀ CÔNG NGHỆ (TECHNOLOGY STACK)
-------------------------------------------

Frontend
• React / Next.js: Xây dựng giao diện người dùng theo component
• HTML, CSS, JavaScript: Các công nghệ web cốt lõi cho phát triển UI

Backend
• Spring Boot (Java): Phát triển RESTful APIs
• Controller – Service – Repository Architecture: Kiến trúc phân tách rõ ràng
• Spring Security + JWT: Xác thực và phân quyền

Database
• MySQL: Lưu trữ dữ liệu hệ thống, dự án và người dùng

Third-party Services
• Jira Cloud REST API: Đồng bộ projects, sprints và issues từ Jira
• GitHub REST API: Thu thập dữ liệu repository và commit để phân tích đóng góp
• Swagger: API documentation và testing

Deployment & Tools
• Vercel: Cloud deployment cho frontend
• Azure: Cloud deployment cho backend

5. ACTORS & FEATURES
--------------------

Actor                   Features
------------------------------------------------------------------------------
Admin                   Tạo, sửa, xóa tài khoản Lecturer và Student
                        Tạo lớp học và phân công Lecturer phụ trách

Lecturer (Giảng viên)   Xem danh sách sinh viên trong lớp
                        Tạo nhóm sinh viên trong lớp
                        Phân công Team Leader cho mỗi nhóm
                        Xem tổng quan các nhóm được phân công
                        Theo dõi tiến độ và nhiệm vụ từ Jira
                        Xem lịch sử commit từ GitHub của các nhóm
                        Xem thống kê đóng góp của từng thành viên
                        Tải tài liệu SRS để review

Team Leader             Kết nối dự án với Jira và GitHub
                        Quản lý Sprint và Task trên Jira (CRUD)
                        Phân công task cho thành viên
                        Theo dõi tiến độ thực hiện công việc
                        Xem thống kê commit của cả nhóm
                        Tạo tài liệu SRS bằng AI

Team Member             Xem nhiệm vụ được phân công
                        Cập nhật trạng thái nhiệm vụ
                        Thêm comment/ghi chú vào task
                        Xem thống kê commit cá nhân và nhóm


6. MAIN FLOWS (QUY TRÌNH NGHIỆP VỤ CHÍNH)
------------------------------------------

Hệ thống TrackSpace có 5 Main Flows chính:

6.1 MF-01: Quản lý Lớp học & Nhóm dự án
   Actor: Admin, Lecturer
   Mục tiêu: Thiết lập hệ thống và tổ chức sinh viên thành nhóm
   
   Quy trình:
   Phase A - Admin thiết lập hệ thống:
   • Admin tạo tài khoản Lecturer với email @fpt.edu.vn
   • Admin import danh sách Student từ file Excel (hệ thống tự động tạo tài khoản)
   • Admin tạo lớp học mới
   • Admin gán Lecturer phụ trách lớp học
   
   Phase B - Lecturer tạo nhóm dự án:
   • Lecturer tạo nhóm mới với tên và mô tả dự án
   • Lecturer thêm sinh viên vào nhóm
   • Lecturer chỉ định Team Leader
   • Hệ thống tự động tạo Project và gửi notification

6.2 MF-02: Thiết lập & Theo dõi Dự án Tích hợp
   Actor: Team Leader
   Mục tiêu: Kết nối Jira/GitHub và quản lý công việc
   
   Quy trình:
   Phase A - Kết nối Jira:
   • Team Leader nhập Jira credentials (Site URL, Email, API Token, Project Key)
   • Hệ thống xác thực và lấy danh sách Projects
   • Team Leader chọn Project cần kết nối
   
   Phase B - Kết nối GitHub:
   • Team Leader nhập Repository URL, Branch, Personal Access Token
   • Hệ thống xác thực quyền truy cập
   
   Phase C - Nhập Project Info:
   • Team Leader nhập Project Info (Topic, Context, Problems, Primary Actors, Functional Requirements)
   • Hệ thống lưu vào database để sử dụng cho SRS generation
   • Lecturer có thể xem Project Info để biết nhóm đang làm đề tài gì
   
   Phase D - Đồng bộ và Quản lý Công việc:
   • Team Leader đồng bộ dữ liệu Sprints, Issues, Commits
   • Team Leader tạo Sprint và Issue trực tiếp trên TrackSpace
   • Team Leader phân công Tasks cho Team Members
   • Hệ thống đồng bộ lên Jira và gửi notification

6.3 MF-03: Thực hiện Công việc Dự án
   Actor: Team Member, Team Leader
   Mục tiêu: Thực hiện và hoàn thành các task được giao
   
   Quy trình:
   Phase A - Nhận và Bắt đầu Công việc:
   • Team Member xem tasks trong "My Tasks Dashboard"
   • Team Member chọn task và click "Start Working"
   • Hệ thống chuyển status sang "In Progress" và đồng bộ lên Jira
   
   Phase B - Thực hiện và Commit Code:
   • Team Member clone repository và tạo branch mới
   • Team Member viết code và test
   • Team Member commit với message chứa Task ID
   • Team Member push code và tạo Pull Request
   
   Phase C - Hoàn thành Task:
   • Team Leader review và approve Pull Request
   • Team Member merge code vào branch chính
   • Team Member click "Mark as Done"
   • Hệ thống đồng bộ commits và tính toán contribution

6.4 MF-04: Theo dõi Tiến độ & Đánh giá Đóng góp
   Actor: Lecturer
   Mục tiêu: Theo dõi tiến độ và phân tích đóng góp sinh viên
   
   Quy trình:
   Phase A - Xem Tổng quan Các Nhóm:
   • Lecturer xem danh sách tất cả nhóm trong lớp
   • Hệ thống hiển thị: Sprint Completion %, Tasks Pending, Active Members, Health Status
   • Lecturer có thể click "View Project Info" để xem thông tin đề tài (Topic, Context, Problems, Primary Actors, Functional Requirements)
   
   Phase B - Xem Sprint Board và Jira Data:
   • Lecturer xem tất cả Sprint và Task của nhóm
   
   Phase C - Xem Thông tin GitHub Repository:
   • Lecturer xem commit history với chi tiết
   • Lecturer xem contribution metrics từ GitHub
   
   Phase D - Xem Dashboard Đánh giá Đóng góp:
   • Lecturer xem bảng so sánh đóng góp từng thành viên
   • Hệ thống hiển thị: Tasks Done, Commits, Lines Changed, Contribution %, Commit Quality Score

6.5 MF-05: Tạo Tài liệu SRS Tự động
   Actor: Student (Generate, Edit, Export), Lecturer (View, Download)
   Mục tiêu: Tự động tạo tài liệu SRS bằng AI
   
   Quy trình:
   Phase A - Generate SRS:
   • Student click "Generate SRS"
   • Hệ thống tự động lấy Project Info từ database
   • Hệ thống lấy tất cả Issues từ Jira
   • AI generate SRS theo template đã train sẵn
   • Hệ thống hiển thị preview SRS
   
   Phase B - Edit SRS:
   • Student chỉnh sửa nội dung trong Rich Text Editor
   • Student thêm diagrams/screenshots
   • Student save version mới
   
   Phase C - Export SRS:
   • Student export SRS sang PDF hoặc DOCX
   • Student download file về máy
   
   Phase D - Lecturer View & Download:
   • Lecturer xem SRS (read-only) của các nhóm
   • Lecturer download SRS dưới dạng PDF/DOCX để review offline


7. MAIN FEATURES (TÍNH NĂNG CHÍNH)
-----------------------------------

Hệ thống TrackSpace có 6 Main Features:

7.1 User & Group Management (Quản lý Người dùng & Nhóm)
   Actor: Admin, Lecturer
   
   Tính năng chính:
   • Admin tạo tài khoản Lecturer và import Student từ Excel
   • Admin tạo lớp học và gán Lecturer phụ trách
   • Lecturer tạo nhóm dự án và chỉ định Team Leader
   • Hệ thống gửi email và notification tự động
   • Phân quyền theo vai trò: Admin, Lecturer, Team Leader, Team Member

7.2 Jira Integration (Tích hợp Jira)
   Actor: Team Leader (Setup & Management), Team Member (Execution), Lecturer (Monitoring)
   
   Kiến trúc tích hợp:
   • Authentication: Jira REST API v3 với API Token
   • Sync Strategy: Bidirectional sync (TrackSpace ↔ Jira)
   • Sync Frequency: Auto-sync mỗi 30 phút + Manual sync on-demand
   • Data Scope: Sprints, Issues (Epic, Story, Task), Issue metadata
   
   Tính năng chính:
   • 2.1. Connection Setup: Kết nối với Jira Cloud
   • 2.2. Sprint Board Management: Hiển thị Sprint theo cột Kanban-style
   • 2.3. Sprint CRUD Operations: Team Leader CRUD Sprint trực tiếp trên TrackSpace
   • 2.4. Issue CRUD Operations: Team Leader CRUD Issue trong Sprint
   • 2.5. Task Status Management: Team Member cập nhật status (To Do → In Progress → Done)
   • 2.6. Sprint Monitoring: Lecturer xem Sprint board read-only
   • 2.7. Data Synchronization: Auto-sync với conflict resolution
  

7.3 GitHub Integration (Tích hợp GitHub)
   Actor: Team Leader (Setup), All Users (View)
   
   Kiến trúc tích hợp:
   • Authentication: GitHub REST API v3 với Personal Access Token
   • Sync Strategy: One-way sync (GitHub → TrackSpace)
   • Sync Frequency: Auto-sync mỗi 30 phút
   • Data Scope: Repository info, Branches, Commits, Contributors
   
   Tính năng chính:
   • 3.1. Connection Setup: Kết nối với GitHub Repository
   • 3.2. Repository Overview: Xem thông tin repository (Name, Description, Language, Size, Stars, Forks)
   • 3.3. Commit History: Xem commits với filter theo branch/author/date range
   • 3.4. Contributor Insights: Xem statistics và charts về contribution

7.4 Contribution Tracking & Analysis (Theo dõi & Phân tích Đóng góp)
   Actor: Lecturer, Student (All can view all members)
   
   Tính năng chính:
   • 4.1. Contribution Metrics: Tự động tính Task Metrics, Commit Metrics, Contribution Score
   • 4.2. Contribution Dashboard: Bảng so sánh đóng góp của tất cả thành viên
   • 4.3. Activity Heatmap: Calendar view hiển thị hoạt động theo ngày
   • 4.4. Issue Detection: Tự động phát hiện Inactive >3 ngày, Low contribution, Overdue tasks

7.5 AI-Powered SRS Generation (Tạo SRS tự động bằng AI)
   Actor: Student (Generate & Edit), Lecturer (Review & Approve)
   
   Tính năng chính:
   • 5.1. SRS Generator: AI tự động generate SRS từ Project Info (database) và Jira Issues
   • 5.2. SRS Editor: Rich Text Editor cho phép chỉnh sửa, thêm diagrams/screenshots
   • 5.3. SRS Export: Export sang PDF hoặc DOCX
   • 5.4. SRS Viewer: Lecturer xem SRS (read-only) và download để review offline

7.6 Notification System (Hệ thống Thông báo)
   Actor: Tất cả
   
   Tính năng chính:
   • In-App Notifications: Hiển thị thông báo trong hệ thống
   • Email Notifications: Gửi email cho sự kiện quan trọng
   • Notification Types: Account created, Team Leader assigned, Task assigned, Task status changed, Issue detected


8. FEATURE PRIORITY (ƯU TIÊN TÍNH NĂNG)
----------------------------------------

Must Have (P0) - Bắt buộc phải có:
• User & Group Management
• Jira Integration
• GitHub Integration

Should Have (P1) - Nên có:
• Contribution Tracking & Analysis
• Notification System

Nice to Have (P2) - Tốt nếu có:
• AI-Powered SRS Generation




========================================
Phiên bản: 2.0
Cập nhật: 29/01/2026
Trạng thái: Đã thêm Main Flows và Main Features
========================================