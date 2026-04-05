package pharmasync.models;

public enum DrugCategory {
    TABLET(0.05),      // 5% tax
    SYRUP(0.08),       // 8% tax
    INJECTION(0.12),   // 12% tax
    TOPICAL(0.06);     // 6% tax

    private final double baseTaxRate;

    DrugCategory(double baseTaxRate) {
        this.baseTaxRate = baseTaxRate;
    }

    public double getBaseTaxRate() {
        return baseTaxRate;
    }
}
