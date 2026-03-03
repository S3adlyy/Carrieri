// Track.java
package entities.guser;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Track {
    private int id;
    private int workspaceId;

    private String title;
    private String description;
    private String category;    // PROJECT / EDUCATION / EXPERIENCE / ACTIVITY
    private String status;      // optional
    private String visibility;  // PUBLIC / PRIVATE

    private LocalDate startDate;
    private LocalDate endDate;

    private LocalDateTime createdAt;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(int workspaceId) { this.workspaceId = workspaceId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
