package services;

import entities.Lecon;
import java.util.List;

public interface ILeconService {
    void ajouter(Lecon l);
    void modifier(Lecon l);
    void supprimer(int id);
    List<Lecon> getAll();
    List<Lecon> getLeconsByModule(int moduleId);
}