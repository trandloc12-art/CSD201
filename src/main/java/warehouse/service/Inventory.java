package warehouse.service;

import warehouse.model.Batch;
import warehouse.model.Product;
import warehouse.structure.BSTNode;
import warehouse.structure.BatchQueue;
import warehouse.structure.ProductBST;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Business-logic coordinator (pseudocode V.A 2.2, 3.2, 4.1, 5.1). Expiry rule: a batch is expired only if expiryDate < currentDate. */
public class Inventory {
    private final ProductBST productBST = new ProductBST();

    public ProductBST getProductBST() { return productBST; }

    /** Pseudocode 2.2 - O(h). */
    public boolean receiveBatch(String barcode, Batch batch) {
        if (barcode == null || barcode.isEmpty() || batch == null) return false;
        if (batch.getImportDate() == null || batch.getExpiryDate() == null) return false;
        if (batch.getQuantity() <= 0) return false;
        if (!batch.getExpiryDate().isAfter(batch.getImportDate())) return false;
        BSTNode node = productBST.search(barcode);
        if (node == null) return false;
        node.getBatchQueue().enqueue(batch);
        return true;
    }

    /**
     * Pseudocode 3.2 - O(h + k). Details are returned through issuedList ("batchId:quantity")
     * and expiredList (expired batches that were discarded). If the request is rejected the queue is left unchanged.
     */
    public boolean issueStock(String barcode, int quantity, LocalDate currentDate,
                              List<String> issuedList, List<String> expiredList) {
        if (barcode == null || barcode.isEmpty() || quantity <= 0 || currentDate == null) return false;
        BSTNode node = productBST.search(barcode);
        if (node == null) return false;
        BatchQueue queue = node.getBatchQueue();

        // Check phase (read-only)
        long available = 0;
        for (Batch b : queue.toList()) {
            if (!b.getExpiryDate().isBefore(currentDate)) available += b.getQuantity();
        }
        if (available < quantity) return false;

        // Issue phase
        int remaining = quantity;
        while (remaining > 0) {
            Batch b = queue.peek();
            if (b.getExpiryDate().isBefore(currentDate)) {
                queue.dequeue();
                if (expiredList != null) expiredList.add(b.getBatchId() + ":" + b.getQuantity());
                continue;
            }
            if (b.getQuantity() <= remaining) {
                remaining -= b.getQuantity();
                if (issuedList != null) issuedList.add(b.getBatchId() + ":" + b.getQuantity());
                queue.dequeue();
            } else {
                b.setQuantity(b.getQuantity() - remaining);
                if (issuedList != null) issuedList.add(b.getBatchId() + ":" + remaining);
                remaining = 0;
            }
        }
        return true;
    }

    /** Pseudocode 4.1 - Θ(n + B). Each element: "barcode|batchId|quantity|status". Returns an empty list if currentDate is null. */
    public List<String> checkExpiry(LocalDate currentDate, int K) {
        List<String> expiryList = new ArrayList<>();
        if (currentDate == null) return expiryList;
        LocalDate limit = currentDate.plusDays(K);
        for (BSTNode node : productBST.inOrderTraversal()) {
            for (Batch b : node.getBatchQueue().toList()) {
                if (b.getExpiryDate().isBefore(currentDate)) {
                    expiryList.add(node.getProduct().getBarcode() + "|" + b.getBatchId() + "|" + b.getQuantity() + "|EXPIRED");
                } else if (!b.getExpiryDate().isAfter(limit)) {
                    expiryList.add(node.getProduct().getBarcode() + "|" + b.getBatchId() + "|" + b.getQuantity() + "|NEAR_EXPIRY");
                }
            }
        }
        return expiryList;
    }

    /** Pseudocode 5.1 - Θ(n + B). Each line: "barcode|name|unit|minStock|total quantity|batch count|earliest expiry date". */
    public List<String> generateInventoryReport() {
        List<String> report = new ArrayList<>();
        for (BSTNode node : productBST.inOrderTraversal()) {
            Product p = node.getProduct();
            long total = 0;
            int count = 0;
            LocalDate earliest = null;
            for (Batch b : node.getBatchQueue().toList()) {
                total += b.getQuantity();
                count++;
                if (earliest == null || b.getExpiryDate().isBefore(earliest)) earliest = b.getExpiryDate();
            }
            report.add(p.getBarcode() + "|" + p.getName() + "|" + p.getUnit() + "|" + p.getMinStock()
                    + "|" + total + "|" + count + "|" + (earliest == null ? "-" : earliest.toString()));
        }
        return report;
    }
}
