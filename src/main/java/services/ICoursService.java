package services;

import entities.Cours;
import java.sql.SQLException;
import java.util.List;

public interface ICoursService extends IService<Cours> {
    void update(Cours cours) throws SQLException;
    List<Cours> readAll() throws SQLException;
    List<Cours> readByAdmin(int userId) throws SQLException;
}