package services;

import entities.OffreEmploi;

import java.sql.SQLException;

public interface IOffreEmploiService extends IService<OffreEmploi> {
    OffreEmploi findById(int id) throws SQLException;
}
