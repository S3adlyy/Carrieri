package services;

import entities.Module;
import java.util.List;

public interface IModuleService extends IService<Module> {
    List<Module> getModulesByCours(int coursId);
}