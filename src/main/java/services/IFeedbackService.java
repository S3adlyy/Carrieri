package services;

import entities.Feedback;
import java.sql.SQLException;
import java.util.List;

public interface IFeedbackService extends IService<Feedback> {
    // Méthodes spécifiques au feedback
    List<Feedback> getByRenduId(int renduId) throws SQLException;
    List<Feedback> getByNoteRange(int minNote, int maxNote) throws SQLException;
    double getAverageNoteByRendu(int renduId) throws SQLException;
}