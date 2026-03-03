package services.guser;

import utils.guser.S3StorageService;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CodeBrowseService {

    public static final class ZipNode {
        private final String name;
        private final String fullPath;
        private final boolean directory;
        private final Map<String, ZipNode> children = new TreeMap<>();

        public ZipNode(String name, String fullPath, boolean directory) {
            this.name = name;
            this.fullPath = fullPath;
            this.directory = directory;
        }

        public String getName() { return name; }
        public String getFullPath() { return fullPath; }
        public boolean isDirectory() { return directory; }
        public Collection<ZipNode> getChildren() { return children.values(); }

        private ZipNode childOrCreate(String childName, String childFullPath, boolean dir) {
            ZipNode existing = children.get(childName);
            if (existing != null) return existing;
            ZipNode created = new ZipNode(childName, childFullPath, dir);
            children.put(childName, created);
            return created;
        }
    }

    private final String bucket;
    private final String region;

    public CodeBrowseService(String bucket, String region) {
        this.bucket = bucket;
        this.region = region;
    }

    public Path downloadZipToTemp(String storageKey) throws Exception {
        String url;
        try (S3StorageService s3 = new S3StorageService(bucket, region)) {
            url = s3.presignedGetUrl(storageKey, Duration.ofMinutes(10));
        }

        Path tmp = Files.createTempFile("carrieri-code-", ".zip");

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<Path> res = client.send(req, HttpResponse.BodyHandlers.ofFile(tmp));
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            try { Files.deleteIfExists(tmp); } catch (Exception ignored) {}
            throw new IOException("Failed to download ZIP. HTTP " + res.statusCode());
        }
        return tmp;
    }

    public ZipNode buildTree(Path zipPath) throws IOException {
        ZipNode root = new ZipNode("", "", true);

        try (InputStream in = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String path = normalize(e.getName());
                if (path.isEmpty()) continue;
                insert(root, path, e.isDirectory());
            }
        }
        return root;
    }

    public Optional<String> findReadmeAtRoot(ZipNode root) {
        for (ZipNode n : root.children.values()) {
            if (!n.directory && n.name.equalsIgnoreCase("README.md")) return Optional.of(n.fullPath);
        }
        return Optional.empty();
    }

    public Optional<String> findFirstFile(ZipNode root) {
        return dfsFirstFile(root);
    }

    public String readTextFile(Path zipPath, String entryPath) throws IOException {
        String wanted = normalize(entryPath);

        try (InputStream in = Files.newInputStream(zipPath);
             ZipInputStream zis = new ZipInputStream(in)) {

            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                String name = normalize(e.getName());
                if (wanted.equals(name)) {
                    byte[] bytes = zis.readAllBytes();
                    return new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }
        throw new FileNotFoundException("File not found in zip: " + entryPath);
    }

    private Optional<String> dfsFirstFile(ZipNode node) {
        for (ZipNode c : node.getChildren()) {
            if (!c.isDirectory()) return Optional.of(c.getFullPath());
            Optional<String> inside = dfsFirstFile(c);
            if (inside.isPresent()) return inside;
        }
        return Optional.empty();
    }

    private void insert(ZipNode root, String path, boolean isDir) {
        String[] parts = path.split("/");
        ZipNode cur = root;

        StringBuilder full = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;

            if (full.length() > 0) full.append("/");
            full.append(p);

            boolean last = (i == parts.length - 1);
            boolean dir = last ? isDir : true;

            cur = cur.childOrCreate(p, full.toString(), dir);
        }
    }

    private String normalize(String p) {
        if (p == null) return "";
        p = p.replace("\\", "/").trim();
        while (p.startsWith("/")) p = p.substring(1);
        while (p.endsWith("/") && p.length() > 1) p = p.substring(0, p.length() - 1);
        return p;
    }
}
