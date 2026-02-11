package services;

import entities.Certification;
import java.sql.SQLException;
import java.util.List;

public interface ICertificationService {
    void ajouter(Certification c) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<Certification> read() throws SQLException;
    Certification readByCoursAndCandidat(int coursId, int candidatId) throws SQLException;
    boolean aDejaCertificat(int coursId, int candidatId) throws SQLException;
    void genererCertification(String nomCandidat, String titreCours, String cheminFichier);
    void genererCertification(String nomCandidat, String titreCours, String cheminFichier, java.time.LocalDateTime dateObtention);
    void genererEtEnregistrer(String nomCandidat, String titreCours, String cheminFichier, int candidatId, int coursId) throws SQLException;
    String genererNomFichier(String nomCandidat, String titreCours);
}