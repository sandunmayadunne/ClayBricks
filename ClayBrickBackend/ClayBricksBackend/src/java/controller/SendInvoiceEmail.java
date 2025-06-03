package controller;

import com.google.gson.*;
import java.io.*;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import model.Mail;
import java.util.logging.*;

@WebServlet(name = "SendInvoiceEmail", urlPatterns = {"/SendInvoiceEmail"})
public class SendInvoiceEmail extends HttpServlet {
    
    private final Gson gson = new Gson();
    private static final Logger LOGGER = Logger.getLogger(SendInvoiceEmail.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        JsonObject jsonResponse = new JsonObject();
        boolean sent = false;
        
        try {
            // Parse and validate request
            JsonObject requestData = parseRequest(request);
            
            String userEmail = getStringOrThrow(requestData, "userEmail");
            String orderId = getStringOrThrow(requestData, "orderId");
            String totalPrice = getStringOrThrow(requestData, "totalPrice");    
            String deliveryPrice = getStringOrThrow(requestData, "deliveryPrice");
            String paymentDate = getStringOrThrow(requestData, "paymentDate");
            JsonArray items = requestData.getAsJsonArray("items");

            // Validate numerical values
            validatePrice(totalPrice);
            validatePrice(deliveryPrice);
            
            // Build and send email
            String emailContent = buildInvoiceContent(orderId, paymentDate, items, totalPrice, deliveryPrice);
            
            try {
                Mail.sendMail(userEmail, "Invoice for Order #" + orderId, emailContent);
                sent = true;
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Email sending failed: {0}", e.getMessage());
                sent = false;
            }
            
            jsonResponse.addProperty("success", sent);
            jsonResponse.addProperty("message", sent ? "Invoice sent successfully" : "Failed to send invoice");

        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "Validation error: {0}", e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Validation error: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Server error: {0}", e.getMessage());
            jsonResponse.addProperty("success", false);
            jsonResponse.addProperty("message", "Server error: " + e.getMessage());
        } finally {
            out.print(jsonResponse.toString());
        }
    }
    private JsonObject parseRequest(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return gson.fromJson(reader, JsonObject.class);
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("Invalid JSON format");
        }
    }

    private String getStringOrThrow(JsonObject obj, String key) {
        if (!obj.has(key)) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return obj.get(key).getAsString();
    }

    private void validatePrice(String price) {
        if (!price.matches("^\\d+(\\.\\d{1,2})?$")) {
            throw new IllegalArgumentException("Invalid price format: " + price);
        }
    }

    private String buildInvoiceContent(String orderId, String paymentDate, 
                                      JsonArray items, String totalPrice, 
                                      String deliveryPrice) {
        try {
            StringBuilder itemsHtml = new StringBuilder();
            double subtotal = Double.parseDouble(totalPrice) - Double.parseDouble(deliveryPrice);
            
            for (JsonElement item : items) {
                JsonObject obj = item.getAsJsonObject();
                itemsHtml.append("<tr>")
                        .append("<td>").append(escapeHtml(obj.get("productName").getAsString())).append("</td>")
                        .append("<td>").append(escapeHtml(obj.get("quantity").getAsString())).append("</td>")
                        .append("<td>Rs.").append(escapeHtml(obj.get("price").getAsString())).append("</td>")
                        .append("</tr>");
            }

            return "<!DOCTYPE html>"
                    + "<html>"
                    + "<head>"
                    + "<meta charset='UTF-8'>"
                    + "<style>"
                    + "body { font-family: Arial, sans-serif; line-height: 1.6; }"
                    + ".invoice-box { max-width: 800px; margin: auto; padding: 30px; border: 1px solid #eee; }"
                    + "table { width: 100%; border-collapse: collapse; margin-top: 20px; }"
                    + "th { background-color: #f8f9fa; padding: 12px; text-align: left; }"
                    + "td { padding: 12px; border-bottom: 1px solid #eee; }"
                    + ".total-section { margin-top: 30px; text-align: right; }"
                    + "</style>"
                    + "</head>"
                    + "<body>"
                    + "<div class='invoice-box'>"
                    + "<h1 style='color: #2c3e50; text-align: center;'>Clay Bricks Invoice</h1>"
                    + "<p><strong>Order ID:</strong> " + escapeHtml(orderId) + "</p>"
                    + "<p><strong>Payment Date:</strong> " + escapeHtml(paymentDate) + "</p>"
                    + "<table>"
                    + "<tr><th>Product</th><th>Quantity</th><th>Price</th></tr>"
                    + itemsHtml.toString()
                    + "</table>"
                    + "<div class='total-section'>"
                    + "<p>Subtotal: Rs." + String.format("%.2f", subtotal) + "</p>"
                    + "<p>Delivery: Rs." + escapeHtml(deliveryPrice) + "</p>"
                    + "<p style='font-size: 1.2em;'><strong>Total: Rs." + escapeHtml(totalPrice) + "</strong></p>"
                    + "</div>"
                    + "<p style='text-align: center; color: #7f8c8d; margin-top: 30px;'>"
                    + "Thank you for your purchase!<br>"
                    + "Need help? Contact us at claybricksglobal@gmail.com<br>"
                    + "© 2025 Clay Bricks. All rights reserved."
                    + "</p>"
                    + "</div>"
                    + "</body>"
                    + "</html>";
            
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid price values in items");
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Missing required fields in items");
        }
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}