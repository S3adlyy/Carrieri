package services;

import entities.Certification;
import java.sql.SQLException;
import java.util.List;

public interface ICertificationService {
    void ajouter(Certification c) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<Certification> read() throws SQLException;
    Certification readByCoursAndCandidat(int coursId, int candidatId) throws SQLException;
}
