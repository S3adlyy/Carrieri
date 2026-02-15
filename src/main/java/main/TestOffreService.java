package main;

import entities.OffreEmploi;
import services.OffreEmploiService;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class TestOffreService {
    public static void main(String[] args) {

        OffreEmploiService service = new OffreEmploiService();

        try {
            OffreEmploi o = new OffreEmploi(
                    "Développeur Java",
                    "Backend + JDBC + JavaFX",
                    2500,
                    "CDI",
                    "Tunis",
                    LocalDateTime.now(),
                    LocalDateTime.now().plusDays(30),
                    "Bac+3",
                    "1 an",
                    "Java, JDBC, MySQL",
                    "Informatique",
                    "Carrieri",
                    "hr@carrieri.tn"
            );

            service.ajouter(o);
            System.out.println("Offre ajoutée !");
            System.out.println(service.afficher());

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
