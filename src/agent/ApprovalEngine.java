package agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the parsed policy rules to a batch of claims and produces a
 * decision + rationale for every claim, always citing the exact rule text
 * that was applied (or explaining the default fallback).
 */
public class ApprovalEngine {

    public static Decision evaluateClaim(Claim claim, List<Rule> rules) {
        for (Rule rule : rules) {
            if (matches(rule, claim)) {
                String rationale = String.format(
                        "Matched rule: \"%s\" (conditions: %s). Claim amount $%s, department %s, category %s.",
                        rule.rawText, rule.describeConditions(), fmt(claim.amount),
                        claim.department, claim.category
                );
                return new Decision(claim.claimId, claim.employee, claim.department, claim.category,
                        claim.amount, rule.action, rule.rawText, rationale);
            }
        }

        // No rule matched -> fail-safe: escalate for human review. Never
        // silently approve or reject an ambiguous / unanticipated case.
        String rationale = "No configured rule matched this claim. Escalated to a human reviewer " +
                "by default policy (fail-safe: ambiguous cases are never auto-approved or auto-rejected).";
        return new Decision(claim.claimId, claim.employee, claim.department, claim.category,
                claim.amount, "escalate", null, rationale);
    }

    public static List<Decision> evaluateBatch(List<Claim> claims, List<Rule> rules) {
        List<Decision> decisions = new ArrayList<>();
        for (Claim c : claims) {
            decisions.add(evaluateClaim(c, rules));
        }
        return decisions;
    }

    private static boolean matches(Rule rule, Claim claim) {
        if (rule.department != null && !rule.department.equalsIgnoreCase(claim.department)) {
            return false;
        }
        if (rule.category != null && !rule.category.equalsIgnoreCase(claim.category)) {
            return false;
        }
        switch (rule.amountOp) {
            case BELOW -> { if (!(claim.amount < rule.amountLow)) return false; }
            case ABOVE -> { if (!(claim.amount > rule.amountLow)) return false; }
            case BETWEEN -> { if (!(claim.amount >= rule.amountLow && claim.amount <= rule.amountHigh)) return false; }
            case NONE -> { /* no amount constraint */ }
        }
        // Rule specifically targets claims lacking justification; if this
        // claim HAS justification, this particular rule does not apply.
        if (rule.requiresNoJustification && claim.hasJustification) {
            return false;
        }
        return true;
    }

    private static String fmt(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.valueOf(d);
    }

    private ApprovalEngine() {}
}
