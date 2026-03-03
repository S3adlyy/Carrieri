package com.example.guser.controllers.getude;

import com.example.guser.controllers.guser.AppNavController;
import entities.getude.Cours;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.web.WebView;
import services.getude.PaiementService;
import session.SessionContext;
import utils.getude.AlertUtils;

public class PaiementController {

    @FXML private WebView webViewPaiement;
    @FXML private Label lblCours;

    private PaiementService paiementService = new PaiementService();
    private Cours cours;
    int me = SessionContext.getCurrentUser().getId();
    private int candidatId = me;
    private AppNavController parentController;

    public void setCours(Cours cours, int candidatId, AppNavController parent) {
        this.cours = cours;
        this.candidatId = candidatId;
        this.parentController = parent;

        if (lblCours != null) {
            lblCours.setText("Cours: " + cours.getTitre() + " - " + String.format("%.2f €", cours.getPrix()));
        }

        chargerPaiement();
    }

    private void chargerPaiement() {
        try {
            paiementService.chargerPagePaiement(
                    webViewPaiement,
                    (int) cours.getPrix(),
                    cours.getTitre(),
                    cours.getId(),
                    candidatId,
                    this::retourAuCatalogue
            );
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("❌ Erreur", "Impossible de charger la page de paiement.");
            retourAuCatalogue();
        }
    }

    // ✅ MÉTHODE AJOUTÉE pour le bouton Annuler
    @FXML
    private void retourAuCatalogue() {
        System.out.println("🔙 Retour au catalogue");
        if (parentController != null) {
            parentController.onEtudeCatalogue();
        }
    }
}