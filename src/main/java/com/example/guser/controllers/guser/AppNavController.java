package com.example.guser.controllers.guser;

import com.example.guser.AppRouter;
import com.example.guser.controllers.goffre.OffreStatsPopupController;
import com.example.guser.controllers.goffre.PostulationsListController;
import com.example.guser.controllers.goffre.PostulerPopupController;
import com.example.guser.controllers.goffre.QRCodeController;
import com.example.guser.controllers.grecru.EntretienCreateController;
import com.example.guser.controllers.grecru.RenduAddController;
import entities.grecru.RenduMission;
import entities.guser.User;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import session.ProfileViewContext;
import session.SessionContext;
import utils.guser.AlertUtils;
import utils.guser.S3StorageService;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;

public class AppNavController {

    private static final String S3_BUCKET = "carrieri-storage-dev-islem";
    private static final String S3_REGION = "eu-west-3";

    public enum Route {
        //GUSER
        PROFILE, PEOPLE, FEED, JOBS, CONNECT, ADMIN,
        //GRECRU
        ADD_MISSION, LIST_MISSION, LIST_RENDU, VIEW_CALENDAR,
        STATS, ENTRETIEN,
        // GETUDE
        ETUDE_CATALOGUE, ETUDE_MESCOURS, ETUDE_CERTIFICATS, ETUDE_RECO,
        ETUDE_COURS, ETUDE_MODULES, ETUDE_LECONS, ETUDE_CHATBOT,

        // GOFFRE
        OFFRES_TABLE, OFFRE_ADD, OFFRES_LIST, POSTULATIONS_CANDIDATS, ADMIN_DASHBOARD,

        //gcommu
        MESSAGES, CONTACTS
    }


    // ====== Shell host (NEW) ======
    @FXML private StackPane contentPane;   // MUST exist in your updated navbar.fxml

    // ====== Buttons (as you had) ======
    //==========USER==============
    @FXML private Button btnFeed;
    @FXML private Button btnJobs;
    @FXML private Button btnConnect;
    @FXML private Button btnUserProfile;
    @FXML private Button btnAdmin;
    @FXML private Button logoutBtn;
    //=============RECRUTEMENT============

    @FXML private Button btnAddMission;
    @FXML private Button btnListMission;
    @FXML private Button btnListRendu;
    @FXML private Button btnViewCalendar;


    //========ETUDE=========
    @FXML private Label brandSubtext;

    @FXML private Button btnCatalogue;
    @FXML private Button btnMesCours;
    @FXML private Button btnCertificats;
    @FXML private Button btnRecommandation;
    @FXML private Button btnEtudeChatbot;
    @FXML private Button btnTheme;

    @FXML private Button btnCours;
    @FXML private Button btnModules;
    @FXML private Button btnLecons;
    @FXML private Button btnThemeRecruiter;


    // GETUDE state
    private int etudeCurrentCoursId = 0;
    private String etudeCurrentCoursTitre = "";
    private boolean etudeHasCoursActif = false;

    private int etudeCurrentModuleId = 0;
    private String etudeCurrentModuleTitre = "";
    private boolean etudeHasModuleActif = false;

    private Node etudeLastMainViewBeforeReco = null;

    // Candidate context
    private entities.getude.Cours etudeCoursActif = null;
    private int etudeCandidatId = SessionContext.getCurrentUser().getId();

    // ====== GOFFRE buttons (NEW) ======
    @FXML private Button btnOffresTable;
    @FXML private Button btnOffreAdd;
    @FXML private Button btnOffresList;
    @FXML private Button btnPostulationsCandidats;
    @FXML private Button btnAdminDashboard;

    //=============GRECLAM SELIM================
    // ====== GRECLAM buttons ======
    @FXML private VBox reclamNavBox;
    @FXML private Button btnReclamations;
    @FXML private Button btnFeedbacks;
    @FXML private Button btnTraitements;
    @FXML private Button btnAjoutReclamation;
    @FXML private Button btnAjoutFeedback;

    //=========G communication youssef=============
    @FXML private Button btnMessages;
    @FXML private Button btnContacts;



    /// ========
    @FXML private Circle navAvatar;
    @FXML private Label navNameLabel;
    @FXML private Label navRoleLabel;
    @FXML private ImageView brandLogo;
    @FXML private VBox sidebar;

    private static AppNavController instance;

    private Button activeButton;
    private static final double COLLAPSED_W = 70;
    private static final double EXPANDED_W = 250;

    @FXML private VBox candidateNavBox;
    @FXML private VBox recruiterNavBox;
    @FXML private VBox adminNavBox;

    private Timeline widthAnim;
    private PauseTransition hoverDebounce;

    @FXML
    public void initialize() {
        instance = this;

        loadLogoSafe("/com/example/guser/images/logo.png", "/images/logo.png");
        hydrateUserSection();

        setupAnimations();
        animateSidebarTo(COLLAPSED_W);

        // Default view inside shell
        if (!SessionContext.isLoggedIn()) {
            showLogin();     // <-- load login into contentPane
            return;          // stop here (don’t navigate to profile/admin)
        }
        onProfile();

    }
    public void showLogin() {
        show("/com/example/guser/guser/login.fxml", "Carrieri • Sign in");
    }
    private boolean requireLogin() {//nbloki ay access l navbar ken mech logged in
        if (SessionContext.isLoggedIn()) return true;
        showLogin();
        return false;
    }


    public static AppNavController getInstance() {
        return instance;
    }

    // =========================
    // Core shell navigation API
    // =========================
    private void loadInShell(String fxml, String title) {
        try {
            URL url = getClass().getResource(fxml);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxml);

            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            contentPane.getChildren().setAll(view);

            // Optional: if you still want window title updates
            if (contentPane.getScene() != null && contentPane.getScene().getWindow() instanceof javafx.stage.Stage st) {
                st.setTitle(title);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", e.getMessage());
        }
    }

    private <T> T loadInShellGetController(String fxml, String title) {
        try {
            System.out.println("Loading FXML: " + fxml);
            URL url = getClass().getResource(fxml);
            System.out.println("URL found: " + url);

            if (url == null) {
                System.err.println("FXML NOT FOUND: " + fxml);
                System.err.println("Current class: " + getClass());
                System.err.println("Class loader: " + getClass().getClassLoader());
                throw new IllegalArgumentException("FXML not found: " + fxml);
            }

            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            System.out.println("View loaded successfully");

            contentPane.getChildren().setAll(view);

            if (contentPane.getScene() != null && contentPane.getScene().getWindow() instanceof javafx.stage.Stage st) {
                st.setTitle(title);
            }

            T controller = loader.getController();
            System.out.println("Controller obtained: " + controller);
            return controller;
        } catch (Exception e) {
            System.err.println("ERROR loading FXML: " + fxml);
            e.printStackTrace();
            AlertUtils.showError("Navigation Error",
                    "Failed to load: " + fxml + "\nError: " + e.getMessage());
            return null;
        }
    }
    private void loadInShellAnimated(String fxml, String title) {
        try {
            URL url = getClass().getResource(fxml);
            if (url == null) throw new IllegalArgumentException("FXML not found: " + fxml);

            FXMLLoader loader = new FXMLLoader(url);
            Node view = loader.load();
            animateContentChange(view);

            if (contentPane.getScene() != null && contentPane.getScene().getWindow() instanceof javafx.stage.Stage st) {
                st.setTitle(title);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", e.getMessage());
        }
    }

    public void show(String fxml, String title) {
        loadInShell(fxml, title);
    }

    public <T> T showGetController(String fxml, String title) {
        return loadInShellGetController(fxml, title);
    }


    // ========= ROUTES (updated to shell) ==========
    @FXML
    private void onProfile() {
        if (!requireLogin()) return;
        if (!SessionContext.isLoggedIn()) return;

        int meId = SessionContext.getCurrentUser().getId();
        ProfileViewContext.viewUser(meId);

        loadInShell("/com/example/guser/guser/profile.fxml", "Carrieri • Profile");
        setActive(btnUserProfile, Route.PROFILE);
    }

    @FXML
    private void onLogout() {
        SessionContext.setCurrentUser(null);
        ProfileViewContext.viewUser(null);
        AppRouter.setRoot("/com/example/guser/guser/login.fxml", "Carrieri • Sign in");
        //loadInShell("/com/example/guser/guser/login.fxml", "Carrieri • Sign in");

        clearActive();
        hydrateUserSection();
    }

    @FXML
    private void onJobs() {
        if (!requireLogin()) return;
        setActive(btnJobs, Route.JOBS);
        //loadInShell("/com/example/guser/guser/jobs.fxml", "Carrieri • Jobs");
    }

    @FXML
    private void onFeed() {
        if (!requireLogin()) return;
        setActive(btnFeed, Route.FEED);
        //loadInShell("/com/example/guser/guser/feed.fxml", "Carrieri • Feed");
    }

    @FXML
    private void onAdmin() {
        if (!requireLogin()) return;
        setActive(btnAdmin, Route.ADMIN);
        // loadInShell("/com/example/guser/guser/admin.fxml", "Carrieri • Admin");
        AlertUtils.showInfo("TODO", "Wire Admin route to a real FXML.");
    }

    @FXML
    private void onConnect() {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/guser/connect.fxml", "Carrieri • Connect");
        setActive(btnConnect, Route.CONNECT);
    }

    // ============== Recrutement ================
    @FXML
    public void onMissionAdd(ActionEvent actionEvent) {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/mission-add.fxml", "Carrieri • Add Mission");
        setActive(btnAddMission, Route.ADD_MISSION);
    }

    @FXML
    public void onMissionShow(ActionEvent actionEvent) {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/mission-list.fxml", "Carrieri • Missions");
        setActive(btnListMission, Route.LIST_MISSION);
    }

    @FXML
    public void onRenduShow(ActionEvent actionEvent) {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/rendu-list.fxml", "Carrieri • Rendus");
        setActive(btnListRendu, Route.LIST_RENDU);
    }

    @FXML
    public void onCalendarShow(ActionEvent actionEvent) {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/calendar-view.fxml", "Carrieri • Calendar");
        setActive(btnViewCalendar, Route.VIEW_CALENDAR);
    }

    @FXML
    public void showStatistics() {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/rendu-stats.fxml", "Carrieri • Statistics");
        setActive(null, Route.STATS);
    }

    @FXML
    public void showEntretienView() {
        if (!requireLogin()) return;
        loadInShell("/com/example/guser/grecru/entretien-create.fxml", "Carrieri • Entretien");
        setActive(null, Route.ENTRETIEN);
    }

    // === Parameterized navigation (fixed) ===
    public void showRenduAddWithMissionId(int missionId) {
        RenduAddController controller =
                loadInShellGetController("/com/example/guser/grecru/rendu-add.fxml", "Carrieri • Add Rendu");
        if (controller != null) controller.setMissionId(missionId);
        setActive(btnListRendu, Route.LIST_RENDU);
    }

    public void showScheduleInterview(RenduMission rendu) {
        System.out.println("rendu in appnav: " + rendu);

        EntretienCreateController controller =
                loadInShellGetController("/com/example/guser/grecru/entretien-create.fxml", "Carrieri • Schedule Interview");

        if (controller != null) {
            System.out.println("controller not null - setting rendu...");
            controller.setRenduMission(rendu);
            System.out.println("Rendu set successfully");
        } else {
            System.err.println("ERROR: controller is null!");
            EntretienCreateController.setCurrentRendu(rendu);
        }

        setActive(null, Route.ENTRETIEN);
    }

    //===================ETUDE=========================
    //navigation
    @FXML
    public void onEtudeCatalogue() {
        if (!requireLogin()) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/CoursCandidat.fxml"));
            Node view = loader.load();

            // refresh purchases like original
            com.example.guser.controllers.getude.CoursCandidatController c = loader.getController();
            if (c != null) c.rafraichirAchats();

            animateContentChange(view);
            setActive(btnCatalogue, Route.ETUDE_CATALOGUE);
        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le catalogue.");
        }
    }

    @FXML
    public void onEtudeMesCours() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/getude/MesCoursCandidat.fxml", "Carrieri • Mes cours");
        setActive(btnMesCours, Route.ETUDE_MESCOURS);
    }

    @FXML
    public void onEtudeCertificats() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/getude/CertificatsCandidat.fxml", "Carrieri • Certificats");
        setActive(btnCertificats, Route.ETUDE_CERTIFICATS);
    }

    @FXML
    public void onEtudeRecommandations() {
        if (!requireLogin()) return;
        showEtudeRecommandation(); // see below
    }

    @FXML
    public void onEtudeChatbot() {
        if (!requireLogin()) return;
        toggleEtudeChatbotOverlay();
        setActive(btnEtudeChatbot, Route.ETUDE_CHATBOT);
    }
    @FXML
    private void onEtudeCours() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/getude/Cours.fxml", "Carrieri • Cours");
        setActive(btnCours, Route.ETUDE_COURS);
    }

    @FXML
    private void onEtudeModules() {
        if (!requireLogin()) return;

        if (etudeHasCoursActif) {
            showEtudeModulesViewWithCours(etudeCurrentCoursId, etudeCurrentCoursTitre);
            return;
        }
        utils.getude.AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
        onEtudeCours();
    }

    @FXML
    private void onEtudeLecons() {
        if (!requireLogin()) return;

        if (etudeHasModuleActif) {
            // validate module exists like original (optional)
            try {
                services.getude.ModuleService ms = new services.getude.ModuleService();
                entities.getude.Module m = ms.getModuleById(etudeCurrentModuleId);
                if (m == null) {
                    resetEtudeModule();
                    onEtudeModules();
                    return;
                }
            } catch (Exception ignored) {}

            showEtudeLeconsViewWithModule(etudeCurrentModuleId, etudeCurrentModuleTitre);
            return;
        }

        if (etudeHasCoursActif) {
            utils.getude.AlertUtils.showNoSelectionWarning("module", "afficher ses leçons");
            onEtudeModules();
        } else {
            utils.getude.AlertUtils.showNoSelectionWarning("cours", "afficher ses modules");
            onEtudeCours();
        }
    }
    public void showEtudeModulesViewWithCours(int coursId, String coursTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/Module.fxml"));
            Node view = loader.load();

            com.example.guser.controllers.getude.ModuleController controller = loader.getController();
            if (controller != null) controller.setCoursInfo(coursId, coursTitre);

            animateContentChange(view);
            setActive(btnModules, Route.ETUDE_MODULES);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", e.getMessage());
        }
    }

    public void showEtudeLeconsViewWithModule(int moduleId, String moduleTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/Lecon.fxml"));
            Node view = loader.load();

            com.example.guser.controllers.getude.LeconController controller = loader.getController();
            if (controller != null) controller.setModuleInfo(moduleId, moduleTitre);

            animateContentChange(view);
            setActive(btnLecons, Route.ETUDE_LECONS);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtils.showError("Navigation Error", e.getMessage());
        }
    }
    public void etudeOpenCours(entities.getude.Cours cours) {
        this.etudeCoursActif = cours;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/CoursPlayer.fxml"));
            Node view = loader.load();

            com.example.guser.controllers.getude.CoursPlayerController c = loader.getController();
            if (c != null) {
                c.setCours(cours);
                c.setCandidatId(etudeCandidatId);
            }

            animateContentChange(view);
        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le cours:\n\n" + e.getMessage());
        }
    }

    public void etudeOpenQuiz(int moduleId, String moduleTitre) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/QuizModulePlayer.fxml"));
            Node view = loader.load();

            com.example.guser.controllers.getude.QuizModulePlayerController c = loader.getController();
            if (c != null) c.setModuleId(moduleId, etudeCandidatId);

            animateContentChange(view);

            utils.getude.AlertUtils.showInfo("📝 Quiz du module",
                    "Vous allez passer le quiz du module \"" + moduleTitre + "\".\n\n" +
                            "Répondez aux 5 questions pour valider ce module.");
        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le quiz:\n\n" + e.getMessage());
        }
    }

    public void etudeOpenTestFinal(int coursId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/TestFinalPlayer.fxml"));
            Node view = loader.load();

            com.example.guser.controllers.getude.TestFinalPlayerController c = loader.getController();
            if (c != null) c.setCoursId(coursId, etudeCandidatId);

            animateContentChange(view);

            utils.getude.AlertUtils.showInfo("🎯 Test final",
                    "Vous allez passer le test final du cours.\n\n" +
                            "15 questions pour valider l'ensemble du cours. Bonne chance !");
        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir le test final:\n\n" + e.getMessage());
        }
    }

    public void etudeRetourAuCours() {
        if (etudeCoursActif != null) etudeOpenCours(etudeCoursActif);
        else onEtudeCatalogue();
    }
    private void toggleEtudeChatbotOverlay() {
        try {
            Node existing = contentPane.lookup("#chatbotOverlay");
            if (existing != null) {
                contentPane.getChildren().remove(existing);
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/Chatbot.fxml"));
            Node chatbotView = loader.load();
            chatbotView.setId("chatbotOverlay");

            com.example.guser.controllers.getude.ChatbotController c = loader.getController();
            if (c != null) {
                c.setContentPane(contentPane);
                if (etudeCoursActif != null) c.setContexte(etudeCoursActif.getTitre(), "", "");
            }

            StackPane.setAlignment(chatbotView, javafx.geometry.Pos.BOTTOM_RIGHT);
            StackPane.setMargin(chatbotView, new javafx.geometry.Insets(0, 20, 20, 0));

            chatbotView.setTranslateY(50);
            chatbotView.setOpacity(0);
            contentPane.getChildren().add(chatbotView);

            Timeline showAnimation = new Timeline(
                    new KeyFrame(javafx.util.Duration.millis(300),
                            new KeyValue(chatbotView.translateYProperty(), 0, Interpolator.EASE_BOTH),
                            new KeyValue(chatbotView.opacityProperty(), 1, Interpolator.EASE_BOTH)
                    )
            );
            showAnimation.play();

        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir l'assistant.");
        }
    }

    public void etudeFermerChatbot() {
        Node chatbot = contentPane.lookup("#chatbotOverlay");
        if (chatbot != null) contentPane.getChildren().remove(chatbot);
    }
    private void showEtudeRecommandation() {
        try {
            Node existingReco = contentPane.lookup("#recommandationView");
            if (existingReco != null) {
                if (etudeLastMainViewBeforeReco != null) animateContentChange(etudeLastMainViewBeforeReco);
                else onEtudeCatalogue();
                return;
            }

            // Save current main view
            if (!contentPane.getChildren().isEmpty()) {
                etudeLastMainViewBeforeReco = contentPane.getChildren().get(0);
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/guser/getude/Recommandation.fxml"));
            Node recoView = loader.load();
            recoView.setId("recommandationView");

            com.example.guser.controllers.getude.RecommandationController c = loader.getController();
            if (c != null) c.setCandidatId(etudeCandidatId);

            animateContentChange(recoView);
            setActive(btnRecommandation, Route.ETUDE_RECO);

        } catch (Exception e) {
            e.printStackTrace();
            utils.getude.AlertUtils.showError("❌ Erreur", "Impossible d'ouvrir les recommandations.");
        }
    }








    //setters
    public void setEtudeCandidatId(int id) { this.etudeCandidatId = id; }

    public void setEtudeCurrentCours(int coursId, String coursTitre) {
        this.etudeCurrentCoursId = coursId;
        this.etudeCurrentCoursTitre = coursTitre;
        this.etudeHasCoursActif = true;
        this.etudeHasModuleActif = false;
    }

    public void setEtudeCurrentModule(int moduleId, String moduleTitre) {
        this.etudeCurrentModuleId = moduleId;
        this.etudeCurrentModuleTitre = moduleTitre;
        this.etudeHasModuleActif = true;
    }

    public void resetEtudeCours() {
        this.etudeCurrentCoursId = 0;
        this.etudeCurrentCoursTitre = "";
        this.etudeHasCoursActif = false;
    }

    public void resetEtudeModule() {
        this.etudeCurrentModuleId = 0;
        this.etudeCurrentModuleTitre = "";
        this.etudeHasModuleActif = false;
    }
    public int getEtudeCandidatId() {
        return etudeCandidatId;
    }

    // ============================================
// GOFFRE - Navigation
// ============================================
    @FXML
    public void showOffresTable() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/offres-table.fxml", "Carrieri • Gérer les Offres");
        setActive(btnOffresTable, Route.OFFRES_TABLE);
    }

    @FXML
    public void showOffreAdd() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/offre-add.fxml", "Carrieri • Ajouter une Offre");
        setActive(btnOffreAdd, Route.OFFRE_ADD);
    }

    @FXML
    public void showOffresList() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/offres-list.fxml", "Carrieri • Liste des Offres");
        setActive(btnOffresList, Route.OFFRES_LIST);
    }

    @FXML
    public void showPostulationsCandidats() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/postulations-candidats.fxml", "Carrieri • Postulations");
        setActive(btnPostulationsCandidats, Route.POSTULATIONS_CANDIDATS);
    }

    @FXML
    public void showAdminDashboard() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/admin-dashboard.fxml", "Carrieri • Dashboard Admin");
        setActive(btnAdminDashboard, Route.ADMIN_DASHBOARD);
    }
    // Optional: if you have this page
    public void showFavorites() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/goffre/favorites-list.fxml", "Carrieri • Favoris");
        setActive(null, Route.OFFRES_LIST); // or create Route.FAVORITES if you want
    }

    public void showPostulationsForOffre(int offreId, String offreTitre) {
        if (!requireLogin()) return;
        PostulationsListController plc =
                loadInShellGetController("/com/example/guser/goffre/postulations-list.fxml", "Carrieri • Postulations");
        if (plc != null) plc.setOffreFilter(offreId, offreTitre);
        setActive(btnPostulationsCandidats, Route.POSTULATIONS_CANDIDATS); // or null
    }

    public void showPostuler(int offreId, String offreTitre) {
        if (!requireLogin()) return;
        PostulerPopupController c =
                loadInShellGetController("/com/example/guser/goffre/postuler.fxml", "Carrieri • Postuler");
        if (c != null) c.setOffreInfo(offreId, offreTitre);
        setActive(null, Route.OFFRES_LIST);
    }

    public void showOffreStats(entities.goffre.OffreEmploi offre) {
        if (!requireLogin()) return;
        OffreStatsPopupController c =
                loadInShellGetController("/com/example/guser/goffre/offre-stats-popup.fxml", "Carrieri • Statistiques");
        if (c != null) c.setOffre(offre);
        setActive(null, Route.OFFRES_TABLE);
    }

    public void showQRCode(entities.goffre.OffreEmploi offre) {
        if (!requireLogin()) return;
        QRCodeController c =
                loadInShellGetController("/com/example/guser/goffre/qrcode.fxml", "Carrieri • QR Code");
        if (c != null) c.initData(offre);
        setActive(null, Route.OFFRES_TABLE);
    }

    // ============================================
// GRECLAM - Navigation (copied names)
// ============================================
    @FXML
    public void showReclamationList() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/greclam/reclamationList.fxml", "Carrieri • Réclamations");
        setActive(btnReclamations, null);
    }

    @FXML
    public void showReclamationForm() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/greclam/reclamationForm.fxml", "Carrieri • Nouvelle réclamation");
        setActive(btnAjoutReclamation, null);
    }

    @FXML
    public void showFeedbackList() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/greclam/feedbackList.fxml", "Carrieri • Feedbacks");
        setActive(btnFeedbacks, null);
    }

    @FXML
    public void showFeedbackForm() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/greclam/feedbackForm.fxml", "Carrieri • Nouveau feedback");
        setActive(btnAjoutFeedback, null);
    }

    @FXML
    public void showTraitementList() {
        if (!requireLogin()) return;
        loadInShellAnimated("/com/example/guser/greclam/traitementList.fxml", "Carrieri • Traitements");
        setActive(btnTraitements, null);
    }

    //============GCOMMU=============
    @FXML
    public void showMessagesView() {
        loadInShellAnimated("/com/example/guser/gcommu/message.fxml", "Carrieri • Messages");
        setActive(btnMessages, Route.MESSAGES);
    }

    @FXML
    public void showContactsView() {
        loadInShellAnimated("/com/example/guser/gcommu/contacts-view.fxml", "Carrieri • Contacts");
        setActive(btnContacts, Route.CONTACTS);
    }


    // ========== ACTIVE STATE ==========
    void setActive(Button btn, Route route) {
        clearActive();
        activeButton = btn;
        if (activeButton != null) activeButton.getStyleClass().add("nav-button-active");
    }

    private void clearActive() {
        if (activeButton != null) {
            activeButton.getStyleClass().remove("nav-button-active");
            activeButton.getStyleClass().remove("nav-link-active");
        }
        activeButton = null;
    }

    private void hydrateUserSection() {
        boolean guest = !SessionContext.isLoggedIn();

        if (guest) {
            navNameLabel.setText("Guest");
            navRoleLabel.setText("Not signed in");
            navAvatar.setFill(Color.web("#D1D5DB"));

            // Hide all role menus when guest
            showBox(candidateNavBox, false);
            showBox(recruiterNavBox, false);
            showBox(adminNavBox, false);

            if (btnUserProfile != null) btnUserProfile.setDisable(true);
            if (logoutBtn != null) logoutBtn.setDisable(true);
            return;
        }

        User me = SessionContext.getCurrentUser();
        String role = roleOf(me);

        navNameLabel.setText((safe(me.getFirstname()) + " " + safe(me.getLastname())).trim());
        navRoleLabel.setText(safe(me.getRoles()).isBlank() ? "Member" : me.getRoles());

        String key = "RECRUITER".equals(role) ? me.getLogourl() : me.getProfilepic();
        Platform.runLater(() -> renderCircleFromS3Key(navAvatar, key));

        // Show exactly one section (you can decide admin also sees recruiter/candidate if you want)
        boolean isCandidate = "CANDIDATE".equals(role);
        boolean isRecruiter = "RECRUITER".equals(role);
        boolean isAdmin = "ADMIN".equals(role);


        showBox(candidateNavBox, isCandidate);
        showBox(recruiterNavBox, isRecruiter);
        showBox(adminNavBox, isAdmin);
        // GRECLAM visibility rules (your requirement)
        showBox(reclamNavBox, isAdmin || isCandidate || isRecruiter);

        showNode(btnReclamations, isAdmin);
        showNode(btnFeedbacks, isAdmin);
        showNode(btnTraitements, isAdmin);

        showNode(btnAjoutReclamation, isCandidate || isRecruiter);
        showNode(btnAjoutFeedback, isCandidate || isRecruiter);


        if (btnUserProfile != null) btnUserProfile.setDisable(false);
        if (logoutBtn != null) logoutBtn.setDisable(false);
    }

    public void refreshAfterLogin() {
        hydrateUserSection();
        onProfile(); // or whatever default
    }

    private void renderCircleFromS3Key(Circle circle, String s3Key) {
        try (S3StorageService s3 = new S3StorageService(S3_BUCKET, S3_REGION)) {
            if (s3Key == null || s3Key.isBlank()) return;
            String url = s3.presignedGetUrl(s3Key, Duration.ofMinutes(10));
            Image img = new Image(url, false);
            if (!img.isError()) circle.setFill(new ImagePattern(img));
        } catch (Exception ignored) {}
    }

    // ========== SIDEBAR ANIM ==========
    private void setupAnimations() {
        widthAnim = new Timeline();
        hoverDebounce = new PauseTransition(javafx.util.Duration.millis(60));
    }

    private void animateSidebarTo(double targetW) {
        double current = sidebar.getWidth();
        if (Math.abs(current - targetW) < 0.5) return;

        widthAnim.stop();
        widthAnim.getKeyFrames().setAll(
                new KeyFrame(javafx.util.Duration.millis(220),
                        new KeyValue(sidebar.prefWidthProperty(), targetW, Interpolator.EASE_OUT),
                        new KeyValue(sidebar.minWidthProperty(),  targetW, Interpolator.EASE_OUT),
                        new KeyValue(sidebar.maxWidthProperty(),  targetW, Interpolator.EASE_OUT)
                )
        );
        widthAnim.playFromStart();
    }

    @FXML
    public void expandSidebar() {
        hoverDebounce.stop();
        hoverDebounce.setOnFinished(e -> animateSidebarTo(EXPANDED_W));
        hoverDebounce.playFromStart();
    }

    @FXML
    public void collapseSidebar() {
        hoverDebounce.stop();
        hoverDebounce.setOnFinished(e -> animateSidebarTo(COLLAPSED_W));
        hoverDebounce.playFromStart();
    }
    private void animateContentChange(Node newView) {
        FadeTransition fadeOut = new FadeTransition(javafx.util.Duration.millis(150), contentPane);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        fadeOut.setOnFinished(e -> {
            contentPane.getChildren().setAll(newView);

            FadeTransition fadeIn = new FadeTransition(javafx.util.Duration.millis(300), contentPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });

        fadeOut.play();
    }

    public void showView(Node view) {
        if (view == null) return;
        // Use your animation version if you added it
        animateContentChange(view);
        contentPane.getChildren().setAll(view);
    }



    // ========== RESOURCES ==========
    private void loadLogoSafe(String... paths) {
        try {
            for (String p : paths) {
                try (InputStream is = getClass().getResourceAsStream(p)) {
                    if (is != null) {
                        if (brandLogo != null) brandLogo.setImage(new Image(is));
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        if (brandLogo != null) brandLogo.setImage(null);
    }
    private static void showBox(VBox box, boolean show) {
        if (box == null) return;
        box.setVisible(show);
        box.setManaged(show);
    }
    private static String roleOf(User u) {
        return u == null ? "" : safe(u.getRoles()).trim().toUpperCase();
    }
    private static void showNode(Node n, boolean show) {
        if (n == null) return;
        n.setVisible(show);
        n.setManaged(show);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
