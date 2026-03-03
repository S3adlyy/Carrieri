package entities.guser;

import java.time.LocalDateTime;

public class Artifact {
    private int id;
    private int trackId;

    private String artifactName;
    private String artifactDescription;
    private String artifactType; // CODE/IMAGE/VIDEO/DOCUMENT/LINK/TEXT
    private String language;     // optional for CODE

    // In your SQL it’s test_content (typo), keep Java name clean:
    private String textContent;

    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTrackId() { return trackId; }
    public void setTrackId(int trackId) { this.trackId = trackId; }

    public String getArtifactName() { return artifactName; }
    public void setArtifactName(String artifactName) { this.artifactName = artifactName; }

    public String getArtifactDescription() { return artifactDescription; }
    public void setArtifactDescription(String artifactDescription) { this.artifactDescription = artifactDescription; }

    public String getArtifactType() { return artifactType; }
    public void setArtifactType(String artifactType) { this.artifactType = artifactType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTextContent() { return textContent; }
    public void setTextContent(String textContent) { this.textContent = textContent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public boolean isDeleted() { return deletedAt != null; }
}
