package pharmasync.models;

import java.io.Serializable;

public abstract class Medicine implements Serializable {
    private static final long serialVersionUID = 1L;

    private String medicineID;
    private String name;
    private Double price; // Wrapper class used per week 2
    private Integer stockQuantity; // Wrapper class used per week 2
    private DrugCategory category;

    public Medicine(String medicineID, String name, Double price, Integer stockQuantity, DrugCategory category) {
        this.medicineID = medicineID;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.category = category;
    }

    // Encapsulation: Getters and Setters
    public String getMedicineID() { return medicineID; }
    public void setMedicineID(String medicineID) { this.medicineID = medicineID; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer stockQuantity) { this.stockQuantity = stockQuantity; }

    public DrugCategory getCategory() { return category; }
    public void setCategory(DrugCategory category) { this.category = category; }

    // Polymorphism requirement
    public abstract Double calculateDiscount();

    @Override
    public String toString() {
        return name + " (" + medicineID + ") - Stock: " + stockQuantity;
    }
}
