package main;

import entities.Personne;
import services.PesronneService;
import utils.MyDatabase;

import java.sql.SQLException;

public class Main {


    public static void main(String[] args) {

        PesronneService ps = new PesronneService( );

        try {
            ps.ajouter(new Personne("Bassem","Hcini",23));
            System.out.println("Personne ajouté");
        }
        catch (SQLException e) {
            System.err.println(e.getMessage());
        }

        try {
            ps.update(new Personne(1,  "Billal","nsit", 23));
        } catch (SQLException e) {
            e.printStackTrace();
        }
//

        try {
            System.out.println(ps.read());
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }
}
