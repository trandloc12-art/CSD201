package warehouse.model;

import java.time.LocalDate;

/** A batch (lot) of one product (Table 8). */
public class Batch {
    private final String batchId;
    private final LocalDate importDate;
    private final LocalDate expiryDate;
    private int quantity;

    public Batch(String batchId, LocalDate importDate, LocalDate expiryDate, int quantity) {
        this.batchId = batchId;
        this.importDate = importDate;
        this.expiryDate = expiryDate;
        this.quantity = quantity;
    }

    public String getBatchId() { return batchId; }
    public LocalDate getImportDate() { return importDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    @Override
    public String toString() { return batchId + "(" + quantity + ", HSD " + expiryDate + ")"; }
}
