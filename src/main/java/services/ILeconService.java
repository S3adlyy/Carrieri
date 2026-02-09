package services;

import entities.Lecon;
import java.util.List;

public interface ILeconService {
    void ajouter(Lecon lecon);

    void modifier(Lecon lecon);

    void supprimer(int id);

    List<Lecon> getAll();

    List<Lecon> getLeconsByModule(int moduleId);
}
