package com.trackspace.srs.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class SrsExportService {

    /**
     * Export HTML content to Word-compatible format (.doc).
     * Uses HTML-as-Word approach to preserve 100% of web formatting.
     */
    public byte[] exportToDoc(String htmlContent, String title) {
        log.info("Exporting SRS to DOC (HTML-as-Word): {}", title);

        // Replace <hr> with Word-compatible page breaks
        String processedHtml = htmlContent != null ? htmlContent : "";
        processedHtml = processedHtml
                .replaceAll("<hr\\s*/?>", "<br clear='all' style='mso-special-character:line-break;page-break-before:always'>");

        // Center images with data-align="center"
        processedHtml = processedHtml.replaceAll(
                "data-align=\"center\"",
                "data-align=\"center\" style=\"text-align:center\""
        );

        String wordDoc = """
                <html xmlns:o="urn:schemas-microsoft-com:office:office"
                      xmlns:w="urn:schemas-microsoft-com:office:word"
                      xmlns="http://www.w3.org/TR/REC-html40">
                <head>
                <meta charset="utf-8">
                <xml>
                  <w:WordDocument>
                    <w:View>Print</w:View>
                    <w:Zoom>100</w:Zoom>
                    <w:DoNotOptimizeForBrowser/>
                  </w:WordDocument>
                </xml>
                <style>
                  @page {
                    size: A4;
                    margin: 2.5cm 2cm 2.5cm 2cm;
                  }
                  * {
                    font-family: Calibri, sans-serif !important;
                  }
                  body {
                    font-family: Calibri, sans-serif;
                    font-size: 11pt;
                    line-height: 1.5;
                    color: #000;
                  }
                  h1 {
                    font-family: Calibri, sans-serif;
                    font-size: 28pt;
                    font-weight: bold;
                    text-align: center;
                    margin-top: 20px;
                    margin-bottom: 8px;
                    color: #c00000;
                  }
                  h2 {
                    font-family: Calibri, sans-serif;
                    font-size: 16pt;
                    font-weight: bold;
                    margin-top: 15px;
                    margin-bottom: 15px;
                    color: #c00000;
                  }
                  h3 {
                    font-family: Calibri, sans-serif;
                    font-size: 13pt;
                    font-weight: bold;
                    margin-top: 20px;
                    margin-bottom: 10px;
                    color: #000;
                  }
                  h4 {
                    font-family: Calibri, sans-serif;
                    font-size: 11pt;
                    font-weight: bold;
                    margin-top: 15px;
                    margin-bottom: 5px;
                    color: #000;
                  }
                  h5 {
                    font-family: Calibri, sans-serif;
                    font-size: 22pt;
                    font-weight: bold;
                    text-align: center;
                    margin-top: 0;
                    margin-bottom: 8px;
                    color: #c00000;
                  }
                  p {
                    font-family: Calibri, sans-serif;
                    text-align: justify;
                    margin-top: 0;
                    margin-bottom: 10px;
                  }
                  table {
                    font-family: Calibri, sans-serif;
                    border-collapse: collapse;
                    width: 100%%;
                    margin-bottom: 15px;
                  }
                  table th, table td {
                    font-family: Calibri, sans-serif;
                    border: 1px solid #000;
                    padding: 8px;
                    text-align: left;
                    vertical-align: top;
                  }
                  table th {
                    background-color: #f5d5c8;
                    font-weight: bold;
                    color: #1e293b;
                  }
                  img {
                    max-width: 100%%;
                    height: auto;
                    display: block;
                    margin: 8px auto;
                  }
                  [data-align="center"] {
                    text-align: center;
                  }
                  [data-align="center"] img {
                    margin: 8px auto;
                  }
                  blockquote {
                    font-family: Calibri, sans-serif;
                    border-left: 3px solid #cbd5e1;
                    margin-left: 20px;
                    padding-left: 12px;
                    color: #64748b;
                  }
                  a {
                    font-family: Calibri, sans-serif;
                    color: #2563eb;
                    text-decoration: underline;
                  }
                  ul, ol {
                    font-family: Calibri, sans-serif;
                    margin-top: 4pt;
                    margin-bottom: 8pt;
                  }
                  li {
                    font-family: Calibri, sans-serif;
                    margin-bottom: 2pt;
                  }
                  span {
                    font-family: Calibri, sans-serif;
                  }
                  .srs-ai-action { display: none !important; }
                  div[data-type="aiActionButton"] { display: none !important; }
                </style>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(processedHtml);

        // Add BOM for proper Unicode handling in Word
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] content = wordDoc.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);

        log.info("DOC generated successfully, size: {} bytes", result.length);
        return result;
    }
}
