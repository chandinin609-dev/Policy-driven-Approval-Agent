package agent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * CLI entry point.
 *
 * Usage:
 *   java -cp out agent.Main
 *   java -cp out agent.Main rules.txt claims.csv decisions.csv
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String rulesPath = args.length > 0 ? args[0] : "rules.txt";
        String claimsPath = args.length > 1 ? args[1] : "claims.csv";
        String outPath = args.length > 2 ? args[2] : "decisions.csv";

        List<String> rawRuleLines = Files.readAllLines(Paths.get(rulesPath), StandardCharsets.UTF_8);
        List<Rule> rules = RuleParser.parseRules(rawRuleLines);

        List<Claim> claims = loadClaims(claimsPath);
        List<Decision> decisions = ApprovalEngine.evaluateBatch(claims, rules);

        printTable(decisions);
        saveCsv(decisions, outPath);
        System.out.println("\nSaved " + decisions.size() + " decisions to " + outPath);
    }

    private static List<Claim> loadClaims(String path) throws IOException {
        List<Claim> claims = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        if (lines.isEmpty()) return claims;

        String[] header = splitCsvLine(lines.get(0));
        Map<String, Integer> col = new HashMap<>();
        for (int i = 0; i < header.length; i++) col.put(header[i].trim(), i);

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.trim().isEmpty()) continue;
            String[] f = splitCsvLine(line);

            String hj = get(f, col, "has_justification", "yes").trim().toLowerCase();
            boolean hasJustification = hj.equals("yes") || hj.equals("true") || hj.equals("1");

            claims.add(new Claim(
                    get(f, col, "claim_id", ""),
                    get(f, col, "employee", ""),
                    get(f, col, "department", ""),
                    get(f, col, "category", ""),
                    Double.parseDouble(get(f, col, "amount", "0")),
                    get(f, col, "description", ""),
                    hasJustification
            ));
        }
        return claims;
    }

    private static String get(String[] fields, Map<String, Integer> col, String name, String defaultVal) {
        Integer idx = col.get(name);
        if (idx == null || idx >= fields.length) return defaultVal;
        return fields[idx];
    }

    // Minimal CSV splitter: handles simple comma-separated values (no
    // embedded commas in our sample data, so no quoting logic needed).
    private static String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private static void printTable(List<Decision> decisions) {
        String header = String.format("%-6s %-14s %-12s %10s  %-9s %s",
                "ID", "Employee", "Dept", "Amount", "Decision", "Rationale");
        System.out.println(header);
        System.out.println("-".repeat(header.length()));
        for (Decision d : decisions) {
            System.out.printf("%-6s %-14s %-12s $%9.2f  %-9s %s%n",
                    d.claimId, d.employee, d.department, d.amount,
                    d.decision.toUpperCase(), d.rationale);
        }
    }

    private static void saveCsv(List<Decision> decisions, String outPath) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(Paths.get(outPath), StandardCharsets.UTF_8)) {
            w.write("claim_id,employee,department,category,amount,decision,matched_rule,rationale");
            w.newLine();
            for (Decision d : decisions) {
                w.write(String.join(",",
                        csvEscape(d.claimId), csvEscape(d.employee), csvEscape(d.department),
                        csvEscape(d.category), String.valueOf(d.amount), csvEscape(d.decision),
                        csvEscape(d.matchedRule == null ? "" : d.matchedRule), csvEscape(d.rationale)
                ));
                w.newLine();
            }
        }
    }

    private static String csvEscape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
