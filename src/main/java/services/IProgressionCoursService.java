package services;

import java.sql.SQLException;

public interface IProgressionCoursService {
    void ajouterOuUpdate(int candidatId, int coursId, int progression) throws SQLException;
    boolean estCoursComplete(int candidatId, int coursId) throws SQLException;
}