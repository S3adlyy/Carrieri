package entities;

import java.util.Date;
import java.util.Objects;

public class Feedback {
    private int id;
    private String commentaire;
    private int note;
    private Date createdAt;
    private int renduId;

    public Feedback() {
    }

    public Feedback(int id, String commentaire, int note, Date createdAt, int renduId) {
        this.id = id;
        this.commentaire = commentaire;
        this.note = note;
        this.createdAt = createdAt;
        this.renduId = renduId;
    }

    public Feedback(String commentaire, int note, Date createdAt, int renduId) {
        this.commentaire = commentaire;
        this.note = note;
        this.createdAt = createdAt;
        this.renduId = renduId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCommentaire() {
        return commentaire;
    }

    public void setCommentaire(String commentaire) {
        this.commentaire = commentaire;
    }

    public int getNote() {
        return note;
    }

    public void setNote(int note) {
        this.note = note;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getRenduId() {
        return renduId;
    }

    public void setRenduId(int renduId) {
        this.renduId = renduId;
    }

    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + id +
                ", commentaire='" + commentaire + '\'' +
                ", note=" + note +
                ", createdAt=" + createdAt +
                ", renduId=" + renduId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Feedback feedback = (Feedback) o;
        return id == feedback.id &&
                note == feedback.note &&
                renduId == feedback.renduId &&
                Objects.equals(commentaire, feedback.commentaire) &&
                Objects.equals(createdAt, feedback.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, commentaire, note, createdAt, renduId);
    }
}