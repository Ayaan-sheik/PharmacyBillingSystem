package pharmasync.models;

import java.time.LocalDate;

public class OverTheCounterDrug extends Medicine {
    private boolean isAgeRestricted;

    public OverTheCounterDrug(String medicineID, String name, Double price, Integer stockQuantity, DrugCategory category, LocalDate expiryDate, boolean isAgeRestricted) {
        super(medicineID, name, price, stockQuantity, category, expiryDate);
        this.isAgeRestricted = isAgeRestricted;
    }

    public boolean isAgeRestricted() { return isAgeRestricted; }
    public void setAgeRestricted(boolean ageRestricted) { this.isAgeRestricted = ageRestricted; }

    @Override
    public Double calculateDiscount() {
        // Over the counter drugs get a generic 10% standard discount
        return this.getPrice() * 0.10;
    }
}
