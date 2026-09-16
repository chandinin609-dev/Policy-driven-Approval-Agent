package agent;
public class Claim {
    public final String claimId;
    public final String employee;
    public final String department;
    public final String category;
    public final double amount;
    public final String description;
    public final boolean hasJustification;

    public Claim(String claimId, String employee, String department, String category,
                 double amount, String description, boolean hasJustification) {
        this.claimId = claimId;
        this.employee = employee;
        this.department = department;
        this.category = category;
        this.amount = amount;
        this.description = description;
        this.hasJustification = hasJustification;
    }
}
