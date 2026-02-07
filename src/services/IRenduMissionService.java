package services;
import entities.RenduMission;
import java.util.List;

public interface IRenduMissionService {
    void ajouterRenduMission(RenduMission renduMission);
    void modifierRenduMission(RenduMission renduMission);
    void supprimerRenduMission(int id);
    List<RenduMission> afficherRenduMissions();
    RenduMission getRenduMissionById(int id);
    List<RenduMission> getRenduMissionsByCandidat(int candidatId);
    List<RenduMission> getRenduMissionsByMission(int missionId);
    RenduMission evaluerCodePython(String code, int missionId, int candidatId);
}
