package services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIEvaluationService {
    static final String AI_SERVER_URL = "http://127.0.0.1:5000/evaluate";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EvaluationResponse evaluatePythonCode(String code, String missionType) throws Exception {
        // Create JSON using ObjectMapper (it should work now with module-info fixed)
        Map<String, Object> request = new HashMap<>();
        request.put("code", code);
        request.put("tests", getTestCases(missionType));

        String jsonPayload = objectMapper.writeValueAsString(request);

        System.out.println("📤 JSON Payload sent to AI server:");
        System.out.println(jsonPayload);
        System.out.println("=".repeat(50));

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(AI_SERVER_URL);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(jsonPayload));

            try (CloseableHttpResponse response = client.execute(httpPost)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                System.out.println("📥 Response from AI server:");
                System.out.println(responseBody);

                if (response.getStatusLine().getStatusCode() == 200) {
                    return objectMapper.readValue(responseBody, EvaluationResponse.class);
                } else {
                    System.err.println("❌ AI Server returned error: " + response.getStatusLine().getStatusCode());
                    System.err.println("Response body: " + responseBody);
                    throw new Exception("AI Server error: " + response.getStatusLine().getStatusCode());
                }
            }
        }
    }

    private List<Map<String, String>> getTestCases(String missionType) {
        List<Map<String, String>> tests = new ArrayList<>();

        switch (missionType) {
            case "ADDITION":
                // Try different formats
                addTest(tests, "5\n3\n", "8");
                addTest(tests, "-2\n7\n", "5");
                addTest(tests, "0\n0\n", "0");
                break;

            case "FACTORIAL":
                addTest(tests, "5\n", "120");
                addTest(tests, "0\n", "1");
                addTest(tests, "7\n", "5040");
                break;

            case "FIBONACCI":
                addTest(tests, "5\n", "5");
                addTest(tests, "7\n", "13");
                addTest(tests, "10\n", "55");
                break;

            case "PRIME_CHECK":
                addTest(tests, "7\n", "True");
                addTest(tests, "10\n", "False");
                addTest(tests, "17\n", "True");
                break;

            default:
                addTest(tests, "test\n", "test output");
        }

        return tests;
    }

    private void addTest(List<Map<String, String>> tests, String input, String expected) {
        Map<String, String> test = new HashMap<>();
        test.put("input", input);
        test.put("expected", expected);
        tests.add(test);
    }
}