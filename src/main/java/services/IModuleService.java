package services;

import entities.Module;
import java.util.List;

public interface IModuleService {
    void ajouter(Module module);
    void modifier(Module module);
    void supprimer(int id);
    List<Module> getAll();
    List<Module> getModulesByCours(int coursId);
}