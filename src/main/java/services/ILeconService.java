package services;

import entities.Lecon;
import java.util.List;

public interface ILeconService extends IService<Lecon> {
    List<Lecon> getLeconsByModule(int moduleId);
}