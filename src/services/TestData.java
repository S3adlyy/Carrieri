package services;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TestData {
    @JsonProperty("input")
    private String input;

    @JsonProperty("expected")
    private String expected;

    // Default constructor
    public TestData() {}

    // Parameterized constructor
    public TestData(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    // Getters and setters
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getExpected() { return expected; }
    public void setExpected(String expected) { this.expected = expected; }
}