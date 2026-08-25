package agent;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebUI {

    private static final Path RULES_PATH = Paths.get("rules.txt");
    private static final Path CLAIMS_PATH = Paths.get("claims.csv");
    private static final String CURRENT_USER = "Olivia Rhye";
    private static final Logger LOGGER = Logger.getLogger(WebUI.class.getName());

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        PageHandler pageHandler = new PageHandler();
        server.createContext("/", pageHandler);
        server.createContext("/my-requests", pageHandler);
        server.createContext("/all-requests", pageHandler);
        server.createContext("/mileage", pageHandler);
        server.createContext("/expenses", pageHandler);
        server.createContext("/approvals", pageHandler);
        server.createContext("/reports", pageHandler);
        server.createContext("/analytics", pageHandler);
        server.createContext("/policy-rules", pageHandler);
        server.createContext("/settings", pageHandler);

        server.setExecutor(null);
        server.start();

        LOGGER.info("Policy Approval Agent UI started.");
        LOGGER.info("Open: http://localhost:8080");
    }

    private static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                String response = renderPageForPath(path);

                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling request", e);
                String error = "ERROR: " + e.getMessage();
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(500, bytes.length);

                try (OutputStream output = exchange.getResponseBody()) {
                    output.write(bytes);
                }
            }
        }
    }

    private static List<Claim> readClaims() {
        List<Claim> claims = new ArrayList<>();
        try {
            if (Files.exists(CLAIMS_PATH)) {
                List<String> lines = Files.readAllLines(CLAIMS_PATH, StandardCharsets.UTF_8);
                boolean first = true;
                for (String line : lines) {
                    if (first) { first = false; continue; }
                    String[] parts = line.split(",");
                    if (parts.length >= 5) {
                        claims.add(new Claim(
                                parts[0].trim(),
                                parts[1].trim(),
                                parts.length > 4 ? parts[4].trim() : "Engineering",
                                parts[2].trim(),
                                Double.parseDouble(parts[3].trim()),
                                parts.length > 5 ? parts[5].trim() : "Standard Expense Claim",
                                true
                        ));
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not read claims file", e);
        }
        if (claims.isEmpty()) {
            claims.add(new Claim("EXP-2024-1056", "Alice Smith", "Engineering", "Travel", 450.00, "Client site visit", true));
            claims.add(new Claim("EXP-2024-1055", "Bob Jones", "Product", "Software", 1250.00, "Annual license renewal", true));
            claims.add(new Claim("EXP-2024-1054", "Charlie Brown", "Marketing", "Meals", 85.50, "Team lunch", false));
            claims.add(new Claim("EXP-2024-1053", "Diana Prince", "Sales", "Travel", 1550.00, "Q3 Conference", true));
            claims.add(new Claim("EXP-2024-1052", "Evan Wright", "Engineering", "Hardware", 190.00, "Peripherals", true));
        }
        return claims;
    }

    private static String escape(String val) {
        if (val == null) return "";
        return val.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String renderPageForPath(String currentPath) throws IOException {
        if (currentPath != null && currentPath.length() > 1 && currentPath.endsWith("/")) {
            currentPath = currentPath.substring(0, currentPath.length() - 1);
        }
        if (currentPath == null || currentPath.isEmpty()) {
            currentPath = "/";
        }

        List<String> rawRuleLines = Files.exists(RULES_PATH) ? Files.readAllLines(RULES_PATH, StandardCharsets.UTF_8) : new ArrayList<>();
        List<Rule> rules = RuleParser.parseRules(rawRuleLines);
        List<Claim> claims = readClaims();
        List<Decision> decisions = ApprovalEngine.evaluateBatch(claims, rules);

        int escalated = 0;
        double totalAmount = 0;
        double approvedAmount = 0;
        double rejectedAmount = 0;
        int approvedCountVal = 0;
        int rejectedCountVal = 0;

        final double IRS_MILEAGE_RATE = 0.67;

        for (Decision d : decisions) {
            totalAmount += d.amount;
            if ("approve".equalsIgnoreCase(d.decision)) {
                approvedAmount += d.amount;
                approvedCountVal++;
            } else if ("reject".equalsIgnoreCase(d.decision)) {
                rejectedAmount += d.amount;
                rejectedCountVal++;
            } else if ("escalate".equalsIgnoreCase(d.decision) || "in-review".equalsIgnoreCase(d.decision)) {
                escalated++;
            }
        }

        int pendingTotal = Math.max(escalated, 3);
        int totalRequests = decisions.size();

        StringBuilder sb = new StringBuilder();

        sb.append("<!DOCTYPE html>\n")
                .append("<html lang=\"en\">\n")
                .append("<head>\n")
                .append("    <meta charset=\"UTF-8\">\n")
                .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
                .append("    <title>Expense Approval Dashboard</title>\n")
                .append("    <link href=\"https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap\" rel=\"stylesheet\">\n")
                .append("    <style>\n")
                .append("        * { box-sizing: border-box; margin: 0; padding: 0; }\n")
                .append("        body { font-family: 'Inter', sans-serif; background: #f8fafc; color: #0f172a; }\n")
                .append("        .layout { display: flex; min-height: 100vh; }\n")
                .append("        .sidebar { width: 260px; background: #ffffff; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; justify-content: space-between; padding: 24px 16px; position: fixed; height: 100vh; overflow-y: auto; z-index: 20; }\n")
                .append("        .brand-area { display: flex; align-items: center; gap: 10px; padding: 0 12px; margin-bottom: 24px; }\n")
                .append("        .brand-icon { width: 34px; height: 34px; background: #10b981; border-radius: 8px; display: flex; align-items: center; justify-content: center; color: white; font-weight: 700; font-size: 16px; }\n")
                .append("        .brand-text { font-size: 18px; font-weight: 700; color: #0f172a; letter-spacing: -0.5px; }\n")
                .append("        .nav-group { margin-bottom: 20px; display: flex; flex-direction: column; gap: 4px; }\n")
                .append("        .nav-item { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 8px; color: #64748b; font-size: 13px; font-weight: 500; text-decoration: none; transition: all 0.2s; }\n")
                .append("        .nav-item:hover { background: #f1f5f9; color: #0f172a; }\n")
                .append("        .nav-item.active { background: #10b981; color: white; font-weight: 600; }\n")
                .append("        .sidebar-help { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 16px; text-align: center; margin-top: auto; }\n")
                .append("        .sidebar-help-icon { width: 36px; height: 36px; background: #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin: 0 auto 10px auto; font-size: 16px; }\n")
                .append("        .sidebar-help h4 { font-size: 13px; font-weight: 600; color: #0f172a; margin-bottom: 4px; }\n")
                .append("        .sidebar-help p { font-size: 11px; color: #64748b; margin-bottom: 12px; line-height: 1.4; }\n")
                .append("        .sidebar-help-btn { display: block; background: #10b981; color: white; padding: 8px; border-radius: 6px; font-size: 12px; font-weight: 600; text-decoration: none; }\n")
                .append("        .main { margin-left: 260px; flex: 1; display: flex; flex-direction: column; }\n")
                .append("        .header { background: #ffffff; border-bottom: 1px solid #e2e8f0; padding: 14px 32px; display: flex; justify-content: space-between; align-items: center; position: sticky; top: 0; z-index: 10; }\n")
                .append("        .header-search { display: flex; align-items: center; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 8px 14px; width: 380px; gap: 10px; color: #94a3b8; font-size: 13px; }\n")
                .append("        .header-search input { border: none; background: transparent; outline: none; font-size: 13px; width: 100%; color: #0f172a; }\n")
                .append("        .header-right { display: flex; align-items: center; gap: 16px; }\n")
                .append("        .notif-badge { width: 36px; height: 36px; border: 1px solid #e2e8f0; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: #fff; cursor: pointer; }\n")
                .append("        .user-profile { display: flex; align-items: center; gap: 10px; border-left: 1px solid #e2e8f0; padding-left: 16px; }\n")
                .append("        .user-avatar { width: 36px; height: 36px; border-radius: 50%; background: #10b981; color: white; display: flex; align-items: center; justify-content: center; font-weight: 600; font-size: 13px; }\n")
                .append("        .user-info .name { font-size: 13px; font-weight: 600; color: #0f172a; }\n")
                .append("        .user-info .role { font-size: 11px; color: #64748b; }\n")
                .append("        .content { padding: 28px 32px; }\n")
                .append("        .dashboard-welcome { margin-bottom: 22px; }\n")
                .append("        .dashboard-welcome h2 { font-size: 22px; font-weight: 700; color: #0f172a; letter-spacing: -0.5px; }\n")
                .append("        .dashboard-welcome p { font-size: 13px; color: #64748b; margin-top: 2px; }\n")
                .append("        .metrics-grid { display: grid; grid-template-columns: repeat(5, 1fr); gap: 16px; margin-bottom: 24px; }\n")
                .append("        .metric-card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 18px; box-shadow: 0 1px 2px rgba(0,0,0,0.02); }\n")
                .append("        .metric-card-top { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }\n")
                .append("        .metric-icon-box { width: 32px; height: 32px; border-radius: 8px; background: #f1f5f9; display: flex; align-items: center; justify-content: center; font-size: 14px; }\n")
                .append("        .metric-header { font-size: 11px; font-weight: 600; color: #64748b; text-transform: uppercase; letter-spacing: 0.5px; }\n")
                .append("        .metric-value { font-size: 22px; font-weight: 700; color: #0f172a; margin-bottom: 6px; }\n")
                .append("        .metric-footer { font-size: 11px; color: #10b981; font-weight: 500; display: flex; align-items: center; gap: 4px; }\n")
                .append("        .dashboard-row-2 { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 24px; }\n")
                .append("        .card { background: #ffffff; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; box-shadow: 0 1px 2px rgba(0,0,0,0.02); margin-bottom: 20px; }\n")
                .append("        .card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }\n")
                .append("        .card-title { font-size: 15px; font-weight: 600; color: #0f172a; }\n")
                .append("        .card-select { background: #f8fafc; border: 1px solid #e2e8f0; padding: 6px 12px; border-radius: 6px; font-size: 12px; color: #64748b; outline: none; cursor: pointer; }\n")
                .append("        table { width: 100%; border-collapse: collapse; text-align: left; }\n")
                .append("        th { background: #f8fafc; color: #64748b; font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; padding: 10px 14px; border-bottom: 1px solid #e2e8f0; }\n")
                .append("        td { padding: 12px 14px; font-size: 13px; color: #334155; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }\n")
                .append("        tr:last-child td { border-bottom: none; }\n")
                .append("        tr:hover td { background: #f8fafc; }\n")
                .append("        .badge { display: inline-flex; align-items: center; gap: 4px; padding: 3px 8px; border-radius: 6px; font-size: 11px; font-weight: 500; }\n")
                .append("        .badge.approve { background: #ecfdf5; color: #047857; }\n")
                .append("        .badge.reject { background: #fef2f2; color: #b91c1c; }\n")
                .append("        .badge.escalate, .badge.in-review { background: #fffbeb; color: #b45309; }\n")
                .append("        .action-btn { background: #10b981; color: white; border: none; padding: 8px 16px; border-radius: 8px; font-size: 13px; font-weight: 600; cursor: pointer; text-decoration: none; display: inline-flex; align-items: center; gap: 6px; }\n")
                .append("        .action-btn:hover { background: #059669; }\n")
                .append("        .empty-state { padding: 30px; text-align: center; color: #64748b; font-size: 13px; }\n")
                .append("    </style>\n")
                .append("</head>\n")
                .append("<body>\n")
                .append("<div class=\"layout\">\n")
                .append("    <aside class=\"sidebar\">\n")
                .append("        <div>\n")
                .append("            <div class=\"brand-area\">\n")
                .append("                <div class=\"brand-icon\">⚡</div>\n")
                .append("                <div class=\"brand-text\">Expensely</div>\n")
                .append("            </div>\n")
                .append("            <div class=\"nav-group\">\n");

        sb.append(String.format("<a href=\"/\" class=\"nav-item %s\">📊 Dashboard</a>\n", "/".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/my-requests\" class=\"nav-item %s\">📝 My Requests</a>\n", "/my-requests".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/all-requests\" class=\"nav-item %s\">📋 All Requests</a>\n", "/all-requests".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/mileage\" class=\"nav-item %s\">🚗 Mileage</a>\n", "/mileage".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/expenses\" class=\"nav-item %s\">💳 Expenses</a>\n", "/expenses".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/approvals\" class=\"nav-item %s\">✅ Approvals</a>\n", "/approvals".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/reports\" class=\"nav-item %s\">📈 Reports</a>\n", "/reports".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/analytics\" class=\"nav-item %s\">📉 Analytics</a>\n", "/analytics".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/policy-rules\" class=\"nav-item %s\">⚙️ Policy & Rules</a>\n", "/policy-rules".equals(currentPath) ? "active" : ""));
        sb.append(String.format("<a href=\"/settings\" class=\"nav-item %s\">🛠️ Settings</a>\n", "/settings".equals(currentPath) ? "active" : ""));

        sb.append("            </div>\n")
                .append("        </div>\n")
                .append("        <div class=\"sidebar-help\">\n")
                .append("            <div class=\"sidebar-help-icon\">❓</div>\n")
                .append("            <h4>Need Help?</h4>\n")
                .append("            <p>Check our help docs or contact support.</p>\n")
                .append("            <a href=\"/all-requests\" class=\"sidebar-help-btn\">Visit Help Center</a>\n")
                .append("        </div>\n")
                .append("    </aside>\n")
                .append("    <main class=\"main\">\n")
                .append("        <header class=\"header\">\n")
                .append("            <div class=\"header-search\">\n")
                .append("                🔍 <input type=\"text\" placeholder=\"Search for requests, employees, or departments...\">\n")
                .append("            </div>\n")
                .append("            <div class=\"header-right\">\n")
                .append("                <div class=\"notif-badge\">🔔</div>\n")
                .append("                <div class=\"user-profile\">\n")
                .append("                    <div class=\"user-avatar\">OR</div>\n")
                .append("                    <div class=\"user-info\">\n")
                .append("                        <div class=\"name\">Olivia Rhye</div>\n")
                .append("                        <div class=\"role\">Finance Manager</div>\n")
                .append("                    </div>\n")
                .append("                </div>\n")
                .append("            </div>\n")
                .append("        </header>\n")
                .append("        <div class=\"content\">\n");

        if ("/policy-rules".equals(currentPath)) {
            sb.append("<div class=\"dashboard-welcome\"><h2>Policy & Rules</h2><p>Active system evaluation rules loaded from rules.txt</p></div>");
            sb.append("<div class=\"card\"><div class=\"card-header\"><span class=\"card-title\">Loaded Rules</span></div><div style='display:flex; flex-direction:column; gap:10px;'>");
            for (String rule : rawRuleLines) {
                if (!rule.trim().isEmpty()) {
                    sb.append("<div style='background:#f8fafc; border:1px solid #e2e8f0; padding:12px 16px; border-radius:8px; font-family:monospace; font-size:12px; color:#334155;'>")
                            .append(escape(rule))
                            .append("</div>");
                }
            }
            sb.append("</div></div>");
        } else if ("/my-requests".equals(currentPath)) {
            double myTotalAmount = 0;
            double myApprovedAmount = 0;
            int myCount = 0;
            for (Decision d : decisions) {
                if (CURRENT_USER.equalsIgnoreCase(d.employee)) {
                    myCount++;
                    myTotalAmount += d.amount;
                    if ("approve".equalsIgnoreCase(d.decision)) {
                        myApprovedAmount += d.amount;
                    }
                }
            }

            sb.append("<div class=\"dashboard-welcome\" style=\"display:flex; justify-content:space-between; align-items:flex-end;\">\n")
                    .append("    <div>\n")
                    .append("        <h2>My Personal Requests</h2>\n")
                    .append("        <p>Manage, monitor, and track expense claims submitted directly by <strong>Olivia Rhye</strong>.</p>\n")
                    .append("    </div>\n")
                    .append("    <a href=\"/all-requests\" class=\"action-btn\">＋ Submit New Claim</a>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">My Submissions</div>\n")
                    .append("            <div class=\"metric-icon-box\">📁</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">").append(myCount).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Total claims logged</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Claimed Volume</div>\n")
                    .append("            <div class=\"metric-icon-box\">💲</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", myTotalAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Sum of all active filings</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Approved Volume</div>\n")
                    .append("            <div class=\"metric-icon-box\">✅</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">$").append(String.format("%.2f", myApprovedAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Cleared for payout</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">My Submission Ledger</span>\n")
                    .append("        <select class=\"card-select\"><option>All Statuses</option></select>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr>\n")
                    .append("                <th>Claim ID</th>\n")
                    .append("                <th>Department</th>\n")
                    .append("                <th>Category</th>\n")
                    .append("                <th>Amount</th>\n")
                    .append("                <th>Status</th>\n")
                    .append("                <th>Policy Rationale</th>\n")
                    .append("            </tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            boolean foundAny = false;
            for (Decision d : decisions) {
                if (CURRENT_USER.equalsIgnoreCase(d.employee)) {
                    foundAny = true;
                    sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                            .append("<td>").append(escape(d.department)).append("</td>")
                            .append("<td>").append(escape(d.category)).append("</td>")
                            .append("<td>$").append(d.amount).append("</td>")
                            .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td>")
                            .append("<td style=\"color: #64748b;\">").append(escape(d.rationale)).append("</td></tr>");
                }
            }
            if (!foundAny) {
                sb.append("<tr><td colspan=\"6\" class=\"empty-state\">You have not submitted any claims yet under user ").append(CURRENT_USER).append(".</td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/all-requests".equals(currentPath)) {
            int totalRequestsCount = decisions.size();
            double systemTotalVolume = totalAmount;

            sb.append("<div class=\"dashboard-welcome\" style=\"display:flex; justify-content:space-between; align-items:flex-end;\">\n")
                    .append("    <div>\n")
                    .append("        <h2>All System Requests</h2>\n")
                    .append("        <p>Complete organizational view of all employee expense claims, company travel requests, and policy evaluations.</p>\n")
                    .append("    </div>\n")
                    .append("    <a href=\"/all-requests\" class=\"action-btn\">＋ Export Full Report</a>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Requests Logged</div>\n")
                    .append("            <div class=\"metric-icon-box\">📋</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">").append(totalRequestsCount).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Organization-wide filings</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Financial Volume</div>\n")
                    .append("            <div class=\"metric-icon-box\">💲</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", systemTotalVolume)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Combined requested amount</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Auto-Approved Claims</div>\n")
                    .append("            <div class=\"metric-icon-box\">✅</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">").append(approvedCountVal).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Cleared through rule engine</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">Global Claims Master Ledger</span>\n")
                    .append("        <select class=\"card-select\"><option>All Departments</option></select>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr><th>Claim ID</th><th>Employee</th><th>Department</th><th>Category</th><th>Amount</th><th>Decision</th><th>Policy Rationale</th></tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            for (Decision d : decisions) {
                sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                        .append("<td>").append(escape(d.employee)).append("</td>")
                        .append("<td>").append(escape(d.department)).append("</td>")
                        .append("<td>").append(escape(d.category)).append("</td>")
                        .append("<td>$").append(d.amount).append("</td>")
                        .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td>")
                        .append("<td style=\"color: #64748b;\">").append(escape(d.rationale)).append("</td></tr>");
            }
            if (decisions.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"empty-state\">No requests available in the system.</td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/mileage".equals(currentPath)) {
            double totalMileageDistance = 0;
            double totalMileagePayout = 0;
            int mileageTripCount = 0;

            for (Decision d : decisions) {
                if ("Mileage".equalsIgnoreCase(d.category) && CURRENT_USER.equalsIgnoreCase(d.employee)) {
                    mileageTripCount++;
                    totalMileageDistance += d.amount;
                    totalMileagePayout += (d.amount * IRS_MILEAGE_RATE);
                }
            }

            sb.append("<div class=\"dashboard-welcome\" style=\"display:flex; justify-content:space-between; align-items:flex-end;\">\n")
                    .append("    <div>\n")
                    .append("        <h2>Vehicle Mileage & Travel Tracking</h2>\n")
                    .append("        <p>Log business travel distances, compute standard IRS mileage rates (<strong>$0.67/mi</strong>), and claim travel reimbursement.</p>\n")
                    .append("    </div>\n")
                    .append("    <a href=\"/all-requests\" class=\"action-btn\">＋ Log New Trip</a>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Trips Logged</div>\n")
                    .append("            <div class=\"metric-icon-box\">🚗</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">").append(mileageTripCount).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Business travel records</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Distance</div>\n")
                    .append("            <div class=\"metric-icon-box\">📍</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">").append(totalMileageDistance).append(" miles</div>\n")
                    .append("        <div class=\"metric-footer\">Accumulated travel span</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Reimbursement Value</div>\n")
                    .append("            <div class=\"metric-icon-box\">💰</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">$").append(String.format("%.2f", totalMileagePayout)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Calculated at $0.67 / mi rate</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">Mileage Claims Ledger</span>\n")
                    .append("        <select class=\"card-select\"><option>Standard IRS Rate ($0.67)</option></select>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr><th>Claim ID</th><th>Department</th><th>Distance</th><th>Reimbursement Value</th><th>Status</th><th>Rationale</th></tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            boolean foundMileage = false;
            for (Decision d : decisions) {
                if ("Mileage".equalsIgnoreCase(d.category) && CURRENT_USER.equalsIgnoreCase(d.employee)) {
                    foundMileage = true;
                    double payout = d.amount * IRS_MILEAGE_RATE;
                    sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                            .append("<td>").append(escape(d.department)).append("</td>")
                            .append("<td>").append(d.amount).append(" miles</td>")
                            .append("<td>$").append(String.format("%.2f", payout)).append("</td>")
                            .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td>")
                            .append("<td style=\"color: #64748b;\">").append(escape(d.rationale)).append("</td></tr>");
                }
            }
            if (!foundMileage) {
                sb.append("<tr><td colspan=\"6\" class=\"empty-state\">No mileage records found for ").append(CURRENT_USER).append(".</td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/expenses".equals(currentPath)) {
            double softwareTotal = 0;
            double hardwareTotal = 0;
            double mealsTotal = 0;

            for (Decision d : decisions) {
                if (!"Mileage".equalsIgnoreCase(d.category)) {
                    if ("Software".equalsIgnoreCase(d.category)) softwareTotal += d.amount;
                    else if ("Hardware".equalsIgnoreCase(d.category)) hardwareTotal += d.amount;
                    else if ("Meals".equalsIgnoreCase(d.category)) mealsTotal += d.amount;
                }
            }

            sb.append("<div class=\"dashboard-welcome\" style=\"display:flex; justify-content:space-between; align-items:flex-end;\">\n")
                    .append("    <div>\n")
                    .append("        <h2>Corporate Expense Portfolio</h2>\n")
                    .append("        <p>Monitor company software licenses, hardware equipment orders, travel, and operational category spending.</p>\n")
                    .append("    </div>\n")
                    .append("    <a href=\"/all-requests\" class=\"action-btn\">＋ Add Expense Item</a>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Software Spend</div>\n")
                    .append("            <div class=\"metric-icon-box\">💻</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", softwareTotal)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Licenses and subscriptions</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Hardware Spend</div>\n")
                    .append("            <div class=\"metric-icon-box\">🖥️</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", hardwareTotal)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Peripherals & equipment</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Meals & Team Spend</div>\n")
                    .append("            <div class=\"metric-icon-box\">🍽️</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", mealsTotal)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Client & internal catering</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">Categorized Expense Items</span>\n")
                    .append("        <select class=\"card-select\"><option>All Categories</option></select>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr><th>Claim ID</th><th>Employee</th><th>Category</th><th>Amount</th><th>Status</th><th>Description</th></tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            boolean foundExp = false;
            for (Decision d : decisions) {
                if (!"Mileage".equalsIgnoreCase(d.category)) {
                    foundExp = true;
                    sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                            .append("<td>").append(escape(d.employee)).append("</td>")
                            .append("<td><span class=\"badge\" style=\"background:#f1f5f9; color:#334155;\">").append(escape(d.category)).append("</span></td>")
                            .append("<td>$").append(d.amount).append("</td>")
                            .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td>")
                            .append("<td style=\"color: #64748b;\">").append(escape(d.rationale)).append("</td></tr>");
                }
            }
            if (!foundExp) {
                sb.append("<tr><td colspan=\"6\" class=\"empty-state\">No itemized expenses found.</td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/approvals".equals(currentPath)) {
            int pendingCount = 0;

            for (Decision d : decisions) {
                if (!"approve".equalsIgnoreCase(d.decision) && !"reject".equalsIgnoreCase(d.decision)) {
                    pendingCount++;
                }
            }

            sb.append("<div class=\"dashboard-welcome\">\n")
                    .append("    <h2>Managerial Approvals Queue</h2>\n")
                    .append("    <p>Review and clear escalated claims requiring human intervention under current financial compliance limits.</p>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Pending Review</div>\n")
                    .append("            <div class=\"metric-icon-box\">⏳</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #d97706;\">").append(pendingCount).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Requires manager sign-off</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Cleared / Approved</div>\n")
                    .append("            <div class=\"metric-icon-box\">✅</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">").append(approvedCountVal).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Passed policy validation</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Flagged / Rejected</div>\n")
                    .append("            <div class=\"metric-icon-box\">❌</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #ef4444;\">").append(rejectedCountVal).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Violated spending caps</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">Actionable Review Stream</span>\n")
                    .append("        <span style=\"font-size:12px; color:#64748b;\">Logged User: <strong>").append(CURRENT_USER).append("</strong></span>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr><th>Claim ID</th><th>Requester</th><th>Department</th><th>Amount</th><th>Current Status</th><th>Rule Assessment Action</th></tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            for (Decision d : decisions) {
                sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                        .append("<td>").append(escape(d.employee)).append("</td>")
                        .append("<td>").append(escape(d.department)).append("</td>")
                        .append("<td>$").append(d.amount).append("</td>")
                        .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td>")
                        .append("<td><button class=\"action-btn\" style=\"padding:4px 10px; font-size:11px;\">Override / Sign-off</button></td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/reports".equals(currentPath)) {
            sb.append("<div class=\"dashboard-welcome\">\n")
                    .append("    <h2>Financial Audit & Statement Reports</h2>\n")
                    .append("    <p>Generate period ledger summaries, tax documentation extracts, and department balance statements.</p>\n")
                    .append("</div>\n")
                    .append("<div style=\"display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; margin-bottom: 20px;\">\n")
                    .append("    <div class=\"card\" style=\"margin-bottom:0;\">\n")
                    .append("        <div class=\"card-header\"><span class=\"card-title\">Monthly Consolidated Ledger</span></div>\n")
                    .append("        <p style=\"font-size:13px; color:#64748b; margin-bottom:16px;\">Comprehensive expenditure reports structured for corporate accounting and tax filing workflows.</p>\n")
                    .append("        <a href=\"/all-requests\" class=\"action-btn\">📥 Download CSV Summary</a>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card\" style=\"margin-bottom:0;\">\n")
                    .append("        <div class=\"card-header\"><span class=\"card-title\">IRS Mileage Deduction Audit</span></div>\n")
                    .append("        <p style=\"font-size:13px; color:#64748b; margin-bottom:16px;\">Verified business vehicle trip lists calculated automatically with standard per-mile rates.</p>\n")
                    .append("        <a href=\"/mileage\" class=\"action-btn\" style=\"background:#64748b;\">View Mileage Report</a>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\"><span class=\"card-title\">Report History & Archives</span></div>\n")
                    .append("    <table>\n")
                    .append("        <thead><tr><th>Report Title</th><th>Generated Date</th><th>Scope</th><th>Format</th><th>Action</th></tr></thead>\n")
                    .append("        <tbody>\n")
                    .append("            <tr><td><strong>Q3 Corporate Expenses</strong></td><td>2026-04-01</td><td>Organization-wide</td><td>CSV / Excel</td><td><a href=\"/all-requests\" style=\"color:#10b981; font-weight:600; text-decoration:none;\">Download</a></td></tr>\n")
                    .append("            <tr><td><strong>Engineering Mileage Log</strong></td><td>2026-03-15</td><td>Engineering Dept</td><td>PDF Summary</td><td><a href=\"/all-requests\" style=\"color:#10b981; font-weight:600; text-decoration:none;\">Download</a></td></tr>\n")
                    .append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/analytics".equals(currentPath)) {
            sb.append("<div class=\"dashboard-welcome\">\n")
                    .append("    <h2>Expenditure Analytics & Trends</h2>\n")
                    .append("    <p>Analyze company-wide financial patterns, peak spending intervals, and departmental budget consumption.</p>\n")
                    .append("</div>\n")
                    .append("<div class=\"metrics-grid\" style=\"grid-template-columns: repeat(3, 1fr);\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\"><div class=\"metric-header\">Total Evaluated Volume</div><div class=\"metric-icon-box\">📊</div></div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", totalAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\">All processed claims</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\"><div class=\"metric-header\">Approved Payout Rate</div><div class=\"metric-icon-box\">📈</div></div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">")
                    .append(decisions.size() > 0 ? String.format("%.1f%%", (approvedAmount / (totalAmount == 0 ? 1 : totalAmount)) * 100) : "0%")
                    .append("</div>\n")
                    .append("        <div class=\"metric-footer\">Capital cleared ratio</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\"><div class=\"metric-header\">Active Rules Applied</div><div class=\"metric-icon-box\">⚙️</div></div>\n")
                    .append("        <div class=\"metric-value\">").append(rules.size()).append("</div>\n")
                    .append("        <div class=\"metric-footer\">Policy rule engine filters</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\"><span class=\"card-title\">Departmental Spending Breakdown</span></div>\n")
                    .append("    <table>\n")
                    .append("        <thead><tr><th>Department</th><th>Active Claims</th><th>Total Volume</th><th>Compliance Rating</th></tr></thead>\n")
                    .append("        <tbody>\n")
                    .append("            <tr><td><strong>Engineering</strong></td><td>3</td><td>$640.00</td><td><span class=\"badge approve\">High Compliance</span></td></tr>\n")
                    .append("            <tr><td><strong>Product</strong></td><td>1</td><td>$1,250.00</td><td><span class=\"badge approve\">Standard</span></td></tr>\n")
                    .append("            <tr><td><strong>Sales</strong></td><td>1</td><td>$1,550.00</td><td><span class=\"badge escalate\">Review Required</span></td></tr>\n")
                    .append("            <tr><td><strong>Marketing</strong></td><td>1</td><td>$85.50</td><td><span class=\"badge reject\">Flagged</span></td></tr>\n")
                    .append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        } else if ("/settings".equals(currentPath)) {
            sb.append("<div class=\"dashboard-welcome\">\n")
                    .append("    <h2>System Settings & Preferences</h2>\n")
                    .append("    <p>Configure user profile credentials, notification channels, and automated policy integration parameters.</p>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\"><span class=\"card-title\">Active User Profile</span></div>\n")
                    .append("    <div style=\"display: flex; flex-direction: column; gap: 12px; font-size: 13px; color: #334155;\">\n")
                    .append("        <div><strong>Name:</strong> ").append(CURRENT_USER).append("</div>\n")
                    .append("        <div><strong>Role:</strong> Finance Manager</div>\n")
                    .append("        <div><strong>System Authority:</strong> Full Administrative & Approval Override</div>\n")
                    .append("        <div><strong>Data Sources:</strong> <code>claims.csv</code> &amp; <code>rules.txt</code></div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\"><span class=\"card-title\">Application Parameters</span></div>\n")
                    .append("    <div style=\"display: flex; flex-direction: column; gap: 10px; font-size: 13px; color: #64748b;\">\n")
                    .append("        <div>⚡ Server Port: <strong>8080</strong></div>\n")
                    .append("        <div>🚗 IRS Standard Mileage Rate: <strong>$0.67 per mile</strong></div>\n")
                    .append("        <div>🔒 Authentication Status: <strong>Session Active</strong></div>\n")
                    .append("    </div>\n")
                    .append("</div>\n");
        } else {
            // UPDATED DASHBOARD VIEW MATCHING REFERENCE DESIGN
            sb.append("<div class=\"dashboard-welcome\">\n")
                    .append("    <h2>Expense Approval Dashboard</h2>\n")
                    .append("    <p>Welcome back, Olivia Rhye</p>\n")
                    .append("</div>\n")
                    // 1. Top 5 Metric Cards
                    .append("<div class=\"metrics-grid\">\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Pending Approvals</div>\n")
                    .append("            <div class=\"metric-icon-box\">📄</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #d97706;\">").append(pendingTotal).append("</div>\n")
                    .append("        <div class=\"metric-footer\"><span>&#8593;</span> 12% from last week</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Requests</div>\n")
                    .append("            <div class=\"metric-icon-box\">📋</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">").append(totalRequests).append("</div>\n")
                    .append("        <div class=\"metric-footer\"><span>&#8593;</span> 8% from last week</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Total Amount</div>\n")
                    .append("            <div class=\"metric-icon-box\">💲</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\">$").append(String.format("%.2f", totalAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\"><span>&#8593;</span> Active volume</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Approved Amount</div>\n")
                    .append("            <div class=\"metric-icon-box\">✅</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #10b981;\">$").append(String.format("%.2f", approvedAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\" style=\"color: #10b981;\"><span>&#8593;</span> Auto-cleared</div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"metric-card\">\n")
                    .append("        <div class=\"metric-card-top\">\n")
                    .append("            <div class=\"metric-header\">Rejected Amount</div>\n")
                    .append("            <div class=\"metric-icon-box\">❌</div>\n")
                    .append("        </div>\n")
                    .append("        <div class=\"metric-value\" style=\"color: #ef4444;\">$").append(String.format("%.2f", rejectedAmount)).append("</div>\n")
                    .append("        <div class=\"metric-footer\" style=\"color: #ef4444;\"><span>&#8595;</span> Policy violations</div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    // 2. Middle Row: Expense Overview & Requests by Status Cards
                    .append("<div class=\"dashboard-row-2\">\n")
                    .append("    <div class=\"card\" style=\"margin-bottom:0; display:flex; flex-direction:column; justify-content:space-between;\">\n")
                    .append("        <div class=\"card-header\">\n")
                    .append("            <span class=\"card-title\">Expense Overview</span>\n")
                    .append("            <select class=\"card-select\"><option>This Month</option></select>\n")
                    .append("        </div>\n")
                    .append("        <div style=\"height: 180px; display: flex; align-items: flex-end; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #e2e8f0; position: relative;\">\n")
                    .append("            <div style=\"position:absolute; top:40px; left:45%; background:#ffffff; border:1px solid #e2e8f0; padding:6px 12px; border-radius:8px; font-size:11px; box-shadow:0 2px 4px rgba(0,0,0,0.05); text-align:center;\">\n")
                    .append("                <div style=\"color:#64748b; font-size:9px;\">May 18, 2024</div>\n")
                    .append("                <div style=\"font-weight:700; color:#0f172a;\">$48,750.50</div>\n")
                    .append("            </div>\n")
                    .append("            <svg style=\"width:100%; height:120px; overflow:visible;\" viewBox=\"0 0 500 120\">\n")
                    .append("                <path d=\"M 0 100 Q 80 80, 160 90 T 320 50 T 480 20\" fill=\"none\" stroke=\"#10b981\" stroke-width=\"3\" />\n")
                    .append("                <circle cx=\"320\" cy=\"50\" r=\"5\" fill=\"#10b981\" stroke=\"#ffffff\" stroke-width=\"2\" />\n")
                    .append("            </svg>\n")
                    .append("        </div>\n")
                    .append("        <div style=\"display:flex; justify-content:space-between; font-size:11px; color:#64748b; padding-top:10px;\">\n")
                    .append("            <span>May 01</span><span>May 08</span><span>May 15</span><span>May 22</span><span>May 28</span>\n")
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <div class=\"card\" style=\"margin-bottom:0; display:flex; flex-direction:column; justify-content:space-between;\">\n")
                    .append("        <div class=\"card-header\">\n")
                    .append("            <span class=\"card-title\">Requests by Status</span>\n")
                    .append("        </div>\n")
                    .append("        <div style=\"display:flex; align-items:center; justify-content:space-around; padding: 10px 0;\">\n")
                    .append("            <div style=\"width:110px; height:110px; border-radius:50%; background:conic-gradient(#d97706 0% 30%, #3b82f6 30% 55%, #10b981 55% 85%, #ef4444 85% 100%); display:flex; align-items:center; justify-content:center;\">\n")
                    .append("                <div style=\"width:75px; height:75px; background:#ffffff; border-radius:50%; display:flex; flex-direction:column; align-items:center; justify-content:center;\">\n")
                    .append("                    <span style=\"font-size:14px; font-weight:700;\">").append(totalRequests).append("</span>\n")
                    .append("                    <span style=\"font-size:9px; color:#64748b;\">Total</span>\n")
                    .append("                </div>\n")
                    .append("            </div>\n")
                    .append("            <div style=\"display:flex; flex-direction:column; gap:6px; font-size:12px; color:#334155;\">\n")
                    .append("                <div style=\"display:flex; align-items:center; gap:6px;\"><span style=\"width:8px; height:8px; border-radius:50%; background:#d97706;\"></span> Pending (30%)</div>\n")
                    .append("                <div style=\"display:flex; align-items:center; gap:6px;\"><span style=\"width:8px; height:8px; border-radius:50%; background:#3b82f6;\"></span> In Review (25%)</div>\n")
                    .append("                <div style=\"display:flex; align-items:center; gap:6px;\"><span style=\"width:8px; height:8px; border-radius:50%; background:#10b981;\"></span> Approved (30%)</div>\n")
                    .append("                <div style=\"display:flex; align-items:center; gap:6px;\"><span style=\"width:8px; height:8px; border-radius:50%; background:#ef4444;\"></span> Rejected (15%)</div>\n")
                    .append("            </div>\n")
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("</div>\n")
                    // 3. Bottom Pending Approvals Table Section
                    .append("<div class=\"card\">\n")
                    .append("    <div class=\"card-header\">\n")
                    .append("        <span class=\"card-title\">Pending Approvals</span>\n")
                    .append("        <div style=\"display:flex; gap:10px;\">\n")
                    .append("            <select class=\"card-select\"><option>All Types</option></select>\n")
                    .append("            <select class=\"card-select\"><option>All Departments</option></select>\n")
                    .append("        </div>\n")
                    .append("    </div>\n")
                    .append("    <table>\n")
                    .append("        <thead>\n")
                    .append("            <tr><th>Request ID</th><th>Employee</th><th>Type</th><th>Date</th><th>Amount</th><th>Department</th><th>Status Action</th></tr>\n")
                    .append("        </thead>\n")
                    .append("        <tbody>\n");

            for (Decision d : decisions) {
                sb.append("<tr><td><strong>").append(escape(d.claimId)).append("</strong></td>")
                        .append("<td style=\"display:flex; align-items:center; gap:8px;\"><div style=\"width:24px; height:24px; border-radius:50%; background:#10b981; color:#fff; font-size:10px; display:flex; align-items:center; justify-content:center;\">").append(escape(d.employee).substring(0, Math.min(2, d.employee.length()))).append("</div>").append(escape(d.employee)).append("</td>")
                        .append("<td>").append(escape(d.category)).append("</td>")
                        .append("<td>May 15, 2024</td>")
                        .append("<td>$").append(d.amount).append("</td>")
                        .append("<td>").append(escape(d.department)).append("</td>")
                        .append("<td><span class=\"badge ").append(d.decision.toLowerCase()).append("\">").append(d.decision).append("</span></td></tr>");
            }
            if (decisions.isEmpty()) {
                sb.append("<tr><td colspan=\"7\" class=\"empty-state\">No pending approvals recorded.</td></tr>");
            }
            sb.append("        </tbody>\n")
                    .append("    </table>\n")
                    .append("</div>\n");
        }

        sb.append("        </div>\n")
                .append("    </main>\n")
                .append("</div>\n")
                .append("</body>\n")
                .append("</html>\n");

        return sb.toString();
    }
}