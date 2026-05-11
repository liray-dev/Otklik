package com.normilinet.otklik.service;

import com.normilinet.otklik.domain.enums.FileKind;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileStorageService {

    @Value("${app.file-storage.upload-dir:./uploads}")
    private String uploadDir;

    private Path root;

    @PostConstruct
    void init() throws IOException {
        root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public Path rootPath() {
        return root;
    }

    public StoredFile store(MultipartFile file, String relativeFolder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл пуст");
        }
        String original = sanitize(file.getOriginalFilename());
        String stored = UUID.randomUUID() + "_" + original;
        Path folder = root.resolve(relativeFolder).normalize();
        if (!folder.startsWith(root)) {
            throw new SecurityException("Path traversal");
        }
        Files.createDirectories(folder);
        Path target = folder.resolve(stored);
        try (var in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return new StoredFile(
                relativeFolder + "/" + stored,
                original,
                file.getContentType(),
                file.getSize()
        );
    }

    public StoredFile storeVoice(byte[] data, String relativeFolder) throws IOException {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Аудио пустое");
        }
        String stored = UUID.randomUUID() + ".webm";
        Path folder = root.resolve(relativeFolder).normalize();
        if (!folder.startsWith(root)) {
            throw new SecurityException("Path traversal");
        }
        Files.createDirectories(folder);
        Path target = folder.resolve(stored);
        Files.write(target, data);
        return new StoredFile(relativeFolder + "/" + stored, stored, "audio/webm", (long) data.length);
    }

    public Path resolve(String storedPath) {
        Path p = root.resolve(storedPath).normalize();
        if (!p.startsWith(root)) {
            throw new SecurityException("Path traversal");
        }
        return p;
    }

    public void delete(String storedPath) throws IOException {
        if (storedPath == null) return;
        Files.deleteIfExists(resolve(storedPath));
    }

    public static String sanitize(String filename) {
        if (filename == null) return "file";
        String base = Paths.get(filename).getFileName().toString();
        base = base.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_").trim();
        if (base.isEmpty()) return "file";
        if (base.length() > 200) base = base.substring(0, 200);
        return base;
    }

    public static FileKind classify(String filename, String mime) {
        String ext = extension(filename);
        Set<String> docs = Set.of("pdf", "docx", "doc", "odt", "rtf", "txt", "md");
        Set<String> sheets = Set.of("xlsx", "xls", "csv", "tsv", "ods");
        Set<String> slides = Set.of("pptx", "ppt", "odp");
        Set<String> images = Set.of("png", "jpg", "jpeg", "gif", "webp", "svg", "bmp", "heic");
        Set<String> audio = Set.of("mp3", "wav", "ogg", "m4a", "weba");
        Set<String> video = Set.of("mp4", "webm", "mov");
        Set<String> code = Set.of("py", "java", "js", "ts", "tsx", "jsx", "cpp", "c", "cs", "kt", "go", "rs", "sql", "html", "css", "xml", "json", "yaml", "yml", "sh", "rb", "php");
        Set<String> archives = Set.of("zip", "tar", "gz", "tgz", "rar", "7z");
        if (docs.contains(ext)) return FileKind.DOCUMENT;
        if (sheets.contains(ext)) return FileKind.SPREADSHEET;
        if (slides.contains(ext)) return FileKind.PRESENTATION;
        if (images.contains(ext)) return FileKind.IMAGE;
        if (audio.contains(ext)) return FileKind.AUDIO;
        if (video.contains(ext)) return FileKind.VIDEO;
        if (code.contains(ext)) return FileKind.CODE;
        if (archives.contains(ext)) return FileKind.ARCHIVE;
        if (mime != null) {
            if (mime.startsWith("image/")) return FileKind.IMAGE;
            if (mime.startsWith("audio/")) return FileKind.AUDIO;
            if (mime.startsWith("video/")) return FileKind.VIDEO;
        }
        return FileKind.OTHER;
    }

    public static String extension(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return "";
        return filename.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static final Map<String, String> EXT_MIME = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("doc", "application/msword"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("mp3", "audio/mpeg"),
            Map.entry("wav", "audio/wav"),
            Map.entry("ogg", "audio/ogg"),
            Map.entry("webm", "video/webm"),
            Map.entry("mp4", "video/mp4"),
            Map.entry("zip", "application/zip"),
            Map.entry("txt", "text/plain"),
            Map.entry("md", "text/markdown"),
            Map.entry("csv", "text/csv"),
            Map.entry("json", "application/json")
    );

    public static String guessMime(String filename) {
        String ext = extension(filename);
        return EXT_MIME.getOrDefault(ext, "application/octet-stream");
    }

    public record StoredFile(String relativePath, String originalName, String mimeType, long size) {}
}
