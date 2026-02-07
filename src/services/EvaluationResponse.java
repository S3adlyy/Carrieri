package services;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EvaluationResponse {
    @JsonProperty("score")
    private int score;

    @JsonProperty("result")
    private String result;

    public EvaluationResponse() {}

    // Getters and setters
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
}