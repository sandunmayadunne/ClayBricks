package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import model.Mail;

@WebServlet(name = "SendOrderNotification", urlPatterns = {"/SendOrderNotification"})
public class SendOrderNotification extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws IOException {
        
        response.setContentType("application/json");
        JsonObject jsonResponse = new JsonObject();
        
        try {
            // Parse request
            JsonObject requestData = parseRequest(request);
            JsonArray adminEmails = requestData.getAsJsonArray("adminEmails");
            String orderId = requestData.get("orderId").getAsString();
            
            // Build email content
            String emailContent = buildEmailContent(requestData);
            
            // Send to all admins
            int successCount = 0;
            for (int i = 0; i < adminEmails.size(); i++) {
                String email = adminEmails.get(i).getAsString();
                try {
                    Mail.sendMail(email, "New Order #" + orderId, emailContent);
                    successCount++;
                } catch (Exception e) {
                    System.out.println("Failed to send to " + email + ": " + e.getMessage());
                }
            }
            
            jsonResponse.addProperty("success", successCount > 0);
            jsonResponse.addProperty("sentCount", successCount);
            jsonResponse.addProperty("totalCount", adminEmails.size());
            
        } catch (Exception e) {
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", e.getMessage());
            e.printStackTrace();
        }
        
        response.getWriter().write(jsonResponse.toString());
    }

    private String buildEmailContent(JsonObject data) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>")
      .append("<style>")
      .append(":root { --primary: #2A5CAA; --secondary: #e74c3c; --light: #f8f9fa; --text: #000000; }")
      .append("body { font-family: 'Segoe UI', system-ui, -apple-system, sans-serif; line-height: 1.6; color: var(--text); margin: 0; padding: 20px; }")
      .append(".container { max-width: 600px; margin: 0 auto; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }")
      .append(".header { background: var(--primary); padding: 30px; text-align: center; color: black; }")
      .append(".content { padding: 30px; }")
      .append(".order-id { font-size: 24px; color: var(--text); margin: 15px 0; font-weight: 700; }")
      .append(".customer-info { background: var(--light); padding: 20px; border-radius: 8px; margin-bottom: 25px; }")
      .append(".item-card { display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 15px; padding: 15px; border: 1px solid #eee; border-radius: 8px; margin-bottom: 15px; }")
      .append(".total-price { background: var(--light); color: var(--text); padding: 20px; border-radius: 8px; margin-top: 25px; text-align: center; border: 2px solid #ddd; }")
      .append(".footer { text-align: center; padding: 20px; color: var(--text); font-size: 0.9em; border-top: 1px solid #eee; margin-top: 30px; }")
      .append("@media (max-width: 600px) { .item-card { grid-template-columns: 1fr; } .container { border-radius: 0; } body { padding: 0; } }")
      .append("</style></head><body>")
      .append("<div class='container'>")
      .append("<div class='header'><h1>New Order Received</h1></div>")
      .append("<div class='content'>")
      .append("<div class='order-id'>Order ID: #").append(data.get("orderId").getAsString()).append("</div>")
      .append("<div class='customer-info'>")
      .append("<h3 style='margin-top: 0;'>Customer Details</h3>")
      .append("<p><strong>Name:</strong> ").append(data.get("userName").getAsString()).append("</p>")
      .append("<p><strong>Contact:</strong> ").append(data.get("userMobile").getAsString()).append("</p>")
      .append("</div>")
      .append("<h3 style='margin-bottom: 20px;'>Order Items</h3>");

    JsonArray items = data.getAsJsonArray("items");
    for (int i = 0; i < items.size(); i++) {
        JsonObject item = items.get(i).getAsJsonObject();
        sb.append("<div class='item-card'>")
          .append("<div><strong>").append(item.get("name").getAsString()).append("</strong></div>")
          .append("<div>Quantity: ").append(item.get("quantity").getAsInt()).append("</div>")
          .append("<div>Rs.").append(item.get("price").getAsString()).append("</div>")
          .append("</div>");
    }

    sb.append("<div class='total-price'>")
      .append("<h3 style='margin: 0;'>Total Amount</h3>")
      .append("<p style='font-size: 24px; margin: 10px 0;'>Rs.").append(data.get("totalPrice").getAsString()).append("</p>")
      .append("</div>")
      .append("<div class='footer'>")
      .append("<p>Need help? Contact claybricksglobal@gmail.com</p>")
      .append("<p>© 2025 Clay Bricks. All rights reserved.</p>")
      .append("</div>")
      .append("</div></div></body></html>");

    return sb.toString();
}

    private JsonObject parseRequest(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return new com.google.gson.Gson().fromJson(sb.toString(), JsonObject.class);
    }
}