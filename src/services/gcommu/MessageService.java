package services.gcommu;

import entities.gcommu.Message;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements IService<Message> {

    private Connection connection;

    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("✅ MessageService: Connection established");
    }

    @Override
    public void ajouter(Message message) throws SQLException {
        String sql = "INSERT INTO message (contenu, image_data, file_data, file_name, file_size, file_type, date_envoi, statut, type, conversation_id, expediteur_id, destinataire_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getContenu());
            ps.setString(2, message.getImageData());
            ps.setString(3, message.getFileData());
            ps.setString(4, message.getFileName());
            ps.setLong(5, message.getFileSize());
            ps.setString(6, message.getFileType());
            ps.setTimestamp(7, message.getDateEnvoi() != null ? Timestamp.valueOf(message.getDateEnvoi()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(8, message.getStatut() != null ? message.getStatut() : "sent");
            ps.setString(9, message.getType() != null ? message.getType() : "text");
            ps.setInt(10, message.getConversationId());
            ps.setInt(11, message.getExpediteurId());
            ps.setInt(12, message.getDestinataireId());

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setId(rs.getInt(1));
                    }
                }
            }

            System.out.println("✅ Message ajouté avec succès - ID: " + message.getId() + " Type: " + message.getType());
        }
    }

    @Override
    public void update(Message message) throws SQLException {
        String sql = "UPDATE message SET contenu = ?, image_data = ?, file_data = ?, file_name = ?, file_size = ?, file_type = ?, date_modification = ?, statut = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, message.getContenu());
            ps.setString(2, message.getImageData());
            ps.setString(3, message.getFileData());
            ps.setString(4, message.getFileName());
            ps.setLong(5, message.getFileSize());
            ps.setString(6, message.getFileType());
            ps.setTimestamp(7, Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(8, message.getStatut());
            ps.setInt(9, message.getId());

            ps.executeUpdate();
            System.out.println("✅ Message mis à jour - ID: " + message.getId());
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM message WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Message supprimé - ID: " + id);
        }
    }

    @Override
    public List<Message> read() throws SQLException {
        String sql = "SELECT * FROM message ORDER BY date_envoi DESC";
        List<Message> messages = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                messages.add(mapResultSetToMessage(rs));
            }
        }

        return messages;
    }

    public List<Message> getMessagesByConversation(int idConversation) throws SQLException {
        String sql = "SELECT * FROM message WHERE conversation_id = ? ORDER BY date_envoi ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idConversation);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }

        return messages;
    }

    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message m = new Message();
        m.setId(rs.getInt("id"));
        m.setContenu(rs.getString("contenu"));

        // Données d'image
        m.setImageData(rs.getString("image_data"));

        // Données de fichier
        m.setFileData(rs.getString("file_data"));
        m.setFileName(rs.getString("file_name"));
        m.setFileSize(rs.getLong("file_size"));
        m.setFileType(rs.getString("file_type"));

        Timestamp ts = rs.getTimestamp("date_envoi");
        if (ts != null) {
            m.setDateEnvoi(ts.toLocalDateTime());
        }

        Timestamp tsModif = rs.getTimestamp("date_modification");
        if (tsModif != null) {
            m.setDateModification(tsModif.toLocalDateTime());
        }

        m.setStatut(rs.getString("statut"));
        m.setType(rs.getString("type"));
        m.setConversationId(rs.getInt("conversation_id"));
        m.setExpediteurId(rs.getInt("expediteur_id"));
        m.setDestinataireId(rs.getInt("destinataire_id"));

        return m;
    }

    public int countMessagesByConversation(int conversationId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM message WHERE conversation_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<Message> getTextMessagesByConversation(int conversationId) throws SQLException {
        String sql = "SELECT * FROM message WHERE conversation_id = ? AND type = 'text' ORDER BY date_envoi ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }

        return messages;
    }

    public List<Message> getImageMessagesByConversation(int conversationId) throws SQLException {
        String sql = "SELECT * FROM message WHERE conversation_id = ? AND type = 'image' ORDER BY date_envoi ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }

        return messages;
    }

    public List<Message> getFileMessagesByConversation(int conversationId) throws SQLException {
        String sql = "SELECT * FROM message WHERE conversation_id = ? AND type = 'file' ORDER BY date_envoi ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }

        return messages;
    }

    public List<Message> getMessagesByType(int conversationId, String type) throws SQLException {
        String sql = "SELECT * FROM message WHERE conversation_id = ? AND type = ? ORDER BY date_envoi ASC";
        List<Message> messages = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, conversationId);
            ps.setString(2, type);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        }

        return messages;
    }
}
