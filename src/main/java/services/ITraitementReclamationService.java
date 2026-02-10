package services;

import entities.TraitementReclamation;
import java.sql.SQLException;
import java.util.List;

public interface ITraitementReclamationService extends IService<TraitementReclamation> {
    // Méthodes spécifiques au traitement des réclamations
    List<TraitementReclamation> getByReclamationId(int reclamationId) throws SQLException;
    List<TraitementReclamation> getByAdminId(int adminId) throws SQLException;
    TraitementReclamation getLatestByReclamationId(int reclamationId) throws SQLException;
    void traiterReclamation(int reclamationId, String reponseAdmin, String statutFinal, int adminId) throws SQLException;
}