package com.example.guser.controllers.getude;

public class LangueTest {
    private static LangueTest instance;
    private String langueCourante = "fr"; // Français par défaut

    private LangueTest() {}

    public static LangueTest getInstance() {
        if (instance == null) {
            instance = new LangueTest();
        }
        return instance;
    }

    public String getLangue() {
        return langueCourante;
    }

    public void setLangue(String langue) {
        this.langueCourante = langue;
        System.out.println("🌐 Langue de test changée: " + langue);
    }

    public String getNomLangue() {
        switch(langueCourante) {
            case "fr": return "Français";
            case "en": return "Anglais";
            case "es": return "Espagnol";
            case "de": return "Allemand";
            case "it": return "Italien";
            default: return langueCourante;
        }
    }
}