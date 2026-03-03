package entities.guser;

import java.time.LocalDateTime;

public class FileObject {
    private int id;
    private int artifactId;

    private String storageKey;
    private String publicUrl; // can be placeholder if you rely on presigned URLs
    private String mimeType;

    private long fileSize;
    private LocalDateTime uploadedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getArtifactId() { return artifactId; }
    public void setArtifactId(int artifactId) { this.artifactId = artifactId; }

    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
}
