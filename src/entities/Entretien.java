package entities;

import java.time.LocalDateTime;

public class Entretien {
    private int id;
    private LocalDateTime dateEntretien;
    private String type;
    private String status;
    private Integer postulationId;

    // Additional fields for display purposes
    private String candidatName;
    private String candidatEmail;
    private int candidatId;

    // Constructors
    public Entretien() {
        this.status = "SCHEDULED"; // Default status
    }

    public Entretien(LocalDateTime dateEntretien, String type, Integer postulationId) {
        this.dateEntretien = dateEntretien;
        this.type = type;
        this.postulationId = postulationId;
        this.status = "SCHEDULED";
    }

    public Entretien(int id, LocalDateTime dateEntretien, String type, String status, Integer postulationId) {
        this.id = id;
        this.dateEntretien = dateEntretien;
        this.type = type;
        this.status = status;
        this.postulationId = postulationId;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public LocalDateTime getDateEntretien() {
        return dateEntretien;
    }

    public void setDateEntretien(LocalDateTime dateEntretien) {
        this.dateEntretien = dateEntretien;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getPostulationId() {
        return postulationId;
    }

    public void setPostulationId(Integer postulationId) {
        this.postulationId = postulationId;
    }

    public String getCandidatName() {
        return candidatName;
    }

    public void setCandidatName(String candidatName) {
        this.candidatName = candidatName;
    }

    public String getCandidatEmail() {
        return candidatEmail;
    }

    public void setCandidatEmail(String candidatEmail) {
        this.candidatEmail = candidatEmail;
    }

    public int getCandidatId() {
        return candidatId;
    }

    public void setCandidatId(int candidatId) {
        this.candidatId = candidatId;
    }

    @Override
    public String toString() {
        return "Entretien{" +
                "id=" + id +
                ", dateEntretien=" + dateEntretien +
                ", type='" + type + '\'' +
                ", status='" + status + '\'' +
                ", postulationId=" + postulationId +
                '}';
    }
}