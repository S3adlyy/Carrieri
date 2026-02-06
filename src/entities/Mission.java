package entities;

import java.time.LocalDateTime;

public class Mission {
    private int id;
    private String description;
    private int score_min;
    private LocalDateTime created_at;
    private Integer created_by_id;

    public Mission() {
    }

    public Mission(int id, String description, int score_min, LocalDateTime created_at, Integer created_by_id) {
        this.id = id;
        this.description = description;
        this.score_min = score_min;
        this.created_at = created_at;
        this.created_by_id = created_by_id;
    }

    public Mission(String description, int score_min, Integer created_by_id) {
        this.description = description;
        this.score_min = score_min;
        this.created_by_id = created_by_id;
        this.created_at = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getScore_min() {
        return score_min;
    }

    public void setScore_min(int score_min) {
        this.score_min = score_min;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public Integer getCreated_by_id() {
        return created_by_id;
    }

    public void setCreated_by_id(Integer created_by_id) {
        this.created_by_id = created_by_id;
    }

    @Override
    public String toString() {
        return "Mission{" +
                "id=" + id +
                ", description='" + description + '\'' +
                ", score_min=" + score_min +
                ", created_at=" + created_at +
                ", created_by_id=" + created_by_id +
                '}';
    }
}