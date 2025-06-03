package controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import model.Mail;

@WebServlet(name = "SendDeleteNotification", urlPatterns = {"/SendDeleteNotification"})
public class SendDeleteNotification extends HttpServlet {

    private final Gson gson = new Gson();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        JsonObject responseJson = new JsonObject();

        try {
            JsonObject requestJson = parseRequest(req);
            String adminEmail = requestJson.get("adminEmail").getAsString();

            String subject = "Account Deleted - Clay Bricks Admin";
            String content = "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>"
                + "<style>"
                + ":root { --primary: #2A5CAA; --secondary: #e74c3c; --light: #f8f9fa; --text: #000000; }"
                + "body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; line-height: 1.6; margin: 0; padding: 20px; }"
                + ".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
                + ".header { background: var(--primary); padding: 30px; text-align: center; color: black; border-bottom: 3px solid var(--secondary); }"
                + ".content { padding: 30px; text-align: center; }"
                + ".notification-box { background: var(--light); padding: 25px; border-radius: 8px; margin: 25px 0; border-left: 4px solid var(--secondary); }"
                + ".footer { text-align: center; padding: 20px; color: #666; font-size: 0.9em; border-top: 1px solid #eee; margin-top: 30px; }"
                + "</style></head><body>"
                + "<div class='container'>"
                + "<div class='header'><h1>Clay Bricks Admin</h1></div>"
                + "<div class='content'>"
                + "<h2 style='color: var(--text); margin-bottom: 15px;'>Account Removal Notification</h2>"
                + "<div class='notification-box'>"
                + "<p style='font-size: 1.1em; margin: 0; color: var(--text);'>"
                + "Your administrator account has been permanently removed from the Clay Bricks system."
                + "</p>"
                + "</div>"
                + "<p style='color: #666; margin-top: 20px;'>This action is irreversible. If this was a mistake, please contact us immediately.</p>"
                + "</div>"
                + "<div class='footer'>"
                + "<p>Contact support: claybricksglobal@gmail.com</p>"
                + "<p>© 2025 Clay Bricks. All rights reserved.</p>"
                + "</div>"
                + "</div></body></html>";
            Mail.sendMail(adminEmail, subject, content);
            responseJson.addProperty("success", true);
        } catch (Exception e) {
            responseJson.addProperty("success", false);
            responseJson.addProperty("message", e.getMessage());
        }

        resp.getWriter().write(responseJson.toString());
    }

    private JsonObject parseRequest(HttpServletRequest req) throws IOException {
        // Same as previous implementation
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
