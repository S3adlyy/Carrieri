package utils.guser;

import java.util.UUID;

public class S3KeyUtil {

    public static String profilePicKey(String extNoDot) {
        return "users/profile-pics/" + UUID.randomUUID() + "." + extNoDot;
    }

    public static String orgLogoKey(String extNoDot) {
        return "org/logos/" + UUID.randomUUID() + "." + extNoDot;
    }

    public static String extNoDotOrDefault(String filename, String def) {
        if (filename == null) return def;
        int i = filename.lastIndexOf('.');
        if (i < 0 || i == filename.length() - 1) return def;
        return filename.substring(i + 1).toLowerCase();
    }

    public static String contentTypeFromExt(String extNoDot) {
        return switch (extNoDot.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
    public static String workspaceArtifactKey(int candidateId, int trackId, int artifactId, String extNoDot) {
        return "workspace/candidate-" + candidateId +
                "/track-" + trackId +
                "/artifact-" + artifactId +
                "/" + UUID.randomUUID() + "." + extNoDot;
    }

    public static String workspaceSnapshotTextKey(int candidateId, int trackId, int artifactId, int snapshotId, String extNoDot) {
        return "workspace/candidate-" + candidateId +
                "/track-" + trackId +
                "/artifact-" + artifactId +
                "/snapshots/" + snapshotId +
                "/" + UUID.randomUUID() + "." + extNoDot;
    }

    public static String contentTypeFromExtExtended(String extNoDot) {
        return switch (extNoDot.toLowerCase()) {
            case "zip" -> "application/zip";
            case "pdf" -> "application/pdf";
            case "txt", "md" -> "text/plain";
            case "json" -> "application/json";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
