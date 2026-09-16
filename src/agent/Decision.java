package agent;
public class Decision {
    public final String claimId;
    public final String employee;
    public final String department;
    public final String category;
    public final double amount;
    public final String decision;      // approve | reject | escalate
    public final String matchedRule;   // null if default fallback
    public final String rationale;

    public Decision(String claimId, String employee, String department, String category,
                     double amount, String decision, String matchedRule, String rationale) {
        this.claimId = claimId;
        this.employee = employee;
        this.department = department;
        this.category = category;
        this.amount = amount;
        this.decision = decision;
        this.matchedRule = matchedRule;
        this.rationale = rationale;
    }
}
