package agent;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a plain-English policy rule (a line of text) into a structured
 * Rule object the engine can evaluate deterministically.
 *
 * Design choice: instead of calling an LLM at decision time (which would
 * make approvals non-deterministic and hard to demo/test offline), this
 * uses a small, transparent set of regex + keyword extraction rules. Every
 * extraction step is visible and testable, and the raw rule text is always
 * kept attached so the engine can cite it verbatim (traceability).
 */
public class RuleParser {

    private static final String[] KNOWN_DEPARTMENTS = {
            "Sales", "Marketing", "Engineering", "Finance",
            "HR", "Operations", "IT", "Legal", "Support"
    };

    private static final String[] KNOWN_CATEGORIES = {
            "travel", "meals", "software", "equipment",
            "client_dinner", "advertising", "other"
    };

    private static final Pattern BETWEEN = Pattern.compile(
            "between\\s+\\$?([\\d,]+(?:\\.\\d+)?)\\s+and\\s+\\$?([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern BELOW = Pattern.compile(
            "(?:under|below|less than)\\s+\\$?([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ABOVE = Pattern.compile(
            "(?:above|over|more than|exceeding|greater than)\\s+\\$?([\\d,]+(?:\\.\\d+)?)",
            Pattern.CASE_INSENSITIVE);

    public static Rule parseRule(String rawText) {
        String text = rawText.trim();
        String action = extractAction(text);

        Rule.Op op = Rule.Op.NONE;
        double low = 0, high = 0;

        Matcher m = BETWEEN.matcher(text);
        if (m.find()) {
            op = Rule.Op.BETWEEN;
            low = parseNum(m.group(1));
            high = parseNum(m.group(2));
        } else {
            m = BELOW.matcher(text);
            if (m.find()) {
                op = Rule.Op.BELOW;
                low = parseNum(m.group(1));
            } else {
                m = ABOVE.matcher(text);
                if (m.find()) {
                    op = Rule.Op.ABOVE;
                    low = parseNum(m.group(1));
                }
            }
        }

        String department = extractDepartment(text);
        String category = extractCategory(text);
        boolean requiresNoJustification = text.toLowerCase().contains("without justification");

        return new Rule(text, action, op, low, high, department, category, requiresNoJustification);
    }

    public static List<Rule> parseRules(List<String> rawLines) {
        List<Rule> rules = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            rules.add(parseRule(trimmed));
        }
        // Most specific rules are checked first: a narrow rule (department +
        // category + amount) always beats a broad catch-all rule.
        rules.sort((a, b) -> Integer.compare(b.specificity, a.specificity));
        return rules;
    }

    private static double parseNum(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }

    private static String extractAction(String text) {
        String t = text.toLowerCase();
        if (t.matches(".*\\bauto[- ]approve\\b.*")) return "approve";
        if (t.matches(".*\\bapprove\\b.*")) return "approve";
        if (t.matches(".*\\breject\\b.*")) return "reject";
        if (t.matches(".*\\bdeny\\b.*")) return "reject";
        if (t.matches(".*\\bescalate\\b.*")) return "escalate";
        // No recognizable action verb -> safest default is escalate.
        return "escalate";
    }

    private static String extractDepartment(String text) {
        String t = text.toLowerCase();
        if (t.contains("regardless of department") || t.contains("all departments") || t.contains("any department")) {
            return null;
        }
        for (String dept : KNOWN_DEPARTMENTS) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(dept) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(text).find()) {
                return dept;
            }
        }
        return null;
    }

    private static String extractCategory(String text) {
        String t = text.toLowerCase();
        for (String cat : KNOWN_CATEGORIES) {
            String spaced = cat.replace("_", " ");
            if (t.contains(spaced) || t.contains(cat)) {
                return cat;
            }
        }
        return null;
    }

    private RuleParser() {}
}
