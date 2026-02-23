// File: src/test/java/services/PythoniumServiceTest.java
package services;

public class PythoniumServiceTest {

    public static void main(String[] args) {
        System.out.println("🚀 Testing Pythonium API Integration...");
        System.out.println("========================================");

        try {
            // Create service instance
            PythoniumService service = new PythoniumService();

            // Test code
            String testCode = "print('Hello from Pythonium!')\nresult = 42\nprint(f'The answer is {result}')";
            String missionType = "ADDITION";

            System.out.println("📝 Code to execute:");
            System.out.println("----------------------------------------");
            System.out.println(testCode);
            System.out.println("----------------------------------------");
            System.out.println("🎯 Mission type: " + missionType);
            System.out.println();

            // Execute code
            System.out.println("⏳ Sending request to Pythonium API...");
            PythoniumService.PythoniumResponse response = service.executeCode(testCode, missionType);

            System.out.println("\n📊 Response from Pythonium API:");
            System.out.println("========================================");
            System.out.println(response);
            System.out.println("========================================");

            // Check results
            if (response.success) {
                System.out.println("\n✅ TEST PASSED! Pythonium API is working correctly.");
                System.out.println("\n📤 Program output:");
                System.out.println("----------------------------------------");
                System.out.println(response.output);
                System.out.println("----------------------------------------");
                System.out.println("⏱️ Execution time: " + response.executionTime + "ms");
            } else {
                System.out.println("\n❌ TEST FAILED!");
                System.out.println("Error: " + response.error);
            }

        } catch (Exception e) {
            System.err.println("\n❌ Error testing Pythonium API:");
            e.printStackTrace();

            System.err.println("\n💡 Troubleshooting tips:");
            System.err.println("1. Make sure Pythonium API server is running at: http://localhost:5000");
            System.err.println("2. Start the server with: python python_server.py");
            System.err.println("3. Check if port 5000 is available");
            System.err.println("4. Test with curl: curl -X POST http://localhost:5000/api/execute -H \"Content-Type: application/json\" -d '{\"code\":\"print(\\\"Hello\\\")\",\"mission_type\":\"ADDITION\"}'");
        }
    }
}