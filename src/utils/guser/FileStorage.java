package utils.guser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

public final class FileStorage {
    private static final Path BASE_DIR = Paths.get(System.getProperty("user.home"), "carrieri_uploads");

    private FileStorage() {}

    public static String saveImageToLocalFolder(File selectedFile) throws IOException {
        if (selectedFile == null) return null;

        if (!Files.exists(BASE_DIR)) {
            Files.createDirectories(BASE_DIR);
        }

        String name = selectedFile.getName();
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot >= 0) ext = name.substring(dot);

        String safeName = "img_" + System.currentTimeMillis() + ext;
        Path target = BASE_DIR.resolve(safeName);

        Files.copy(selectedFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }
}
