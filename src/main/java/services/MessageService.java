package services;

import entities.Message;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageService implements IService<Message> {

    private Connection connection;

    public MessageService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection established");
    }

    @Override
    public void ajouter(Message message) throws SQLException {

        String sql = "insert into message " +
            "(contenu, date_envoi, statut, type, conversation_id, expediteur_id, destinataire_id) " +
            "values (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, message.getContenu());

        // ✅ CORRECTION ICI
        ps.setTimestamp(2, Timestamp.valueOf(message.getDateEnvoi()));

        ps.setString(3, message.getStatut());
        ps.setString(4, message.getType());
        ps.setInt(5, message.getConversationId());
        ps.setInt(6, message.getExpediteurId());
        ps.setInt(7, message.getDestinataireId());

        ps.executeUpdate();
        System.out.println("Message ajouté avec succès");
    }

    @Override
    public void update(Message message) throws SQLException {

        String sql = "update message set contenu = ?, date_modification = ?, statut = ? where id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, message.getContenu());

        // ✅ Date modification = maintenant
        ps.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.now()));

        ps.setString(3, message.getStatut());
        ps.setInt(4, message.getId());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "delete from message where id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public List<Message> read() throws SQLException {

        String sql = "select * from message";
        Statement st = connection.createStatement();

        ResultSet rs = st.executeQuery(sql);

        List<Message> messages = new ArrayList<>();

        while (rs.next()) {

            Message m = new Message();

            m.setId(rs.getInt("id"));
            m.setContenu(rs.getString("contenu"));

            // ✅ CORRECTION PRINCIPALE
            Timestamp ts = rs.getTimestamp("date_envoi");
            if (ts != null) {
                m.setDateEnvoi(ts.toLocalDateTime());
            }

            m.setStatut(rs.getString("statut"));
            m.setType(rs.getString("type"));
            m.setConversationId(rs.getInt("conversation_id"));
            m.setExpediteurId(rs.getInt("expediteur_id"));
            m.setDestinataireId(rs.getInt("destinataire_id"));

            messages.add(m);
        }

        return messages;
    }
}
