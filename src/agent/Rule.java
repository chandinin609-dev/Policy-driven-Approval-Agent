package agent;

/**
 * A structured, evaluable representation of one plain-English policy rule.
 * The rawText is always kept so the engine can cite it verbatim in a
 * decision's rationale (traceability requirement).
 */
public class Rule {

    public enum Op { BELOW, ABOVE, BETWEEN, NONE }

    public final String rawText;
    public final String action;          // "approve" | "reject" | "escalate"
    public final Op amountOp;
    public final double amountLow;
    public final double amountHigh;
    public final String department;      // null = applies to all departments
    public final String category;        // null = applies to all categories
    public final boolean requiresNoJustification; // rule mentions "without justification"
    public final int specificity;        // more constraints = higher priority

    public Rule(String rawText, String action, Op amountOp, double amountLow, double amountHigh,
                String department, String category, boolean requiresNoJustification) {
        this.rawText = rawText;
        this.action = action;
        this.amountOp = amountOp;
        this.amountLow = amountLow;
        this.amountHigh = amountHigh;
        this.department = department;
        this.category = category;
        this.requiresNoJustification = requiresNoJustification;

        int spec = 0;
        if (amountOp != Op.NONE) spec++;
        if (department != null) spec++;
        if (category != null) spec++;
        if (requiresNoJustification) spec++;
        this.specificity = spec;
    }

    public String describeConditions() {
        StringBuilder sb = new StringBuilder();
        switch (amountOp) {
            case BELOW -> sb.append("amount < $").append(fmt(amountLow));
            case ABOVE -> sb.append("amount > $").append(fmt(amountLow));
            case BETWEEN -> sb.append("$").append(fmt(amountLow)).append(" <= amount <= $").append(fmt(amountHigh));
            default -> {}
        }
        if (department != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("department == ").append(department);
        }
        if (category != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append("category == ").append(category);
        }
        if (sb.length() == 0) sb.append("always (catch-all)");
        return sb.toString();
    }

    private String fmt(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }
}
