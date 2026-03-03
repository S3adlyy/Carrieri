package services.greclam;

import entities.greclam.Reclamation;

import java.sql.SQLException;
import java.util.List;

public interface IReclamationService extends IService<Reclamation> {
    List<Reclamation> getByStatut(String statut) throws SQLException;
    List<Reclamation> getByPriorite(String priorite) throws SQLException;
    List<Reclamation> getByUtilisateur(int utilisateurId) throws SQLException;
    List<Reclamation> getByCategorie(String categorie) throws SQLException;
    void updateStatut(int id, String nouveauStatut) throws SQLException;
    List<Reclamation> searchByKeyword(String keyword) throws SQLException;
    Reclamation getById(int id) throws SQLException; // Ajoutez si nécessaire
}