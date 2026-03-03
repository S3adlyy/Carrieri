package services.getude;

import entities.getude.Lecon;

import java.util.List;

public interface ILeconService extends IService<Lecon> {
    List<Lecon> getLeconsByModule(int moduleId);
}