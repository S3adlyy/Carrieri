package services;

import entities.Conversation;
import java.sql.SQLException;
import java.util.List;

public interface IConversationService extends IService<Conversation> {

    // Récupérer toutes les conversations d’un utilisateur
    List<Conversation> getConversationsByUser(int idUser) throws SQLException;

    // Récupérer conversation entre 2 utilisateurs
    Conversation getConversationEntre(int idUser1, int idUser2) throws SQLException;

    // Mettre à jour le dernier message
    void updateDernierMessage(int idConversation, String contenu) throws SQLException;

    // Vérifier si conversation existe déjà
    boolean existeConversation(int idUser1, int idUser2) throws SQLException;
}
