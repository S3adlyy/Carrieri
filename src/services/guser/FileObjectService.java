package services.guser;

import entities.guser.FileObject;
import utils.MyDatabase;
import utils.guser.S3KeyUtil;
import utils.guser.S3StorageService;

import java.io.File;
import java.sql.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class FileObjectService {
    public Connection connection;

    private final String bucket;
    private final String region;

    public FileObjectService(String bucket, String region) {
        this.connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection to Carrieri Database is established without any problem");
        this.bucket = bucket;
        this.region = region;
    }

    public FileObject findLatestByArtifact(int artifactId) throws SQLException {
        String sql = "SELECT * FROM file_object WHERE artifact_id=? ORDER BY uploaded_at DESC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artifactId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public FileObject findById(int id) throws SQLException {
        String sql = "SELECT * FROM file_object WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return map(rs);
            }
        }
    }

    public List<FileObject> listByArtifact(int artifactId) throws SQLException {
        String sql = "SELECT * FROM file_object WHERE artifact_id=? ORDER BY uploaded_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, artifactId);

            try (ResultSet rs = ps.executeQuery()) {
                List<FileObject> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        }
    }

    /**
     * Uploads the file to S3 and creates a file_object row in DB.
     * Note: public_url is NOT NULL in your schema, so we store a stable placeholder "s3://bucket/key".
     */
    public FileObject uploadNewVersion(int candidateId, int trackId, int artifactId, File file) throws Exception {
        String ext = S3KeyUtil.extNoDotOrDefault(file.getName(), "bin");
        String contentType = S3KeyUtil.contentTypeFromExtExtended(ext);
        String key = S3KeyUtil.workspaceArtifactKey(candidateId, trackId, artifactId, ext);

        try (S3StorageService s3 = new S3StorageService(bucket, region)) {
            s3.uploadFile(file, key, contentType);
        }

        String stableUrl = "s3://" + bucket + "/" + key;

        String sql = "INSERT INTO file_object (storage_key, public_url, mime_type, file_size, uploaded_at, artifact_id) " +
                "VALUES (?, ?, ?, ?, NOW(), ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, key);
            ps.setString(2, stableUrl);
            ps.setString(3, contentType);
            ps.setLong(4, file.length());
            ps.setInt(5, artifactId);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated key returned for file_object insert");
                int id = keys.getInt(1);

                FileObject fo = new FileObject();
                fo.setId(id);
                fo.setArtifactId(artifactId);
                fo.setStorageKey(key);
                fo.setPublicUrl(stableUrl);
                fo.setMimeType(contentType);
                fo.setFileSize(file.length());
                return fo;
            }
        }
    }

    public String presignedDownloadUrl(String storageKey, Duration validFor) {
        try (S3StorageService s3 = new S3StorageService(bucket, region)) {
            return s3.presignedGetUrl(storageKey, validFor);
        }
    }

    private FileObject map(ResultSet rs) throws SQLException {
        FileObject f = new FileObject();
        f.setId(rs.getInt("id"));
        f.setArtifactId(rs.getInt("artifact_id"));
        f.setStorageKey(rs.getString("storage_key"));
        f.setPublicUrl(rs.getString("public_url"));
        f.setMimeType(rs.getString("mime_type"));
        f.setFileSize(rs.getLong("file_size"));

        Timestamp up = rs.getTimestamp("uploaded_at");
        if (up != null) f.setUploadedAt(up.toLocalDateTime());
        return f;
    }
}
