package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.FileKind;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class DocumentRenderService {

    public String renderToHtml(Path file, String originalFilename) throws IOException {
        String ext = FileStorageService.extension(originalFilename);
        return switch (ext) {
            case "docx" -> renderDocx(file);
            case "doc" -> renderDoc(file);
            case "xlsx", "xls" -> renderSpreadsheet(file);
            case "csv" -> renderCsv(file, ',');
            case "tsv" -> renderCsv(file, '\t');
            case "txt", "md" -> renderText(file, ext);
            case "zip" -> renderZipListing(file);
            case "rtf" -> renderText(file, "txt");
            default -> "<div class=\"viewer__fallback\">Предпросмотр недоступен для этого типа файла.</div>";
        };
    }

    private String renderDocx(Path file) throws IOException {
        StringBuilder html = new StringBuilder("<div class=\"docx\">");
        try (InputStream in = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(in)) {
            for (XWPFParagraph p : doc.getParagraphs()) {
                String style = p.getStyle();
                String tag = "p";
                if (style != null) {
                    if (style.startsWith("Heading1") || style.startsWith("1")) tag = "h2";
                    else if (style.startsWith("Heading2") || style.startsWith("2")) tag = "h3";
                    else if (style.startsWith("Heading3")) tag = "h4";
                }
                html.append("<").append(tag).append(">");
                for (XWPFRun run : p.getRuns()) {
                    String text = run.getText(0);
                    if (text == null) continue;
                    String escaped = escape(text);
                    if (run.isBold()) escaped = "<strong>" + escaped + "</strong>";
                    if (run.isItalic()) escaped = "<em>" + escaped + "</em>";
                    html.append(escaped);
                }
                html.append("</").append(tag).append(">");
            }
        }
        html.append("</div>");
        return html.toString();
    }

    private String renderDoc(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file); HWPFDocument doc = new HWPFDocument(in)) {
            WordExtractor ex = new WordExtractor(doc);
            StringBuilder sb = new StringBuilder("<div class=\"docx\">");
            for (String p : ex.getParagraphText()) {
                sb.append("<p>").append(escape(p)).append("</p>");
            }
            sb.append("</div>");
            return sb.toString();
        }
    }

    private String renderSpreadsheet(Path file) throws IOException {
        StringBuilder html = new StringBuilder();
        try (InputStream in = Files.newInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                Sheet sheet = wb.getSheetAt(s);
                html.append("<h4>").append(escape(sheet.getSheetName())).append("</h4>");
                html.append("<table>");
                for (Row row : sheet) {
                    html.append("<tr>");
                    for (Cell cell : row) {
                        html.append("<td>").append(escape(cellAsString(cell))).append("</td>");
                    }
                    html.append("</tr>");
                }
                html.append("</table>");
            }
        }
        return html.toString();
    }

    private String renderCsv(Path file, char delim) throws IOException {
        StringBuilder html = new StringBuilder("<table>");
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(r, CSVFormat.DEFAULT.builder().setDelimiter(delim).build())) {
            for (CSVRecord rec : parser) {
                html.append("<tr>");
                rec.forEach(v -> html.append("<td>").append(escape(v)).append("</td>"));
                html.append("</tr>");
            }
        }
        html.append("</table>");
        return html.toString();
    }

    private String renderText(Path file, String ext) throws IOException {
        String content = Files.readString(file, StandardCharsets.UTF_8);
        if ("md".equals(ext)) {
            return "<div class=\"md\"><pre>" + escape(content) + "</pre></div>";
        }
        return "<pre>" + escape(content) + "</pre>";
    }

    private String renderZipListing(Path file) throws IOException {
        StringBuilder html = new StringBuilder("<table><thead><tr><th>Файл</th><th>Размер</th></tr></thead><tbody>");
        try (InputStream in = Files.newInputStream(file); ZipInputStream zin = new ZipInputStream(in)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                html.append("<tr><td>").append(escape(e.getName())).append("</td><td>")
                        .append(e.getSize() < 0 ? "—" : e.getSize() + " B").append("</td></tr>");
            }
        }
        html.append("</tbody></table>");
        return html.toString();
    }

    private String cellAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d)) yield String.valueOf((long) d);
                yield String.valueOf(d);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    public static String escape(String raw) {
        if (raw == null) return "";
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    public static boolean isInlineViewable(FileKind kind) {
        return kind == FileKind.DOCUMENT || kind == FileKind.IMAGE || kind == FileKind.SPREADSHEET
                || kind == FileKind.AUDIO || kind == FileKind.VIDEO || kind == FileKind.CODE
                || kind == FileKind.ARCHIVE;
    }
}
