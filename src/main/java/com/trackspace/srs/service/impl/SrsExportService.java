package com.trackspace.srs.service.impl;

import com.itextpdf.html2pdf.HtmlConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
@Slf4j
public class SrsExportService {

    /**
     * Export HTML content to PDF.
     */
    public byte[] exportToPdf(String htmlContent, String title) {
        log.info("Exporting SRS to PDF: {}", title);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        HtmlConverter.convertToPdf(htmlContent, out);

        return out.toByteArray();
    }

    /**
     * Export HTML content to DOCX.
     */
    public byte[] exportToDocx(String htmlContent, String title) {
        log.info("Exporting SRS to DOCX: {}", title);
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            document.createParagraph().createRun().setBold(true);
            document.createParagraph().createRun().setText(title);

            // Basic tag stripping for DOCX (simple text)
            // For a better DOCX export, a more robust HTML-to-Word parser would be needed
            String plainText = htmlContent.replaceAll("<[^>]*>", "");
            document.createParagraph().createRun().setText(plainText);

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export DOCX", e);
            throw new RuntimeException("Lỗi khi chuyển đổi file DOCX");
        }
    }
}
