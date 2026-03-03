package services.getude;

public interface IProgressionLeconService {
    void marquerTerminee(int candidatId, int leconId);
    boolean isLeconTerminee(int candidatId, int leconId);
    double getProgressionCours(int candidatId, int coursId);
}