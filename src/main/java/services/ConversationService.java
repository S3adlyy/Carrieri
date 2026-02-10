package services;

import entities.Conversation;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConversationService implements IService<Conversation> {

    private Connection connection;

    public ConversationService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection established");
    }

    @Override
    public void ajouter(Conversation c) throws SQLException {

        String sql = "insert into conversation (date_creation, dernier_message, statut, user1_id, user2_id) " +
            "values (?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(sql);

        // ✅ Conversion LocalDateTime → Timestamp
        ps.setTimestamp(1, Timestamp.valueOf(c.getDateCreation()));
        ps.setString(2, c.getDernierMessage());
        ps.setString(3, c.getStatut());
        ps.setInt(4, c.getUser1Id());
        ps.setInt(5, c.getUser2Id());

        ps.executeUpdate();

        System.out.println("Conversation ajoutée");
    }

    @Override
    public void update(Conversation c) throws SQLException {

        String sql = "update conversation set dernier_message = ?, statut = ? where id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);

        ps.setString(1, c.getDernierMessage());
        ps.setString(2, c.getStatut());
        ps.setInt(3, c.getId());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {

        String sql = "delete from conversation where id = ?";

        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);

        ps.executeUpdate();
    }

    @Override
    public List<Conversation> read() throws SQLException {

        String sql = "select * from conversation";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(sql);

        List<Conversation> conversations = new ArrayList<>();

        while (rs.next()) {

            Conversation c = new Conversation();

            c.setId(rs.getInt("id"));

            // ✅ CORRECTION PRINCIPALE ICI
            Timestamp ts = rs.getTimestamp("date_creation");
            if (ts != null) {
                c.setDateCreation(ts.toLocalDateTime());
            }

            c.setDernierMessage(rs.getString("dernier_message"));
            c.setStatut(rs.getString("statut"));
            c.setUser1Id(rs.getInt("user1_id"));
            c.setUser2Id(rs.getInt("user2_id"));

            conversations.add(c);
        }

        return conversations;
    }
}
