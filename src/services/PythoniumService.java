package services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PythoniumService {
    private static final String API_URL = "http://localhost:5000/api/execute";
    private final Gson gson = new Gson();

    public PythoniumResponse executeCode(String code, String missionType) throws Exception {
        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        try {
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("code", code);
            requestBody.addProperty("mission_type", missionType);
            requestBody.addProperty("language", "python");

            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();
            String line;

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ?
                                    conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {

                while ((line = br.readLine()) != null) {
                    response.append(line.trim());
                }
            }

            String responseBody = response.toString();
            System.out.println("📥 API Response: " + responseBody);

            if (responseCode >= 200 && responseCode < 300) {
                return gson.fromJson(responseBody, PythoniumResponse.class);
            } else {
                PythoniumResponse errorResponse = new PythoniumResponse();
                errorResponse.success = false;
                errorResponse.error = "API Error (" + responseCode + "): " + responseBody;
                return errorResponse;
            }

        } finally {
            conn.disconnect();
        }
    }

    public static class PythoniumResponse {
        @SerializedName("success")
        public boolean success;

        @SerializedName("output")
        public String output;

        @SerializedName("error")
        public String error;

        @SerializedName("executionTime")
        public double executionTime;

        @SerializedName("result")
        public String result;

        @SerializedName("status")
        public String status;

        public PythoniumResponse() {
            this.success = false;
            this.output = "";
            this.error = "";
            this.executionTime = 0;
            this.result = "";
            this.status = "unknown";
        }

        @Override
        public String toString() {
            return String.format(
                    "PythoniumResponse{success=%s, output='%s', error='%s', executionTime=%.2fms, result='%s'}",
                    success,
                    output != null ? output.substring(0, Math.min(50, output.length())) : "null",
                    error,
                    executionTime,
                    result
            );
        }
    }
}