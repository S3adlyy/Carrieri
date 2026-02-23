package main;

import entities.Postulation;
import services.PostulationService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class TestPostulationService {
    public static void main(String[] args) {

        PostulationService ps = new PostulationService();

        try {
            // ⚠️ Mets des IDs existants chez toi
            Postulation p = new Postulation(
                    1, // candidat_id
                    6, // offre_id
                    LocalDateTime.now(),
                    "en attente",
                    "Je suis motivé(e) et prêt(e) à contribuer dès le premier jour.",
                    null  // cv_path (null pour les tests, ou mettre un chemin si vous voulez)
            );

            ps.postuler(p);
            System.out.println("✅ Postulation ajoutée !");

            System.out.println("===== POSTULATIONS (ALL) =====");
            for (Postulation x : ps.afficher()) {
                System.out.println(x);
            }

        } catch (SQLException e) {
            System.out.println("❌ Erreur : " + e.getMessage());
        }
    }
}
