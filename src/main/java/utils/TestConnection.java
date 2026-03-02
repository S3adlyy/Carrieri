package utils;

public class TestConnection {
    public static void main(String[] args) {
        try {
            MyDatabase db = MyDatabase.getInstance();
            System.out.println("✅ Connexion établie avec succès!");
            System.out.println("📊 Base de données: carrieri");
        } catch (Exception e) {
            System.err.println("❌ Échec de connexion: " + e.getMessage());
        }
    }
}
