package services;

import entities.Cours;
import java.sql.SQLException;
import java.util.List;

public interface ICoursService {
    void ajouter(Cours cours) throws SQLException;
    void update(Cours cours) throws SQLException;
    void supprimer(int id) throws SQLException;
    List<Cours> readAll() throws SQLException;
    List<Cours> readByAdmin(int userId) throws SQLException;
}