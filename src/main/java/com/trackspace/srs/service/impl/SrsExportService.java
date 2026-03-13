package com.trackspace.srs.service.impl;

import com.itextpdf.html2pdf.HtmlConverter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

@Service
@Slf4j
public class SrsExportService {

    /**
     * Export Markdown content to PDF.
     */
    public byte[] exportToPdf(String markdownContent, String title) {
        log.info("Exporting SRS to PDF: {}", title);
        
        String htmlContent = renderMarkdownToHtml(markdownContent);
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(htmlContent, out);

        return out.toByteArray();
    }

    /**
     * Export Markdown content to DOCX.
     */
    public byte[] exportToDocx(String markdownContent, String title) {
        log.info("Exporting SRS to DOCX: {}", title);
        try (XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            document.createParagraph().createRun().setBold(true);
            document.createParagraph().createRun().setText(title);

            // Basic tag stripping for DOCX (simple text)
            // A more robust Markdown-to-Word parser might represent tables better.
            String plainText = markdownContent.replaceAll("(?m)^#+\\s*", ""); // crude header strip
            document.createParagraph().createRun().setText(plainText);

            document.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to export DOCX", e);
            throw new RuntimeException("Lỗi khi chuyển đổi file DOCX");
        }
    }
    
    private String renderMarkdownToHtml(String markdown) {
        Parser parser = Parser.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build();
        org.commonmark.node.Node document = parser.parse(markdown);
        HtmlRenderer renderer = HtmlRenderer.builder()
                .extensions(Collections.singletonList(TablesExtension.create()))
                .build();
        
        String htmlBody = renderer.render(document);
        
        // Add basic CSS to make tables look good in PDF
        return "<html><head><style>" +
                "table { border-collapse: collapse; width: 100%; margin-bottom: 20px; font-family: sans-serif; }" +
                "th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
                "th { background-color: #f2f2f2; }" +
                "h1, h2, h3, h4 { font-family: sans-serif; margin-top: 20px; }" +
                "p, li { font-family: sans-serif; font-size: 14px; line-height: 1.5; }" +
                "</style></head><body>" + htmlBody + "</body></html>";
    }
}
