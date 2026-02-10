package services;

import entities.Artifact;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ArtifactService {
    public Connection connection;

    public ArtifactService() {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
    }

    public Artifact create(int trackId,
                           String name,
                           String description,
                           String type,
                           String language,
                           String textContent) throws SQLException {

        String sql = "INSERT INTO artifact (artifact_name, artifact_description, artifact_type, language, test_content, created_at, track_id, deleted_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), ?, NULL)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description == null ? "" : description);
            ps.setString(3, type);
            ps.setString(4, language);
            ps.setString(5, textContent); // may be null
            ps.setInt(6, trackId);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for artifact insert");
                int id = keys.getInt(1);

                Artifact a = new Artifact();
                a.setId(id);
                a.setTrackId(trackId);
                a.setArtifactName(name);
                a.setArtifactDescription(description == null ? "" : description);
                a.setArtifactType(type);
                a.setLanguage(language);
                a.setTextContent(textContent);
                a.setCreatedAt(java.time.LocalDateTime.now());
                return a;
            }
        }
    }

    public void updateTextContent(int artifactId, String textContent) throws SQLException {
        String sql = "UPDATE artifact SET test_content=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, textContent);
            ps.setInt(2, artifactId);
            ps.executeUpdate();
        }
    }
    public void rename(int artifactId, String newName) throws SQLException {
        String sql = "UPDATE artifact SET artifact_name=? WHERE id=? AND deleted_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, artifactId);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Artifact not found or deleted.");
        }
    }



    public List<Artifact> listActiveByTrack(int trackId) throws SQLException {
        String sql = "SELECT * FROM artifact WHERE track_id=? AND deleted_at IS NULL ORDER BY created_at ASC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, trackId);

            try (ResultSet rs = ps.executeQuery()) {
                List<Artifact> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    public void softDelete(int artifactId) throws SQLException {
        String sql = "UPDATE artifact SET deleted_at=NOW() WHERE id=? AND deleted_at IS NULL";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artifactId);
            int updated = ps.executeUpdate();
            if (updated == 0) throw new SQLException("Artifact not found or already deleted.");
        }
    }

    private Artifact map(ResultSet rs) throws SQLException {
        Artifact a = new Artifact();
        a.setId(rs.getInt("id"));
        a.setTrackId(rs.getInt("track_id"));

        a.setArtifactName(rs.getString("artifact_name"));
        a.setArtifactDescription(rs.getString("artifact_description"));
        a.setArtifactType(rs.getString("artifact_type"));
        a.setLanguage(rs.getString("language"));

        // SQL column is test_content (typo in schema), map to your Java field textContent
        a.setTextContent(rs.getString("test_content"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());

        Timestamp del = rs.getTimestamp("deleted_at");
        if (del != null) a.setDeletedAt(del.toLocalDateTime());

        return a;
    }
}
