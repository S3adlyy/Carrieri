package entities.greclam;

import java.util.Date;
import java.util.Objects;

public class Feedback {
    private int id;
    private String commentaire;
    private int note;
    private Date createdAt;

    public Feedback() {
    }

    public Feedback(int id, String commentaire, int note, Date createdAt) {
        this.id = id;
        this.commentaire = commentaire;
        this.note = note;
        this.createdAt = createdAt;
    }

    public Feedback(String commentaire, int note, Date createdAt) {
        this.commentaire = commentaire;
        this.note = note;
        this.createdAt = createdAt;
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


    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + id +
                ", commentaire='" + commentaire + '\'' +
                ", note=" + note +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Feedback feedback = (Feedback) o;
        return id == feedback.id &&
                note == feedback.note &&
                Objects.equals(commentaire, feedback.commentaire) &&
                Objects.equals(createdAt, feedback.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, commentaire, note, createdAt);
    }
}