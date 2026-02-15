package services;

import entities.Postulation;

import java.sql.SQLException;
import java.util.List;

public interface IPostulationService extends IService<Postulation> {

    void postuler(Postulation p) throws SQLException;

    List<Postulation> afficherParOffre(int offreId) throws SQLException;

    List<Postulation> afficherParCandidat(int candidatId) throws SQLException;

    void changerStatut(int idPostulation, String nouveauStatut) throws SQLException;
}
