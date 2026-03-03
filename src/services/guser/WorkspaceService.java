package services.guser;

import entities.guser.Workspace;
import utils.MyDatabase;

import java.sql.*;

public class WorkspaceService {
    public Connection connection;

    public WorkspaceService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
    }

    public Workspace findByCandidateId(int candidateId) throws SQLException {
        String sql = "SELECT id, description, created_at, candidat_id FROM workspace WHERE candidat_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, candidateId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;

                Workspace w = new Workspace();
                w.setId(rs.getInt("id"));
                w.setDescription(rs.getString("description"));
                w.setCandidatId(rs.getInt("candidat_id"));
                Timestamp ts = rs.getTimestamp("created_at");
                if (ts != null) w.setCreatedAt(ts.toLocalDateTime());
                return w;
            }
        }
    }

    public Workspace getOrCreateByCandidateId(int candidateId) throws SQLException {
        Workspace existing = findByCandidateId(candidateId);
        if (existing != null) return existing;

        String sql = "INSERT INTO workspace (description, created_at, candidat_id) VALUES (?, NOW(), ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, "");
            ps.setInt(2, candidateId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for workspace insert");
                int id = keys.getInt(1);

                Workspace w = new Workspace();
                w.setId(id);
                w.setCandidatId(candidateId);
                w.setDescription("");
                return w;
            }
        }
    }
}
