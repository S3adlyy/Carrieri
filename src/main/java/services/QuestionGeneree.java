package services;

import java.util.List;

class QuestionGeneree {
    private final String question;
    private final String bonneReponse;
    private final List<String> reponses;
    private final int points;

    QuestionGeneree(String question, String bonneReponse, List<String> reponses, int points) {
        this.question = question;
        this.bonneReponse = bonneReponse;
        this.reponses = reponses;
        this.points = points;
    }

    String getQuestion() {
        return question;
    }

    String getBonneReponse() {
        return bonneReponse;
    }

    List<String> getReponses() {
        return reponses;
    }

    int getPoints() {
        return points;
    }
}

