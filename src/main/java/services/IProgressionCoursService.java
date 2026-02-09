package services;

import java.sql.SQLException;

public interface IProgressionCoursService {

    // Ajouter ou mettre à jour la progression
    void ajouterOuUpdate(int candidatId, int coursId, int progression) throws SQLException;

    // Vérifier si le cours est complété à 100%
    boolean estCoursComplete(int candidatId, int coursId) throws SQLException;
}
