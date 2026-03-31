package pharmasync.models;

public class PrescriptionDrug extends Medicine {
    private String prescribedDoctor;

    public PrescriptionDrug(String medicineID, String name, Double price, Integer stockQuantity, DrugCategory category, String prescribedDoctor) {
        super(medicineID, name, price, stockQuantity, category);
        this.prescribedDoctor = prescribedDoctor;
    }

    public String getPrescribedDoctor() { return prescribedDoctor; }
    public void setPrescribedDoctor(String prescribedDoctor) { this.prescribedDoctor = prescribedDoctor; }

    @Override
    public Double calculateDiscount() {
        // Strict prescription drugs have a tiny base discount (e.g. 2%)
        return this.getPrice() * 0.02;
    }
}
