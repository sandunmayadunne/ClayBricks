package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.util.Random;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Mail;

@WebServlet(name = "SendVerificationCode", urlPatterns = {"/SendVerificationCode"})
public class SendVerificationCode extends HttpServlet {
    private final Random random = new Random();
    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        JsonObject responseJson = new JsonObject();
        
        try {
            // Parse request
            JsonObject requestJson = parseRequest(req);
            String adminEmail = requestJson.get("adminEmail").getAsString();

            // Validate email
            if (!isValidEmail(adminEmail)) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Invalid email format");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            // Generate code
            String code = generateRandomCode();
            
            // Send email
            boolean emailSent = sendVerificationEmail(adminEmail, code);
            
            // Prepare response
            responseJson.addProperty("success", emailSent);
            responseJson.addProperty("verificationCode", code);

        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Server error: " + e.getMessage());
            e.printStackTrace();
        }
        
        resp.getWriter().write(responseJson.toString());
    }

    private String generateRandomCode() {
        return String.format("%06d", random.nextInt(999999));
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private boolean sendVerificationEmail(String email, String code) {
        try {
            String subject = "Clay Bricks Admin Verification";
            
            String content = "<html style='font-family: Arial, sans-serif;'>"
                + "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>"
                + "<h1 style='color: #2c3e50; text-align: center; border-bottom: 2px solid #e74c3c;"
                + "    padding-bottom: 10px; margin-bottom: 25px;'>"
                + "Clay Bricks"
                + "</h1>"
                + "<h2 style='color: #34495e; text-align: center;'>Verification Code</h2>"
                + "<div style='background: #f8f9fa; padding: 15px; border-radius: 8px;"
                + "     text-align: center; font-size: 24px; margin: 20px 0;"
                + "     border: 2px dashed #bdc3c7;'>"
                + code
                + "</div>"
                + "<p style='color: #7f8c8d; text-align: center; font-size: 0.9em;'>"
                + "This code is valid for 5 minutes</p>"
                + "</div></html>";

            Mail.sendMail(email, subject, content);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private JsonObject parseRequest(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return gson.fromJson(sb.toString(), JsonObject.class);
    }
}