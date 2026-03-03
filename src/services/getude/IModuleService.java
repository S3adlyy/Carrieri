package services.getude;

import entities.getude.Module;

import java.util.List;

public interface IModuleService extends IService<Module> {
    List<Module> getModulesByCours(int coursId);
}