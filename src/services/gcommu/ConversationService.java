package services.gcommu;

import entities.gcommu.Conversation;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationService implements IConversationService {

    private Connection connection;

    public ConversationService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("✅ ConversationService: Connection established");
    }

    // ===== MÉTHODES DE IService =====
    @Override
    public void ajouter(Conversation c) throws SQLException {
        String sql = "INSERT INTO conversation (date_creation, dernier_message, statut, user1_id, user2_id) " +
            "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setTimestamp(1, c.getDateCreation() != null ? Timestamp.valueOf(c.getDateCreation()) : Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(2, c.getDernierMessage());
            ps.setString(3, c.getStatut() != null ? c.getStatut() : "active");
            ps.setInt(4, c.getUser1Id());
            ps.setInt(5, c.getUser2Id());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        c.setId(rs.getInt(1));
                    }
                }
            }

            System.out.println("✅ Conversation ajoutée - ID: " + c.getId());
        }
    }

    @Override
    public void update(Conversation c) throws SQLException {
        String sql = "UPDATE conversation SET dernier_message = ?, statut = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getDernierMessage());
            ps.setString(2, c.getStatut());
            ps.setInt(3, c.getId());

            ps.executeUpdate();
            System.out.println("✅ Conversation mise à jour - ID: " + c.getId());
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM conversation WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Conversation supprimée - ID: " + id);
        }
    }

    @Override
    public List<Conversation> read() throws SQLException {
        String sql = "SELECT * FROM conversation ORDER BY date_creation DESC";
        List<Conversation> conversations = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                conversations.add(mapResultSetToConversation(rs));
            }
        }

        return conversations;
    }

    // ===== MÉTHODES DE IConversationService =====
    @Override
    public List<Conversation> getConversationsByUser(int idUser) throws SQLException {
        String sql = "SELECT * FROM conversation WHERE user1_id = ? OR user2_id = ? ORDER BY date_creation DESC";
        List<Conversation> conversations = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ps.setInt(2, idUser);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
            }
        }

        return conversations;
    }

    @Override
    public Conversation getConversationEntre(int idUser1, int idUser2) throws SQLException {
        String sql = "SELECT * FROM conversation WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser1);
            ps.setInt(2, idUser2);
            ps.setInt(3, idUser2);
            ps.setInt(4, idUser1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToConversation(rs);
                }
            }
        }

        return null;
    }

    @Override
    public void updateDernierMessage(int idConversation, String contenu) throws SQLException {
        String sql = "UPDATE conversation SET dernier_message = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, contenu);
            ps.setInt(2, idConversation);
            ps.executeUpdate();
        }
    }

    @Override
    public boolean existeConversation(int idUser1, int idUser2) throws SQLException {
        String sql = "SELECT COUNT(*) FROM conversation WHERE (user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idUser1);
            ps.setInt(2, idUser2);
            ps.setInt(3, idUser2);
            ps.setInt(4, idUser1);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    // ===== MÉTHODES SUPPLÉMENTAIRES =====
    public List<Conversation> getArchivedConversations(int userId) throws SQLException {
        String sql = "SELECT * FROM conversation WHERE (user1_id = ? OR user2_id = ?) AND statut = 'archived' ORDER BY date_creation DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Conversation> conversations = new ArrayList<>();
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
                return conversations;
            }
        }
    }

    public List<Conversation> getAllConversations(int userId) throws SQLException {
        String sql = "SELECT * FROM conversation WHERE user1_id = ? OR user2_id = ? ORDER BY date_creation DESC";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, userId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Conversation> conversations = new ArrayList<>();
                while (rs.next()) {
                    conversations.add(mapResultSetToConversation(rs));
                }
                return conversations;
            }
        }
    }

    // ===== MÉTHODE UTILITAIRE =====
    private Conversation mapResultSetToConversation(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getInt("id"));

        Timestamp ts = rs.getTimestamp("date_creation");
        if (ts != null) {
            c.setDateCreation(ts.toLocalDateTime());
        }

        c.setDernierMessage(rs.getString("dernier_message"));
        c.setStatut(rs.getString("statut"));
        c.setUser1Id(rs.getInt("user1_id"));
        c.setUser2Id(rs.getInt("user2_id"));

        return c;
    }
}
