import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Điều phối nghiệp vụ (Mục V.A 2.2, 3.2, 4.1, 5.1). Quy ước hết hạn: expiryDate < currentDate. */
public class Inventory {
    private final ProductBST productBST = new ProductBST();

    public ProductBST getProductBST() { return productBST; }

    /** Mục 2.2 - O(h). */
    public boolean receiveBatch(String barcode, Batch batch) {
        if (barcode == null || barcode.isEmpty() || batch == null) return false;
        if (batch.getQuantity() <= 0) return false;
        if (!batch.getExpiryDate().isAfter(batch.getImportDate())) return false;
        BSTNode node = productBST.search(barcode);
        if (node == null) return false;
        node.getBatchQueue().enqueue(batch);
        return true;
    }

    /**
     * Mục 3.2 - O(h + k). Kết quả chi tiết trả qua issuedList ("batchId:số lượng")
     * và expiredList (lô hết hạn đã bị loại). Từ chối thì hàng đợi giữ nguyên.
     */
    public boolean issueStock(String barcode, int quantity, LocalDate currentDate,
                              List<String> issuedList, List<String> expiredList) {
        if (barcode == null || barcode.isEmpty() || quantity <= 0) return false;
        BSTNode node = productBST.search(barcode);
        if (node == null) return false;
        BatchQueue queue = node.getBatchQueue();

        // Giai đoạn kiểm tra (chỉ đọc)
        long available = 0;
        for (Batch b : queue.toList()) {
            if (!b.getExpiryDate().isBefore(currentDate)) available += b.getQuantity();
        }
        if (available < quantity) return false;

        // Giai đoạn xuất
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

    /** Mục 4.1 - Θ(n + B). Mỗi phần tử: "barcode|batchId|quantity|trạng thái". */
    public List<String> checkExpiry(LocalDate currentDate, int K) {
        List<String> expiryList = new ArrayList<>();
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

    /** Mục 5.1 - Θ(n + B). Mỗi dòng: "barcode|name|unit|minStock|tổng SL|số lô|HSD sớm nhất". */
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
