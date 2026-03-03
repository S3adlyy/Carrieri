package services.goffre;

import entities.goffre.OffreEmploi;

import java.sql.SQLException;
import java.util.List;

public interface IOffreEmploiService extends IService<OffreEmploi> {
    List<OffreEmploi> readCandidates() throws SQLException;

    OffreEmploi findById(int id) throws SQLException;
}
