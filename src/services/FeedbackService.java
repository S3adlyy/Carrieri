package services;

import entities.Feedback;
import utils.MyDatabase;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService implements IFeedbackService {

    private Connection connection;

    public FeedbackService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("FeedbackService: Connection established");
    }

    @Override
    public void ajouter(Feedback feedback) throws SQLException {
        String req = "INSERT INTO feedback (commentaire, note, created_at, rendu_id) " +
                "VALUES (?, ?, ?, ?)";

        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, feedback.getCommentaire());
        preparedStatement.setInt(2, feedback.getNote());
        preparedStatement.setTimestamp(3, new Timestamp(feedback.getCreatedAt().getTime()));
        preparedStatement.setInt(4, feedback.getRenduId());

        preparedStatement.executeUpdate();
        System.out.println("Feedback added successfully.");
    }

    @Override
    public void update(Feedback feedback) throws SQLException {
        String sql = "UPDATE feedback SET commentaire = ?, note = ?, created_at = ?, rendu_id = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, feedback.getCommentaire());
        preparedStatement.setInt(2, feedback.getNote());
        preparedStatement.setTimestamp(3, new Timestamp(feedback.getCreatedAt().getTime()));
        preparedStatement.setInt(4, feedback.getRenduId());
        preparedStatement.setInt(5, feedback.getId());

        preparedStatement.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM feedback WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        preparedStatement.executeUpdate();
    }

    @Override
    public List<Feedback> read() throws SQLException {
        String sql = "SELECT * FROM feedback ORDER BY created_at DESC";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);
        List<Feedback> feedbacks = new ArrayList<>();

        while (rs.next()) {
            Feedback feedback = new Feedback();
            feedback.setId(rs.getInt("id"));
            feedback.setCommentaire(rs.getString("commentaire"));
            feedback.setNote(rs.getInt("note"));
            feedback.setCreatedAt(new Date(rs.getTimestamp("created_at").getTime()));
            feedback.setRenduId(rs.getInt("rendu_id"));

            feedbacks.add(feedback);
        }
        return feedbacks;
    }

    @Override
    public List<Feedback> getByRenduId(int renduId) throws SQLException {
        String sql = "SELECT * FROM feedback WHERE rendu_id = ? ORDER BY created_at DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, renduId);
        ResultSet rs = preparedStatement.executeQuery();

        List<Feedback> feedbacks = new ArrayList<>();
        while (rs.next()) {
            Feedback feedback = new Feedback();
            feedback.setId(rs.getInt("id"));
            feedback.setCommentaire(rs.getString("commentaire"));
            feedback.setNote(rs.getInt("note"));
            feedback.setCreatedAt(new Date(rs.getTimestamp("created_at").getTime()));
            feedback.setRenduId(rs.getInt("rendu_id"));

            feedbacks.add(feedback);
        }
        return feedbacks;
    }

    @Override
    public List<Feedback> getByNoteRange(int minNote, int maxNote) throws SQLException {
        String sql = "SELECT * FROM feedback WHERE note BETWEEN ? AND ? ORDER BY note DESC";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, minNote);
        preparedStatement.setInt(2, maxNote);
        ResultSet rs = preparedStatement.executeQuery();

        List<Feedback> feedbacks = new ArrayList<>();
        while (rs.next()) {
            Feedback feedback = new Feedback();
            feedback.setId(rs.getInt("id"));
            feedback.setCommentaire(rs.getString("commentaire"));
            feedback.setNote(rs.getInt("note"));
            feedback.setCreatedAt(new Date(rs.getTimestamp("created_at").getTime()));
            feedback.setRenduId(rs.getInt("rendu_id"));

            feedbacks.add(feedback);
        }
        return feedbacks;
    }

    @Override
    public double getAverageNoteByRendu(int renduId) throws SQLException {
        String sql = "SELECT AVG(note) as moyenne FROM feedback WHERE rendu_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, renduId);
        ResultSet rs = preparedStatement.executeQuery();

        if (rs.next()) {
            return rs.getDouble("moyenne");
        }
        return 0.0;
    }
}