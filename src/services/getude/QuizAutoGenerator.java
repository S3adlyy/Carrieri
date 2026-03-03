package services.getude;

import entities.getude.Lecon;
import entities.getude.Module;
import utils.MyDatabase;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuizAutoGenerator implements IQuizAutoGenerator {

    private Connection con = MyDatabase.getInstance().getConnection();
    private LeconService leconService = new LeconService();
    private Random random = new Random();
    private Set<String> questionsUtilisees = new HashSet<>();

    // ⭐ STATIC pour persister entre instances (organisé par cours)
    private static Map<Integer, Set<String>> phrasesUtiliseesParCours = new HashMap<>();

    // Structure pour stocker concept et sa définition/explication
    private static class ConceptDefinition {
        String concept;
        String definition;
        String phrase;

        ConceptDefinition(String concept, String definition, String phrase) {
            this.concept = concept;
            this.definition = definition;
            this.phrase = phrase;
        }
    }

    // Mots vides à ignorer
    private final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "le", "la", "les", "un", "une", "des", "du", "de", "d'",
            "et", "ou", "mais", "donc", "car", "ni", "or",
            "pour", "dans", "sur", "sous", "avec", "sans", "vers",
            "ce", "cet", "cette", "ces", "mon", "ton", "son",
            "qui", "que", "quoi", "dont", "où", "comment", "pourquoi",
            "est", "sont", "était", "étaient", "sera", "seront",
            "a", "ont", "avait", "avaient", "aura", "auront",
            "à", "par", "entre", "pendant", "temps", "alors", "tout"
    ));

    // ============================================
    // GÉNÉRER LE QUIZ D'UN MODULE
    // ============================================
    public void genererQuizModule(int moduleId) {
        System.out.println("\n🎲 GÉNÉRATION QUIZ INTELLIGENT - Module " + moduleId);
        questionsUtilisees.clear();

        List<Lecon> lecons = leconService.getLeconsByModule(moduleId);

        if (lecons.isEmpty()) {
            System.out.println("ATTENTION: Aucune leçon trouvée pour le module " + moduleId);
            return;
        }

        // Récupérer le cours_id du module
        int coursId = getCoursIdFromModule(moduleId);

        // Extraire les paires concept-définition
        List<ConceptDefinition> conceptsDefinitions = extraireConceptsDefinitions(lecons);
        List<String> phrases = extrairePhrases(lecons);

        // Vérifier qu'on a du contenu exploitable
        if (conceptsDefinitions.isEmpty() && phrases.isEmpty()) {
            System.out.println("⚠️ ATTENTION: Pas de contenu analysable trouvé dans ce module");
            System.out.println("   Vérifiez que les leçons contiennent du texte explicatif");
            return;
        }

        List<QuestionGeneree> questions = new ArrayList<>();

        // Stratégie pour générer EXACTEMENT 5 questions
        if (!conceptsDefinitions.isEmpty() && !phrases.isEmpty()) {
            // Cas idéal : concepts ET phrases disponibles
            // 2 définitions + 2 vrai/faux + 1 complétion = 5 questions
            questions.addAll(genererQuestionsDefinitionLogiques(conceptsDefinitions, 2));
            questions.addAll(genererQuestionsVraiFauxAvecTracking(phrases, 2, coursId));
            questions.addAll(genererQuestionsCompletionAvecTracking(phrases, 1, coursId));
        } else if (!conceptsDefinitions.isEmpty()) {
            // Que des concepts : 2 définitions + 2 importance + 1 autre
            questions.addAll(genererQuestionsDefinitionLogiques(conceptsDefinitions, 3));
            questions.addAll(genererQuestionsImportance(conceptsDefinitions, 2));
        } else if (!phrases.isEmpty()) {
            // Que des phrases : 3 vrai/faux + 2 complétion = 5 questions
            questions.addAll(genererQuestionsVraiFauxAvecTracking(phrases, 3, coursId));
            if (phrases.size() >= 2) {
                questions.addAll(genererQuestionsCompletionAvecTracking(phrases, 2, coursId));
            }
        }

        // Stratégie de complétion améliorée pour atteindre 5 questions
        while (questions.size() < 5) {
            int manquantes = 5 - questions.size();

            if (!phrases.isEmpty()) {
                // Essayer d'abord Vrai/Faux
                List<QuestionGeneree> nouvellesVF = genererQuestionsVraiFauxAvecTracking(phrases, manquantes, coursId);
                if (!nouvellesVF.isEmpty()) {
                    questions.addAll(nouvellesVF);
                    continue;
                }
            }

            if (!phrases.isEmpty() && manquantes > 0) {
                // Essayer ensuite Complétion
                List<QuestionGeneree> nouvellesComp = genererQuestionsCompletionAvecTracking(phrases, manquantes, coursId);
                if (!nouvellesComp.isEmpty()) {
                    questions.addAll(nouvellesComp);
                    continue;
                }
            }

            if (!conceptsDefinitions.isEmpty() && manquantes > 0) {
                // En dernier recours, essayer Définition
                List<QuestionGeneree> nouvellesDef = genererQuestionsDefinitionLogiques(conceptsDefinitions, manquantes);
                if (!nouvellesDef.isEmpty()) {
                    questions.addAll(nouvellesDef);
                    continue;
                }
            }

            // Si vraiment aucune nouvelle question possible, arrêter la boucle
            break;
        }

        // Si plus de 5 questions, tronquer à 5
        if (questions.size() > 5) {
            questions = new ArrayList<>(questions.subList(0, 5));
        }

        // Avertissement si moins de 5 questions
        if (questions.size() < 5) {
            System.out.println("⚠️ ATTENTION: Seulement " + questions.size() + " questions générées (contenu insuffisant)");
        }

        if (questions.isEmpty()) {
            System.out.println("ATTENTION: Aucune question n'a pu être générée");
            return;
        }

        // Mélanger les questions pour varier l'ordre
        Collections.shuffle(questions);

        sauvegarderQuiz(moduleId, questions);
        System.out.println("✅ Quiz généré avec " + questions.size() + " questions (objectif: 5)");
    }

    /**
     * Récupère le cours_id à partir d'un module_id
     */
    private int getCoursIdFromModule(int moduleId) {
        try {
            String sql = "SELECT cours_id FROM module WHERE id = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, moduleId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("cours_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // ============================================
    // GÉNÉRER LE TEST FINAL
    // ============================================
    public void genererTestFinal(int coursId) {
        System.out.println("\n🎲 GÉNÉRATION TEST FINAL INTELLIGENT - Cours " + coursId);
        questionsUtilisees.clear();

        ModuleService moduleService = new ModuleService();
        List<Module> modules = moduleService.getModulesByCours(coursId);

        System.out.println("📚 Modules chargés pour cours " + coursId + ": " + modules.size());

        if (modules.isEmpty()) {
            System.out.println("ATTENTION: Aucun module trouvé pour le cours " + coursId);
            return;
        }

        List<ConceptDefinition> conceptsDefinitions = new ArrayList<>();
        List<String> phrases = new ArrayList<>();

        // Set global pour tracker toutes les phrases utilisées dans le test
        Set<String> phrasesGlobalesUtilisees = new HashSet<>();

        // ⭐ Exclure les phrases des quiz de ce cours
        if (phrasesUtiliseesParCours.containsKey(coursId)) {
            phrasesGlobalesUtilisees.addAll(phrasesUtiliseesParCours.get(coursId));
            System.out.println("   🔒 " + phrasesGlobalesUtilisees.size() + " phrases des quiz exclues du test");
        }

        // Collecter les données de tous les modules
        for (Module m : modules) {
            List<Lecon> lecons = leconService.getLeconsByModule(m.getId());
            if (!lecons.isEmpty()) {
                conceptsDefinitions.addAll(extraireConceptsDefinitions(lecons));

                // Collecter les phrases avec tracking global
                List<String> phrasesDuModule = extrairePhrases(lecons);
                for (String phrase : phrasesDuModule) {
                    String phraseNormalisee = normaliserPhrase(phrase);
                    // Exclure les phrases des quiz
                    if (!phrasesGlobalesUtilisees.contains(phraseNormalisee)) {
                        phrases.add(phrase);
                        phrasesGlobalesUtilisees.add(phraseNormalisee);
                    }
                }
            }
        }

        // Vérifier qu'on a du contenu
        if (conceptsDefinitions.isEmpty() && phrases.isEmpty()) {
            System.out.println("⚠️ ATTENTION: Pas de contenu analysable trouvé dans ce cours");
            System.out.println("   Vérifiez que les leçons contiennent du texte explicatif");
            return;
        }

        List<QuestionGeneree> questions = new ArrayList<>();
        Set<String> toutesLesQuestionsTextes = new HashSet<>();

        // Stratégie pour générer EXACTEMENT 15 questions sans répétition
        if (!conceptsDefinitions.isEmpty() && !phrases.isEmpty()) {
            // Cas idéal : 5 définitions + 5 vrai/faux + 3 complétion + 2 importance = 15
            questions.addAll(genererQuestionsDefinitionLogiques(conceptsDefinitions, 5));
            questions.addAll(genererQuestionsVraiFauxUniques(phrases, 5, toutesLesQuestionsTextes));
            questions.addAll(genererQuestionsCompletionUniques(phrases, 3, toutesLesQuestionsTextes));
            questions.addAll(genererQuestionsImportance(conceptsDefinitions, 2));
        } else if (!conceptsDefinitions.isEmpty()) {
            // Que des concepts : 8 définitions + 7 importance = 15
            questions.addAll(genererQuestionsDefinitionLogiques(conceptsDefinitions, 8));
            questions.addAll(genererQuestionsImportance(conceptsDefinitions, 7));
        } else if (!phrases.isEmpty()) {
            // Que des phrases : 9 vrai/faux + 6 complétion = 15
            questions.addAll(genererQuestionsVraiFauxUniques(phrases, 9, toutesLesQuestionsTextes));
            questions.addAll(genererQuestionsCompletionUniques(phrases, 6, toutesLesQuestionsTextes));
        }

        // Si moins de 15 questions, compléter avec réutilisation intelligente
        if (questions.size() < 15 && !phrases.isEmpty()) {
            int manquantes = 15 - questions.size();

            // Essayer d'abord sans réutilisation
            List<QuestionGeneree> nouvellesVF = genererQuestionsVraiFauxUniques(phrases, manquantes, toutesLesQuestionsTextes);
            questions.addAll(nouvellesVF);

            // Si toujours pas assez, permettre réutilisation modérée
            if (questions.size() < 15 && phrases.size() >= 3) {
                manquantes = 15 - questions.size();
                System.out.println("   ⚠️ Réutilisation de phrases pour atteindre 15 questions");

                // Générer des questions Vrai/Faux avec variations
                for (int i = 0; i < phrases.size() && questions.size() < 15; i++) {
                    String phrase = phrases.get(i);
                    String modifiee = modifierPhrasePourRendreFausse(phrase);

                    if (!modifiee.equals(phrase)) {
                        String question = "Vrai ou Faux : " + modifiee;
                        String questionNorm = normaliserPhrase(question);

                        if (!toutesLesQuestionsTextes.contains(questionNorm)) {
                            toutesLesQuestionsTextes.add(questionNorm);
                            questions.add(new QuestionGeneree(question, "Faux", Arrays.asList("Vrai", "Faux"), 1));
                        }
                    }
                }
            }
        } else if (questions.size() < 15 && !conceptsDefinitions.isEmpty()) {
            int manquantes = 15 - questions.size();
            questions.addAll(genererQuestionsDefinitionLogiques(conceptsDefinitions, manquantes));
        }

        // Si plus de 15 questions, tronquer à 15
        if (questions.size() > 15) {
            questions = new ArrayList<>(questions.subList(0, 15));
        }

        if (questions.isEmpty()) {
            System.out.println("ATTENTION: Aucune question n'a pu être générée");
            return;
        }

        // Mélanger les questions pour plus de diversité
        Collections.shuffle(questions);

        sauvegarderTest(coursId, questions);
        System.out.println("✅ Test final généré avec " + questions.size() + " questions (objectif: 15)");
    }

    /**
     * Version stricte de Vrai/Faux qui n'utilise jamais 2 fois la même phrase
     */
    private List<QuestionGeneree> genererQuestionsVraiFauxUniques(List<String> phrases, int nb, Set<String> questionsExistantes) {
        List<QuestionGeneree> questions = new ArrayList<>();
        if (phrases.isEmpty()) return questions;

        Collections.shuffle(phrases);
        Set<String> phrasesDejaUtilisees = new HashSet<>();

        for (int i = 0; i < phrases.size() && questions.size() < nb; i++) {
            String phrase = phrases.get(i);
            phrase = phrase.replaceAll("\\s+", " ").trim();

            String phraseNormalisee = normaliserPhrase(phrase);
            if (phrasesDejaUtilisees.contains(phraseNormalisee)) {
                continue;
            }

            boolean estVrai = random.nextBoolean();
            String question;

            if (estVrai) {
                question = "Vrai ou Faux : " + phrase;
            } else {
                String modifiee = modifierPhrasePourRendreFausse(phrase);
                if (modifiee.equals(phrase)) {
                    continue;
                }
                question = "Vrai ou Faux : " + modifiee;
            }

            String questionNormalisee = normaliserPhrase(question);
            if (questionsExistantes.contains(questionNormalisee)) {
                continue;
            }

            questionsExistantes.add(questionNormalisee);
            phrasesDejaUtilisees.add(phraseNormalisee);

            List<String> reponses = Arrays.asList("Vrai", "Faux");
            String bonneReponse = estVrai ? "Vrai" : "Faux";

            questions.add(new QuestionGeneree(question, bonneReponse, reponses, 1));
        }

        return questions;
    }

    /**
     * Version stricte de Complétion qui n'utilise jamais 2 fois la même phrase
     */
    private List<QuestionGeneree> genererQuestionsCompletionUniques(List<String> phrases, int nb, Set<String> questionsExistantes) {
        List<QuestionGeneree> questions = new ArrayList<>();
        if (phrases.isEmpty()) return questions;

        Collections.shuffle(phrases);
        Set<String> phrasesDejaUtilisees = new HashSet<>();

        for (int i = 0; i < phrases.size() && questions.size() < nb; i++) {
            String phrase = phrases.get(i);
            phrase = phrase.replaceAll("\\s+", " ").trim();
            String[] mots = phrase.split("\\s+");

            if (mots.length < 8) continue;

            String phraseNormalisee = normaliserPhrase(phrase);
            if (phrasesDejaUtilisees.contains(phraseNormalisee)) {
                continue;
            }

            List<Integer> motsImportants = new ArrayList<>();
            for (int j = 0; j < mots.length; j++) {
                String mot = mots[j].toLowerCase().replaceAll("[.,!?;:]", "");
                if (mot.length() > 3 && !STOP_WORDS.contains(mot) &&
                        !mot.matches("\\d+") && !mot.contains("fx") && !mot.contains("javafx")) {
                    motsImportants.add(j);
                }
            }

            if (motsImportants.isEmpty()) continue;

            int index = motsImportants.get(random.nextInt(motsImportants.size()));
            String motCache = mots[index].replaceAll("[.,!?;:]", "");
            mots[index] = "______";

            String question = "Complétez la phrase : " + String.join(" ", mots);

            String questionNormalisee = normaliserPhrase(question);
            if (questionsExistantes.contains(questionNormalisee)) {
                continue;
            }

            questionsExistantes.add(questionNormalisee);
            phrasesDejaUtilisees.add(phraseNormalisee);

            List<String> reponses = new ArrayList<>();
            reponses.add(motCache);
            Set<String> reponsesTextes = new HashSet<>();
            reponsesTextes.add(motCache.toLowerCase());

            for (int idx : motsImportants) {
                if (idx != index && reponses.size() < 4) {
                    String mot = mots[idx].replaceAll("[.,!?;:]", "");
                    if (mot.equals("______")) continue;

                    String motLower = mot.toLowerCase();
                    if (!reponsesTextes.contains(motLower) && mot.length() > 2 &&
                            !STOP_WORDS.contains(motLower)) {
                        reponses.add(mot);
                        reponsesTextes.add(motLower);
                    }
                }
            }

            while (reponses.size() < 4) {
                String relatedWord = generateRelatedWord(motCache);
                if (!reponsesTextes.contains(relatedWord.toLowerCase())) {
                    reponses.add(relatedWord);
                    reponsesTextes.add(relatedWord.toLowerCase());
                }
            }

            Collections.shuffle(reponses);
            questions.add(new QuestionGeneree(question, motCache, reponses, 2));
        }

        return questions;
    }

    // ============================================
    // FONCTIONS AVEC TRACKING POUR LES QUIZ
    // ============================================

    /**
     * Génère Vrai/Faux en trackant les phrases pour exclure du test final
     */
    private List<QuestionGeneree> genererQuestionsVraiFauxAvecTracking(List<String> phrases, int nb, int coursId) {
        List<QuestionGeneree> questions = genererQuestionsVraiFaux(phrases, nb);

        // Initialiser le set pour ce cours si nécessaire
        phrasesUtiliseesParCours.putIfAbsent(coursId, new HashSet<>());
        Set<String> phrasesUtilisees = phrasesUtiliseesParCours.get(coursId);

        // Tracker toutes les phrases utilisées
        for (QuestionGeneree q : questions) {
            String question = q.getQuestion();
            // Extraire la phrase principale (après "Vrai ou Faux : ")
            String phrase = question.replace("Vrai ou Faux : ", "").trim();
            String phraseNormalisee = normaliserPhrase(phrase);
            phrasesUtilisees.add(phraseNormalisee);
        }

        return questions;
    }

    /**
     * Génère Complétion en trackant les phrases pour exclure du test final
     */
    private List<QuestionGeneree> genererQuestionsCompletionAvecTracking(List<String> phrases, int nb, int coursId) {
        List<QuestionGeneree> questions = genererQuestionsCompletion(phrases, nb);

        // Initialiser le set pour ce cours si nécessaire
        phrasesUtiliseesParCours.putIfAbsent(coursId, new HashSet<>());
        Set<String> phrasesUtilisees = phrasesUtiliseesParCours.get(coursId);

        // Tracker toutes les phrases utilisées (avant remplacement)
        for (QuestionGeneree q : questions) {
            String question = q.getQuestion();
            // Reconstruire la phrase complète (remplacer ______ par le mot)
            String phraseCmplete = question.replace("Complétez la phrase : ", "")
                    .replace("______", q.getBonneReponse())
                    .trim();
            String phraseNormalisee = normaliserPhrase(phraseCmplete);
            phrasesUtilisees.add(phraseNormalisee);
        }

        return questions;
    }

    /**
     * Extrait les paires concept-définition du contenu
     * Recherche des patterns comme "X est Y", "X désigne Y", etc.
     */
    private List<ConceptDefinition> extraireConceptsDefinitions(List<Lecon> lecons) {
        List<ConceptDefinition> resultats = new ArrayList<>();
        Set<String> conceptsVus = new HashSet<>();

        // Patterns pour détecter les définitions (adaptés pour contenu technique)
        Pattern[] patterns = {
                // Patterns classiques - capturer le concept COMPLET (pas de troncature)
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+est\\s+(?:un|une|le|la|l')\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+se\\s+définit\\s+(?:par|comme)\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+désigne\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+représente\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+correspond\\s+à\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),

                // Patterns pour définitions techniques - PLUS PRÉCIS
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+(?:est|constitue)\\s+(?:un|une)\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+permet\\s+(?:de|d')\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),
                Pattern.compile("(?:^|[.!?]\\s+)([A-Z][a-zA-Z0-9\\s]{3,45})\\s+sert\\s+à\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE),

                // Pattern avec "Le/La X est..." - capturer sans l'article
                Pattern.compile("(?:^|[.!?]\\s+)(?:Le|La|L')\\s+([A-Z][a-zA-Z0-9\\s]{3,45})\\s+est\\s+([^.!?]{30,200})[.!?]", Pattern.MULTILINE)
        };

        for (Lecon lecon : lecons) {
            String contenu = lecon.getContenu();
            if (contenu == null || contenu.trim().isEmpty()) continue;

            // Nettoyer le contenu plus soigneusement
            String contenuOriginal = contenu;
            contenu = contenu.replaceAll("<[^>]*>", " ")
                    .replaceAll("\\{[^}]*\\}", " ")
                    .replaceAll("\\[[^\\]]*\\]", " ")
                    .replaceAll("\\([^)]*\\)", " ")
                    .replaceAll("//.*", " ")
                    .replaceAll("/\\*.*?\\*/", " ")
                    .replaceAll("\\s+", " ")
                    .trim();

            // Appliquer les patterns
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(contenu);
                while (matcher.find()) {
                    String concept = matcher.group(1).trim();
                    String definition = matcher.group(2).trim();

                    // Normaliser le concept (enlever les fragments)
                    concept = nettoyerConcept(concept);
                    definition = definition.replaceAll("\\s+", " ");

                    // Valider la qualité (règles assouplies pour contenu technique)
                    if (isValidConceptRelaxed(concept) && isValidDefinitionRelaxed(definition)) {
                        String conceptKey = concept.toLowerCase().trim();

                        // Éviter les doublons
                        if (!conceptsVus.contains(conceptKey)) {
                            conceptsVus.add(conceptKey);
                            resultats.add(new ConceptDefinition(concept, definition, ""));
                        }
                    }
                }
            }
        }

        System.out.println("   📚 " + resultats.size() + " concepts-définitions extraits");
        return resultats;
    }

    /**
     * Nettoie un concept pour enlever les fragments et normaliser
     */
    private String nettoyerConcept(String concept) {
        if (concept == null) return "";

        // Normaliser les espaces
        concept = concept.replaceAll("\\s+", " ").trim();

        // Enlever les préfixes de fragments incomplets
        concept = concept.replaceAll("^(tion|ment|ation|ellement|autres?)\\s+", "");

        // Enlever les articles en début si suivi de mots bizarres
        concept = concept.replaceAll("^(Le|La|Les|Un|Une|Des)\\s+(de|du|des|pour)\\s+", "");

        // Si commence par "for " (mot-clé), enlever
        concept = concept.replaceAll("^for\\s+", "");

        // Si trop court après nettoyage, retourner vide
        if (concept.length() < 3) return "";

        return concept;
    }

    private boolean isValidConcept(String concept) {
        // Le concept ne doit pas être trop long ni contenir de code
        if (concept == null || concept.length() < 3 || concept.length() > 50) {
            return false;
        }

        // Rejeter si contient du code ou caractères techniques
        if (concept.contains("{") || concept.contains("}") ||
                concept.contains("//") || concept.contains("/*") ||
                concept.contains("=") || concept.contains(";") ||
                concept.contains("()") || concept.matches(".*\\b(int|string|void|class|public|private)\\b.*")) {
            return false;
        }

        // Doit contenir au moins une lettre
        if (!concept.matches(".*[a-zA-Z]{2,}.*")) {
            return false;
        }

        // Rejeter les concepts trop génériques
        String lower = concept.toLowerCase().trim();
        if (lower.matches("^(ce|cet|cette|ces|le|la|les|un|une|des)\\s.*")) {
            return false;
        }

        return true;
    }

    // Version assouplie pour contenu technique (accepte noms de technologies)
    private boolean isValidConceptRelaxed(String concept) {
        if (concept == null || concept.length() < 3 || concept.length() > 50) {
            return false;
        }

        // Rejeter si contient du code syntaxique
        if (concept.contains("{") || concept.contains("}") ||
                concept.contains("//") || concept.contains("/*") ||
                concept.contains("();") || concept.contains("=") ||
                concept.contains(";") || concept.contains("()")) {
            return false;
        }

        // Rejeter les mots-clés Java/JavaScript
        String lower = concept.toLowerCase().trim();
        if (lower.matches("^(for|if|else|while|return|void|int|string|double|float|boolean|var|let|const|function)\\b.*")) {
            return false;
        }

        // Rejeter les fragments incomplets (commencent par minuscule sans article)
        if (concept.matches("^[a-z].*") && !concept.matches("^(le|la|les|un|une|des)\\s.*")) {
            return false;
        }

        // Rejeter si trop de mots vides
        String[] mots = concept.trim().split("\\s+");
        if (mots.length > 0) {
            int motsVides = 0;
            for (String mot : mots) {
                if (STOP_WORDS.contains(mot.toLowerCase())) {
                    motsVides++;
                }
            }
            // Si plus de 60% de mots vides, rejeter
            if (motsVides > mots.length * 0.6) {
                return false;
            }
        }

        // Doit contenir au moins une lettre
        if (!concept.matches(".*[a-zA-Z]{3,}.*")) {
            return false;
        }

        return true;
    }

    private boolean isValidDefinition(String definition) {
        // La définition doit être une phrase cohérente
        if (definition == null || definition.length() < 40 || definition.length() > 250) {
            return false;
        }

        // Rejeter si contient du code
        if (definition.contains("{") || definition.contains("}") ||
                definition.contains("//") || definition.contains("/*") ||
                definition.contains("();") || definition.contains("()")) {
            return false;
        }

        // Doit contenir suffisamment de mots
        String[] mots = definition.split("\\s+");
        if (mots.length < 6 || mots.length > 50) {
            return false;
        }

        // Doit contenir des verbes ou mots de définition
        String lower = definition.toLowerCase();
        if (!lower.matches(".*(est|sont|permet|désigne|représente|correspond|consiste|constitue|définit).*")) {
            return false;
        }

        return true;
    }

    // Version assouplie pour définitions techniques
    private boolean isValidDefinitionRelaxed(String definition) {
        if (definition == null || definition.length() < 25 || definition.length() > 300) {
            return false;
        }

        // Rejeter si contient du code syntaxique
        if (definition.contains("{") || definition.contains("}") ||
                definition.contains("//") || definition.contains("/*") ||
                definition.contains("();")) {
            return false;
        }

        String[] mots = definition.split("\\s+");
        if (mots.length < 4 || mots.length > 60) {
            return false;
        }

        return true;
    }

    private List<String> extrairePhrases(List<Lecon> lecons) {
        List<String> phrases = new ArrayList<>();
        Set<String> phrasesVues = new HashSet<>();

        for (Lecon l : lecons) {
            String contenu = l.getContenu();
            if (contenu == null || contenu.trim().isEmpty()) continue;

            // Nettoyer le contenu (enlever les balises, code, etc.)
            contenu = contenu.replaceAll("<[^>]*>", " "); // enlever HTML
            contenu = contenu.replaceAll("\\{[^}]*\\}", " "); // enlever code entre accolades
            contenu = contenu.replaceAll("\\[[^\\]]*\\]", " "); // enlever crochets
            contenu = contenu.replaceAll("\\([^)]*\\)", " "); // enlever parenthèses
            contenu = contenu.replaceAll("//.*?\\n", " "); // enlever commentaires //
            contenu = contenu.replaceAll("/\\*.*?\\*/", " "); // enlever commentaires /* */
            contenu = contenu.replaceAll("\\s+", " "); // normaliser espaces

            String[] split = contenu.split("[.!?]");
            for (String p : split) {
                p = p.trim();

                // Validation des phrases (règles assouplies)
                if (isValidPhraseRelaxed(p)) {
                    String phraseKey = normaliserPhrase(p);

                    // Éviter les doublons stricts et les variations trop similaires
                    if (!phrasesVues.contains(phraseKey)) {
                        phrasesVues.add(phraseKey);
                        phrases.add(p);
                    }
                }
            }
        }

        System.out.println("   📝 " + phrases.size() + " phrases valides extraites");
        return phrases;
    }

    /**
     * Normalise une phrase pour la comparaison (enlève variations mineures)
     */
    private String normaliserPhrase(String phrase) {
        return phrase.toLowerCase()
                .replaceAll("\\s+", " ")
                .replaceAll("[',!?;:]", "")
                .trim();
    }

    private boolean isValidPhrase(String phrase) {
        if (phrase == null || phrase.length() < 50 || phrase.length() > 200) {
            return false;
        }

        // Rejeter les phrases contenant du code
        if (phrase.contains("{") || phrase.contains("}") ||
                phrase.contains("//") || phrase.contains("/*") ||
                phrase.contains("();") || phrase.contains("System.") ||
                phrase.matches(".*\\b(public|private|void|int|String|class|import)\\b.*")) {
            return false;
        }

        // Vérifier que c'est une phrase cohérente
        String[] mots = phrase.split("\\s+");
        if (mots.length < 8 || mots.length > 40) {
            return false;
        }

        // Doit contenir au moins un verbe
        String lower = phrase.toLowerCase();
        if (!lower.matches(".*(est|sont|peut|permet|constitue|représente|définit|implique|contient|utilise|comprend|inclut|désigne).*")) {
            return false;
        }

        // Rejeter les phrases qui ressemblent à des listes ou des titres
        if (phrase.matches("^\\d+[.)].*") || phrase.matches("^[A-Z\\s]+$")) {
            return false;
        }

        return true;
    }

    // Version assouplie pour phrases techniques
    private boolean isValidPhraseRelaxed(String phrase) {
        if (phrase == null || phrase.length() < 35 || phrase.length() > 300) {
            return false;
        }

        // Rejeter les phrases contenant du code syntaxique
        if (phrase.contains("{") || phrase.contains("}") ||
                phrase.contains("//") || phrase.contains("/*") ||
                phrase.contains("();") || phrase.contains("System.out") ||
                phrase.contains("setV") || phrase.contains("setH") ||
                phrase.contains("get") && phrase.contains("()")) {
            return false;
        }

        // Rejeter les formats de liste (": texte" au début)
        if (phrase.matches("^[A-Z][a-z]+\\s*:\\s*.*")) {
            return false;
        }

        // Rejeter si contient des points-virgules (liste de méthodes)
        if (phrase.contains(";") && !phrase.endsWith(";")) {
            return false;
        }

        // Vérifier que c'est une phrase cohérente
        String[] mots = phrase.split("\\s+");
        if (mots.length < 5 || mots.length > 60) {
            return false;
        }

        // Doit contenir au moins un verbe ou mot connecteur
        String lower = phrase.toLowerCase();
        if (!lower.matches(".*(est|sont|peut|permet|pour|avec|utilise|comprend|inclut|possède|offre|fournit|contient|représente|désigne|affiche|organise|gère|crée).*")) {
            return false;
        }

        // Rejeter les titres en majuscules
        if (phrase.matches("^[A-Z\\s]+$")) {
            return false;
        }

        // Rejeter les phrases commençant par des mots-clés de code
        if (phrase.matches("^(set|get|add|remove|update|delete|create|for|while|if).*")) {
            return false;
        }

        return true;
    }

    // ============================================
    // GÉNÉRATION DE QUESTIONS LOGIQUES
    // ============================================

    /**
     * Génère des questions de définition avec des réponses pertinentes
     */
    private List<QuestionGeneree> genererQuestionsDefinitionLogiques(List<ConceptDefinition> conceptsDefinitions, int nb) {
        List<QuestionGeneree> questions = new ArrayList<>();

        if (conceptsDefinitions.isEmpty()) return questions;

        Collections.shuffle(conceptsDefinitions);
        Set<String> questionsTextes = new HashSet<>();

        for (int i = 0; i < conceptsDefinitions.size() && questions.size() < nb; i++) {
            ConceptDefinition cd = conceptsDefinitions.get(i);

            // Vérifier que le concept est de qualité
            if (cd.concept.length() < 5 || cd.concept.split("\\s+").length < 2) {
                continue; // Sauter les concepts trop courts ou d'un seul mot
            }

            // Question avec formulation variée
            String[] formulations = {
                    "Quelle est la définition de « " + cd.concept + " » ?",
                    "Que signifie le terme « " + cd.concept + " » ?",
                    "Comment définir « " + cd.concept + " » ?",
                    "« " + cd.concept + " » peut être défini comme :"
            };

            String question = formulations[random.nextInt(formulations.length)];

            // Éviter les questions en double
            if (questionsTextes.contains(question)) {
                continue;
            }
            questionsTextes.add(question);

            // Réponses : la bonne réponse + 3 réponses pertinentes mais différentes
            List<String> reponses = new ArrayList<>();
            reponses.add(cd.definition);
            Set<String> reponsesTextes = new HashSet<>();
            reponsesTextes.add(cd.definition.toLowerCase());

            // Ajouter 3 autres définitions de concepts différents comme distracteurs
            Set<Integer> used = new HashSet<>();
            used.add(i);

            for (int j = 0; j < conceptsDefinitions.size() && reponses.size() < 4; j++) {
                if (!used.contains(j)) {
                    ConceptDefinition autre = conceptsDefinitions.get(j);
                    String autreDef = autre.definition;
                    String autreDefLower = autreDef.toLowerCase();

                    // S'assurer que la définition est vraiment différente
                    if (!reponsesTextes.contains(autreDefLower) &&
                            !autreDef.equals(cd.definition) &&
                            calculerSimilarite(autreDef, cd.definition) < 0.7) {
                        used.add(j);
                        reponses.add(autreDef);
                        reponsesTextes.add(autreDefLower);
                    }
                }
            }

            // Si pas assez de réponses différentes, ajouter des distracteurs génériques
            while (reponses.size() < 4) {
                String distractor = generateGenericDistractor(cd.concept, reponses);
                if (!reponsesTextes.contains(distractor.toLowerCase())) {
                    reponses.add(distractor);
                    reponsesTextes.add(distractor.toLowerCase());
                }
            }

            Collections.shuffle(reponses);

            questions.add(new QuestionGeneree(question, cd.definition, reponses, 2));
        }

        return questions;
    }

    /**
     * Calcule la similarité entre deux textes (0 = différents, 1 = identiques)
     */
    private double calculerSimilarite(String texte1, String texte2) {
        Set<String> mots1 = new HashSet<>(Arrays.asList(texte1.toLowerCase().split("\\s+")));
        Set<String> mots2 = new HashSet<>(Arrays.asList(texte2.toLowerCase().split("\\s+")));

        Set<String> intersection = new HashSet<>(mots1);
        intersection.retainAll(mots2);

        Set<String> union = new HashSet<>(mots1);
        union.addAll(mots2);

        if (union.isEmpty()) return 0;
        return (double) intersection.size() / union.size();
    }

    private String generateGenericDistractor(String concept, List<String> existingReponses) {
        String[] templates = {
                "Un processus qui permet de mettre en œuvre " + concept,
                "Une méthode différente de " + concept,
                "Un élément qui précède " + concept,
                "Une application spécifique de " + concept,
                "Un concept opposé à " + concept
        };

        for (String template : templates) {
            boolean exists = false;
            for (String existing : existingReponses) {
                if (existing.toLowerCase().contains(template.toLowerCase()) ||
                        calculerSimilarite(template, existing) > 0.5) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                return template;
            }
        }

        return "Une notion liée à " + concept + " (variante " + random.nextInt(100) + ")";
    }

    /**
     * Génère des questions Vrai/Faux basées sur des phrases réelles
     */
    private List<QuestionGeneree> genererQuestionsVraiFaux(List<String> phrases, int nb) {
        List<QuestionGeneree> questions = new ArrayList<>();

        if (phrases.isEmpty()) return questions;

        Collections.shuffle(phrases);
        Set<String> questionsTextes = new HashSet<>();

        // Si très peu de phrases, permettre 2 variations par phrase
        int maxPassages = (phrases.size() < 3) ? 2 : 1;
        int passages = 0;

        while (questions.size() < nb && passages < maxPassages) {
            for (int i = 0; i < phrases.size() && questions.size() < nb; i++) {
                String phrase = phrases.get(i);
                phrase = phrase.replaceAll("\\s+", " ").trim();

                boolean estVrai = (passages == 0) ? random.nextBoolean() : !random.nextBoolean();
                String question;

                if (estVrai) {
                    // Utiliser la phrase telle quelle
                    question = "Vrai ou Faux : " + phrase;
                } else {
                    // Modifier intelligemment la phrase pour la rendre fausse
                    String modifiee = modifierPhrasePourRendreFausse(phrase);

                    // S'assurer que la modification a fonctionné
                    if (modifiee.equals(phrase)) {
                        continue; // Skip cette phrase si on ne peut pas la modifier
                    }

                    question = "Vrai ou Faux : " + modifiee;
                }

                // Éviter les questions en double
                if (questionsTextes.contains(question)) {
                    continue;
                }
                questionsTextes.add(question);

                List<String> reponses = Arrays.asList("Vrai", "Faux");
                String bonneReponse = estVrai ? "Vrai" : "Faux";

                questions.add(new QuestionGeneree(question, bonneReponse, reponses, 1));
            }
            passages++;
        }

        return questions;
    }

    /**
     * Modifie une phrase pour la rendre fausse de façon logique
     */
    private String modifierPhrasePourRendreFausse(String phrase) {
        // Liste de modifications possibles
        List<String> modifications = new ArrayList<>();

        // Négations
        if (phrase.matches(".*\\best\\b.*")) {
            modifications.add(phrase.replaceFirst("\\best\\b", "n'est pas"));
        }
        if (phrase.matches(".*\\bsont\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bsont\\b", "ne sont pas"));
        }
        if (phrase.matches(".*\\bpeut\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bpeut\\b", "ne peut pas"));
        }
        if (phrase.matches(".*\\bpermet\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bpermet\\b", "ne permet pas"));
        }

        // Antonymes
        if (phrase.matches(".*\\baugmente\\b.*")) {
            modifications.add(phrase.replaceFirst("\\baugmente\\b", "diminue"));
        }
        if (phrase.matches(".*\\bavant\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bavant\\b", "après"));
        }
        if (phrase.matches(".*\\bcommence\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bcommence\\b", "finit"));
        }
        if (phrase.matches(".*\\bsupérieur\\b.*")) {
            modifications.add(phrase.replaceFirst("\\bsupérieur\\b", "inférieur"));
        }

        // Choisir une modification qui a changé la phrase
        Collections.shuffle(modifications);
        for (String mod : modifications) {
            if (!mod.equals(phrase)) {
                return mod;
            }
        }

        // Si aucune modification n'a marché, retourner la phrase originale
        // (le code appelant la rejettera)
        return phrase;
    }

    /**
     * Génère des questions de complétion avec distracteurs logiques
     */
    private List<QuestionGeneree> genererQuestionsCompletion(List<String> phrases, int nb) {
        List<QuestionGeneree> questions = new ArrayList<>();

        if (phrases.isEmpty()) return questions;

        Collections.shuffle(phrases);
        Set<String> questionsTextes = new HashSet<>();
        Set<String> phrasesUtilisees = new HashSet<>();

        for (int i = 0; i < phrases.size() && questions.size() < nb; i++) {
            String phrase = phrases.get(i);
            phrase = phrase.replaceAll("\\s+", " ").trim();
            String[] mots = phrase.split("\\s+");

            if (mots.length < 8) continue;

            String phraseNormalisee = normaliserPhrase(phrase);
            if (phrasesUtilisees.contains(phraseNormalisee)) {
                continue;
            }

            // Trouver les mots importants (pas les mots vides)
            List<Integer> motsImportants = new ArrayList<>();
            for (int j = 0; j < mots.length; j++) {
                String mot = mots[j].toLowerCase().replaceAll("[.,!?;:]", "");
                if (mot.length() > 3 && !STOP_WORDS.contains(mot) &&
                        !mot.matches("\\d+") && !mot.contains("fx") && !mot.contains("javafx")) {
                    motsImportants.add(j);
                }
            }

            if (motsImportants.isEmpty()) continue;

            // Cacher un mot important aléatoire
            int index = motsImportants.get(random.nextInt(motsImportants.size()));
            String motCache = mots[index].replaceAll("[.,!?;:]", "");
            mots[index] = "______";

            String question = "Complétez la phrase : " + String.join(" ", mots);

            // Éviter les questions en double (normalisation)
            String questionNormalisee = normaliserPhrase(question);
            if (questionsTextes.contains(questionNormalisee)) {
                continue;
            }
            questionsTextes.add(questionNormalisee);
            phrasesUtilisees.add(phraseNormalisee);

            List<String> reponses = new ArrayList<>();
            reponses.add(motCache);
            Set<String> reponsesTextes = new HashSet<>();
            reponsesTextes.add(motCache.toLowerCase());

            // Ajouter d'autres mots importants de la même phrase comme distracteurs
            for (int idx : motsImportants) {
                if (idx != index && reponses.size() < 4) {
                    String mot = mots[idx].replaceAll("[.,!?;:]", "");
                    // Si le mot est "______", c'est celui qu'on a caché, skip
                    if (mot.equals("______")) continue;

                    String motLower = mot.toLowerCase();

                    if (!reponsesTextes.contains(motLower) && mot.length() > 2 &&
                            !STOP_WORDS.contains(motLower)) {
                        reponses.add(mot);
                        reponsesTextes.add(motLower);
                    }
                }
            }

            // Compléter avec des mots pertinents si nécessaire
            while (reponses.size() < 4) {
                String relatedWord = generateRelatedWord(motCache);
                if (!reponsesTextes.contains(relatedWord.toLowerCase())) {
                    reponses.add(relatedWord);
                    reponsesTextes.add(relatedWord.toLowerCase());
                }
            }

            Collections.shuffle(reponses);
            questions.add(new QuestionGeneree(question, motCache, reponses, 2));
        }

        return questions;
    }

    private String generateRelatedWord(String word) {
        // Générer des variations du mot ou des mots liés
        String[] variations = new String[6];
        int idx = 0;

        // Variations avec suffixes
        if (word.length() > 4 && !word.endsWith("tion")) {
            variations[idx++] = word + "tion";
        }
        if (word.length() > 4 && !word.endsWith("ment")) {
            variations[idx++] = word + "ment";
        }
        if (word.length() > 4 && !word.endsWith("able")) {
            variations[idx++] = word + "able";
        }

        // Variations avec préfixes
        if (word.length() > 3 && !word.startsWith("dé")) {
            variations[idx++] = "dé" + word;
        }
        if (word.length() > 3 && !word.startsWith("in")) {
            variations[idx++] = "in" + word;
        }

        // Variation générique
        variations[idx++] = word + "s";

        // Choisir une variation aléatoire qui existe
        List<String> validVariations = new ArrayList<>();
        for (int i = 0; i < idx; i++) {
            if (variations[i] != null && variations[i].length() > 3) {
                validVariations.add(variations[i]);
            }
        }

        if (!validVariations.isEmpty()) {
            return validVariations.get(random.nextInt(validVariations.size()));
        }

        return "terme_" + random.nextInt(100);
    }

    /**
     * Génère des questions sur l'importance des concepts
     */
    private List<QuestionGeneree> genererQuestionsImportance(List<ConceptDefinition> conceptsDefinitions, int nb) {
        List<QuestionGeneree> questions = new ArrayList<>();

        if (conceptsDefinitions.isEmpty()) return questions;

        Collections.shuffle(conceptsDefinitions);
        Set<String> questionsTextes = new HashSet<>();

        for (int i = 0; i < conceptsDefinitions.size() && questions.size() < nb; i++) {
            ConceptDefinition cd = conceptsDefinitions.get(i);

            String question = "Quel est le rôle de « " + cd.concept + " » dans ce cours ?";

            // Éviter les questions en double
            if (questionsTextes.contains(question)) {
                continue;
            }
            questionsTextes.add(question);

            List<String> reponses = new ArrayList<>();
            Set<String> reponsesTextes = new HashSet<>();

            // Bonne réponse basée sur la définition
            String bonneReponse = "C'est un concept clé défini comme : " +
                    (cd.definition.length() > 80 ?
                            cd.definition.substring(0, 80) + "..." :
                            cd.definition);

            reponses.add(bonneReponse);
            reponsesTextes.add(bonneReponse.toLowerCase());

            // Distracteurs plus cohérents
            String[] distracteursTemplates = {
                    "C'est un simple exemple qui illustre d'autres concepts",
                    "C'est uniquement une introduction sans détail approfondi",
                    "Ce concept est mentionné brièvement mais n'est pas central",
                    "Il s'agit d'une variante moins importante d'un autre concept"
            };

            for (String distractor : distracteursTemplates) {
                if (reponses.size() >= 4) break;
                if (!reponsesTextes.contains(distractor.toLowerCase())) {
                    reponses.add(distractor);
                    reponsesTextes.add(distractor.toLowerCase());
                }
            }

            Collections.shuffle(reponses);

            questions.add(new QuestionGeneree(question, bonneReponse, reponses, 2));
        }

        return questions;
    }

    // ============================================
    // SAUVEGARDE
    // ============================================

    private void sauvegarderQuiz(int moduleId, List<QuestionGeneree> questions) {
        if (questions.isEmpty()) {
            System.out.println("ATTENTION: Aucune question a sauvegarder");
            return;
        }

        String sqlQuestion = "INSERT INTO question_quiz (module_id, question_text, points, ordre) VALUES (?, ?, ?, ?)";
        String sqlReponse = "INSERT INTO reponse (question_id, question_type, reponse_text, est_correcte, ordre) VALUES (?, 'QUIZ', ?, ?, ?)";

        try {
            con.setAutoCommit(false);
            int ordre = 1;
            for (QuestionGeneree q : questions) {
                if (q == null) continue;

                PreparedStatement psQ = con.prepareStatement(sqlQuestion, Statement.RETURN_GENERATED_KEYS);
                psQ.setInt(1, moduleId);
                psQ.setString(2, q.getQuestion());
                psQ.setInt(3, q.getPoints());
                psQ.setInt(4, ordre++);
                psQ.executeUpdate();

                ResultSet rs = psQ.getGeneratedKeys();
                int questionId = 0;
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }

                PreparedStatement psR = con.prepareStatement(sqlReponse);
                int ordreR = 1;
                for (String reponse : q.getReponses()) {
                    psR.setInt(1, questionId);
                    psR.setString(2, reponse);
                    psR.setBoolean(3, reponse.equals(q.getBonneReponse()));
                    psR.setInt(4, ordreR++);
                    psR.addBatch();
                }
                psR.executeBatch();

                System.out.println("   ✅ Question: " + tronquer(q.getQuestion(), 60));
            }
            con.commit();
            System.out.println("✅ Quiz sauvegardé avec succès");
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void sauvegarderTest(int coursId, List<QuestionGeneree> questions) {
        if (questions.isEmpty()) {
            System.out.println("ATTENTION: Aucune question a sauvegarder");
            return;
        }

        String sqlQuestion = "INSERT INTO question_test (cours_id, question_text, points, ordre) VALUES (?, ?, ?, ?)";
        String sqlReponse = "INSERT INTO reponse (question_id, question_type, reponse_text, est_correcte, ordre) VALUES (?, 'TEST', ?, ?, ?)";

        try {
            con.setAutoCommit(false);
            int ordre = 1;
            for (QuestionGeneree q : questions) {
                if (q == null) continue;

                PreparedStatement psQ = con.prepareStatement(sqlQuestion, Statement.RETURN_GENERATED_KEYS);
                psQ.setInt(1, coursId);
                psQ.setString(2, q.getQuestion());
                psQ.setInt(3, q.getPoints());
                psQ.setInt(4, ordre++);
                psQ.executeUpdate();

                ResultSet rs = psQ.getGeneratedKeys();
                int questionId = 0;
                if (rs.next()) {
                    questionId = rs.getInt(1);
                }

                PreparedStatement psR = con.prepareStatement(sqlReponse);
                int ordreR = 1;
                for (String reponse : q.getReponses()) {
                    psR.setInt(1, questionId);
                    psR.setString(2, reponse);
                    psR.setBoolean(3, reponse.equals(q.getBonneReponse()));
                    psR.setInt(4, ordreR++);
                    psR.addBatch();
                }
                psR.executeBatch();

                System.out.println("   ✅ Question: " + tronquer(q.getQuestion(), 60));
            }
            con.commit();
            System.out.println("✅ Test sauvegardé avec succès");
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            e.printStackTrace();
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private String tronquer(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max) + "...";
    }
}

