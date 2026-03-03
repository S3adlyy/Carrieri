package entities.gcommu;

import java.time.LocalDateTime;
import java.util.Objects;

public class Conversation {

    private int id;
    private LocalDateTime dateCreation;
    private String dernierMessage;
    private String statut;
    private int user1Id;
    private int user2Id;

    public Conversation() {
    }

    public Conversation(int id, LocalDateTime dateCreation, String dernierMessage,
                        String statut, int user1Id, int user2Id) {
        this.id = id;
        this.dateCreation = dateCreation;
        this.dernierMessage = dernierMessage;
        this.statut = statut;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
    }

    public Conversation(LocalDateTime dateCreation, String statut,
                        int user1Id, int user2Id) {
        this.dateCreation = dateCreation;
        this.statut = statut;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }

    public String getDernierMessage() { return dernierMessage; }
    public void setDernierMessage(String dernierMessage) { this.dernierMessage = dernierMessage; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public int getUser1Id() { return user1Id; }
    public void setUser1Id(int user1Id) { this.user1Id = user1Id; }

    public int getUser2Id() { return user2Id; }
    public void setUser2Id(int user2Id) { this.user2Id = user2Id; }

    @Override
    public String toString() {
        return "Conversation{" +
            "id=" + id +
            ", dateCreation=" + dateCreation +
            ", statut='" + statut + '\'' +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Conversation)) return false;
        Conversation that = (Conversation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
