package services;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class EvaluationRequest {
    @JsonProperty("code")
    private String code;

    @JsonProperty("tests")
    private List<TestData> tests;

    public EvaluationRequest() {}

    public EvaluationRequest(String code, List<TestData> tests) {
        this.code = code;
        this.tests = tests;
    }

    // Getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public List<TestData> getTests() { return tests; }
    public void setTests(List<TestData> tests) { this.tests = tests; }
}