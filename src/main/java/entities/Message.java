package entities;

import java.time.LocalDateTime;
import java.util.Objects;

public class Message {

    private int id;
    private String contenu;
    private LocalDateTime dateEnvoi;
    private LocalDateTime dateModification;
    private String statut;
    private String type;

    private int conversationId;
    private int expediteurId;
    private int destinataireId;

    public Message() {}

    public Message(String contenu, LocalDateTime dateEnvoi, String statut,
                   String type, int conversationId,
                   int expediteurId, int destinataireId) {

        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.statut = statut;
        this.type = type;
        this.conversationId = conversationId;
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
    }

    // Getters & Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(LocalDateTime dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getConversationId() { return conversationId; }
    public void setConversationId(int conversationId) { this.conversationId = conversationId; }

    public int getExpediteurId() { return expediteurId; }
    public void setExpediteurId(int expediteurId) { this.expediteurId = expediteurId; }

    public int getDestinataireId() { return destinataireId; }
    public void setDestinataireId(int destinataireId) { this.destinataireId = destinataireId; }

    @Override
    public String toString() {
        return "Message{" +
            "id=" + id +
            ", contenu='" + contenu + '\'' +
            ", dateEnvoi=" + dateEnvoi +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Message)) return false;
        Message message = (Message) o;
        return id == message.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
