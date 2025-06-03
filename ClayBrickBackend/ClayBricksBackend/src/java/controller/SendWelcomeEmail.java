package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Mail;

@WebServlet(name = "SendWelcomeEmail", urlPatterns = {"/SendWelcomeEmail"})
public class SendWelcomeEmail extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        try {
            // Parse request
            JsonObject requestJson = parseRequest(req);
            String adminEmail = requestJson.get("adminEmail").getAsString();
            String adminName = requestJson.get("adminName").getAsString();

            // Validate email
            if (!isValidEmail(adminEmail)) {
                responseJson.addProperty("success", false);
                responseJson.addProperty("message", "Invalid email format");
                resp.getWriter().write(responseJson.toString());
                return;
            }

            // Send welcome email
            boolean emailSent = sendWelcomeEmail(adminEmail, adminName);

            // Prepare response
            responseJson.addProperty("success", emailSent);
            responseJson.addProperty("message", emailSent ? "Welcome email sent" : "Failed to send email");

        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", "Server error: " + e.getMessage());
            e.printStackTrace();
        }

        resp.getWriter().write(responseJson.toString());
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[\\w-.]+@([\\w-]+\\.)+[\\w-]{2,4}$");
    }

    private boolean sendWelcomeEmail(String email, String name) {
    try {
        String subject = "Welcome to Clay Bricks Admin Panel";
        
        String content = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
            + "<style>"
            + ":root { --primary: #2A5CAA; --secondary: #e74c3c; --light: #f8f9fa; --text: #000000; }"
            + "body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; line-height: 1.6; margin: 0; padding: 20px; color: var(--text); }"
            + ".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
            + ".header { background: var(--primary); padding: 30px; text-align: center; color: black; border-bottom: 3px solid var(--secondary); }"
            + ".content { padding: 30px; text-align: center; }"
            + ".welcome-message { font-size: 1.2em; margin: 20px 0; line-height: 1.8; }"
            + ".features-list { text-align: left; margin: 30px 0; padding: 20px; background: var(--light); border-radius: 8px; }"
            + ".footer { text-align: center; padding: 20px; color: #666; font-size: 0.9em; border-top: 1px solid #eee; margin-top: 30px; }"
            + "@media (max-width: 600px) { .container { border-radius: 0; } body { padding: 0; } }"
            + "</style></head><body>"
            + "<div class='container'>"
            + "<div class='header'><h1>Clay Bricks Admin</h1></div>"
            + "<div class='content'>"
            + "<h2 style='color: var(--text); margin-bottom: 15px;'>Welcome Aboard, <span style='color: var(--secondary);'>" + name + "</span></h2>"
            + "<div class='welcome-message'>"
            + "<p>Your administrator account has been successfully created for the Clay Bricks management system.</p>"
            + "</div>"
            + "<div class='features-list'>"
            + "<h3 style='margin-top: 0;'>Your Admin Privileges:</h3>"
            + "<ul style='list-style: none; padding-left: 20px;'>"
            + "<li style='margin: 15px 0;'>Full system access management</li>"
            + "<li style='margin: 15px 0;'>Order monitoring and tracking</li>"
            + "<li style='margin: 15px 0;'>User account administration</li>"
            + "<li style='margin: 15px 0;'>Product management</li>"
            + "</ul>"
            + "</div>"
            + "</div>"
            + "<div class='footer'>"
            + "<p>Need assistance? Contact claybricksglobal@gmail.com</p>"
            + "<p>© 2025 Clay Bricks. All rights reserved.</p>"
            + "</div>"
            + "</div></body></html>";

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
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return gson.fromJson(sb.toString(), JsonObject.class);
    }
}
