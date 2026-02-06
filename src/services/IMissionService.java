package services;

import java.sql.SQLException;
import java.util.List;

public interface IMissionService<T>{
    void ajouter(T w) throws SQLException;
    void supprimer(int w) throws SQLException;
    void update(T w) throws SQLException;
    List<T> read() throws SQLException;
}
