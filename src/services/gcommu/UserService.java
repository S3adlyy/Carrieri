package services.gcommu;

import entities.gcommu.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private Connection connection;

    public UserService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(User user) throws SQLException {
        String sql = "INSERT INTO user (first_name, last_name, email, is_active, created_at, type) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());

            // Gérer l'email - mettre une chaîne vide si null
            String email = user.getEmail();
            ps.setString(3, email != null ? email : "");

            ps.setBoolean(4, user.isActive());
            ps.setTimestamp(5, user.getCreatedAt() != null ?
                Timestamp.valueOf(user.getCreatedAt()) : Timestamp.valueOf(java.time.LocalDateTime.now()));
            ps.setString(6, user.getType() != null ? user.getType() : "contact");

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        user.setId(rs.getInt(1));
                    }
                }
            }

            System.out.println("✅ Contact ajouté: " + user.getUsername() + " (ID: " + user.getId() + ")");
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT * FROM user ORDER BY first_name ASC";
        List<User> users = new ArrayList<>();

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }

        return users;
    }

    public User getUserById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }

        return null;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));

        String firstName = rs.getString("first_name");
        String lastName = rs.getString("last_name");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : ""));

        user.setEmail(rs.getString("email"));
        user.setOnline(true);
        user.setProfilePic(rs.getString("profile_pic"));
        user.setHeadline(rs.getString("headline"));
        user.setLocation(rs.getString("location"));
        user.setType(rs.getString("type"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        return user;
    }
    public void update(User user) throws SQLException {
        String sql = "UPDATE user SET first_name = ?, last_name = ?, username = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getFirstName());
            ps.setString(2, user.getLastName());
            ps.setString(3, user.getUsername());
            ps.setInt(4, user.getId());

            ps.executeUpdate();
            System.out.println("✅ Contact mis à jour: " + user.getUsername());
        }
    }
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println("✅ Contact supprimé - ID: " + id);
        }
    }
}
