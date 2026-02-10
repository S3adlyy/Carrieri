package services;

import entities.Message;
import java.sql.SQLException;
import java.util.List;

public interface IMessageService extends IService<Message> {

    // Récupérer tous les messages d’une conversation
    List<Message> getMessagesByConversation(int idConversation) throws SQLException;

    // Récupérer messages envoyés par un utilisateur
    List<Message> getMessagesEnvoyes(int idUser) throws SQLException;

    // Récupérer messages reçus par un utilisateur
    List<Message> getMessagesRecus(int idUser) throws SQLException;

    // Marquer un message comme supprimé (suppression logique)
    void supprimerLogique(int idMessage) throws SQLException;

    // Rechercher message par mot clé
    List<Message> rechercherParContenu(String mot) throws SQLException;
}
