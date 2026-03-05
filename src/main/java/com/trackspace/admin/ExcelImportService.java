package com.trackspace.admin;

import com.trackspace.common.BadRequestException;
import com.trackspace.user.User;
import com.trackspace.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "password123";
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private static final Set<String> VALID_ROLES = Set.of("LECTURER", "TEAMLEADER", "TEAMMEMBER");

    /**
     * Import users from Excel file
     */
    @Transactional
    public ImportResult importUsers(MultipartFile file) {
        validateFile(file);

        List<ImportResult.ImportError> errors = new ArrayList<>();
        int successCount = 0;
        int totalRows = 0;

        String encodedPassword = passwordEncoder.encode(DEFAULT_PASSWORD);

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();

            // Find header row and column indices
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BadRequestException("File Excel trống hoặc không có header");
            }

            Map<String, Integer> colMap = mapColumns(headerRow);
            validateRequiredColumns(colMap);

            // Collect existing emails for fast lookup
            Set<String> existingEmails = new HashSet<>();
            userRepository.findAll().forEach(u -> existingEmails.add(u.getEmail().toLowerCase()));

            // Also track emails within this import batch to detect duplicates
            Set<String> batchEmails = new HashSet<>();

            for (int i = 1; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) continue;

                totalRows++;
                int rowNum = i + 1; // 1-indexed for user display

                try {
                    String email = getCellString(row, colMap.get("email")).trim().toLowerCase();
                    String fullName = getCellString(row, colMap.get("fullname")).trim();
                    String roleStr = getCellString(row, colMap.get("role")).trim().toUpperCase();
                    String studentCode = colMap.containsKey("studentcode")
                            ? getCellString(row, colMap.get("studentcode")).trim()
                            : "";

                    // Validate email
                    if (email.isEmpty()) {
                        errors.add(new ImportResult.ImportError(rowNum, "", "Email không được để trống"));
                        continue;
                    }
                    if (!EMAIL_PATTERN.matcher(email).matches()) {
                        errors.add(new ImportResult.ImportError(rowNum, email, "Email không hợp lệ"));
                        continue;
                    }

                    // Validate fullName
                    if (fullName.isEmpty()) {
                        errors.add(new ImportResult.ImportError(rowNum, email, "Họ tên không được để trống"));
                        continue;
                    }

                    // Validate role
                    if (!VALID_ROLES.contains(roleStr)) {
                        errors.add(new ImportResult.ImportError(rowNum, email,
                                "Role không hợp lệ. Chỉ chấp nhận: LECTURER, TEAMLEADER, TEAMMEMBER"));
                        continue;
                    }

                    // Check duplicate in DB
                    if (existingEmails.contains(email)) {
                        errors.add(new ImportResult.ImportError(rowNum, email, "Email đã tồn tại trong hệ thống"));
                        continue;
                    }

                    // Check duplicate in batch
                    if (batchEmails.contains(email)) {
                        errors.add(new ImportResult.ImportError(rowNum, email, "Email trùng lặp trong file"));
                        continue;
                    }

                    // Create user
                    User user = new User();
                    user.setEmail(email);
                    user.setPassword(encodedPassword);
                    user.setFullName(fullName);
                    user.setRole(User.Role.valueOf(roleStr));
                    user.setStudentCode(studentCode.isEmpty() ? null : studentCode);
                    user.setActive(true);

                    userRepository.save(user);
                    existingEmails.add(email);
                    batchEmails.add(email);
                    successCount++;

                } catch (Exception e) {
                    errors.add(new ImportResult.ImportError(rowNum, "",
                            "Lỗi xử lý dòng: " + e.getMessage()));
                }
            }

        } catch (BadRequestException e) {
            throw e;
        } catch (IOException e) {
            throw new BadRequestException("Không thể đọc file Excel: " + e.getMessage());
        }

        return ImportResult.builder()
                .totalRows(totalRows)
                .successCount(successCount)
                .failedCount(errors.size())
                .errors(errors)
                .build();
    }

    /**
     * Generate Excel template file
     */
    public byte[] generateTemplate() {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row header = sheet.createRow(0);
            String[] headers = {"email", "fullName", "role", "studentCode"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, 6000);
            }

            // Example rows
            Row example1 = sheet.createRow(1);
            example1.createCell(0).setCellValue("lecturer@fpt.edu.vn");
            example1.createCell(1).setCellValue("Nguyễn Văn A");
            example1.createCell(2).setCellValue("LECTURER");
            example1.createCell(3).setCellValue("");

            Row example2 = sheet.createRow(2);
            example2.createCell(0).setCellValue("student@fpt.edu.vn");
            example2.createCell(1).setCellValue("Trần Thị B");
            example2.createCell(2).setCellValue("TEAMMEMBER");
            example2.createCell(3).setCellValue("SE1234");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new BadRequestException("Không thể tạo file template");
        }
    }

    // ============ Helper methods ============

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File không được để trống");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException("File quá lớn. Tối đa 5MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new BadRequestException("Chỉ chấp nhận file Excel (.xlsx)");
        }
    }

    private Map<String, Integer> mapColumns(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = getCellString(headerRow, cell.getColumnIndex()).trim().toLowerCase();
            if (!value.isEmpty()) {
                map.put(value, cell.getColumnIndex());
            }
        }
        return map;
    }

    private void validateRequiredColumns(Map<String, Integer> colMap) {
        List<String> missing = new ArrayList<>();
        if (!colMap.containsKey("email")) missing.add("email");
        if (!colMap.containsKey("fullname")) missing.add("fullName");
        if (!colMap.containsKey("role")) missing.add("role");

        if (!missing.isEmpty()) {
            throw new BadRequestException("Thiếu cột bắt buộc: " + String.join(", ", missing));
        }
    }

    private String getCellString(Row row, Integer colIndex) {
        if (colIndex == null) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(row, cell.getColumnIndex()).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }
}
