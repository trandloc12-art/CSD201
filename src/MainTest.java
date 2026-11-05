import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Kiểm thử tái hiện các Trace của Mục V.B (ngày dd/MM năm 2026). */
public class MainTest {
    static int pass = 0, fail = 0;

    static void check(String name, boolean ok) {
        if (ok) pass++; else fail++;
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name);
    }

    static LocalDate d(int day, int month) { return LocalDate.of(2026, month, day); }

    static Inventory milkInventory() {
        Inventory inv = new Inventory();
        inv.getProductBST().insert(new Product("8930001", "Sua tuoi", "hop", 20));
        inv.receiveBatch("8930001", new Batch("B01", d(1, 9), d(1, 10), 50));
        inv.receiveBatch("8930001", new Batch("B02", d(5, 9), d(5, 10), 60));
        inv.receiveBatch("8930001", new Batch("B03", d(10, 9), d(10, 10), 40));
        return inv;
    }

    public static void main(String[] args) {
        // Trace 1 - BST
        ProductBST t = new ProductBST();
        check("T1 insert BC-04/02/06/05", t.insert(new Product("BC-04", "a", "u", 0)) && t.insert(new Product("BC-02", "b", "u", 0))
                && t.insert(new Product("BC-06", "c", "u", 0)) && t.insert(new Product("BC-05", "d", "u", 0)));
        check("T1 insert trùng barcode bị từ chối", !t.insert(new Product("BC-05", "x", "u", 0)));
        check("T1 search BC-05 / BC-09", t.search("BC-05") != null && t.search("BC-09") == null);
        check("T1 delete BC-04 (2 con) -> root = BC-05", t.delete("BC-04") && t.getRoot().product.getBarcode().equals("BC-05")
                && t.getRoot().left.product.getBarcode().equals("BC-02") && t.getRoot().right.product.getBarcode().equals("BC-06")
                && t.getRoot().right.left == null);
        check("T1 inOrder tăng dần", t.inOrderTraversal().size() == 3
                && t.inOrderTraversal().get(0).product.getBarcode().equals("BC-02"));
        // Tiền điều kiện Delete (xử lý điểm 5)
        t.search("BC-06").batchQueue.enqueue(new Batch("X", d(1, 9), d(1, 10), 5));
        check("Delete bị từ chối khi hàng đợi còn lô; cây không đổi", !t.delete("BC-06") && t.search("BC-06") != null);
        check("Delete barcode không tồn tại trả về false", !t.delete("BC-99"));

        // Trace 2 / 2.1 / 2.2 - BatchQueue
        BatchQueue q = new BatchQueue();
        check("T2.2 queue rỗng: peek/dequeue = null, isEmpty = TRUE, size = 0",
                q.peek() == null && q.dequeue() == null && q.isEmpty() && q.size() == 0);
        q.enqueue(new Batch("B01", d(1, 9), d(1, 10), 1));
        q.enqueue(new Batch("B02", d(2, 9), d(2, 10), 1));
        q.enqueue(new Batch("B03", d(3, 9), d(3, 10), 1));
        check("T2 enqueue B01,B02,B03 -> size 3, toList theo thứ tự", q.size() == 3 && q.toList().get(0).getBatchId().equals("B01")
                && q.toList().get(2).getBatchId().equals("B03") && q.size() == 3);
        check("T2.1 dequeue theo FIFO B01,B02,B03", q.dequeue().getBatchId().equals("B01") && q.dequeue().getBatchId().equals("B02")
                && q.dequeue().getBatchId().equals("B03") && q.isEmpty());

        // Trace 3.1 - ReceiveBatch
        Inventory inv = milkInventory();
        check("T3.1 receiveBatch B04 hợp lệ", inv.receiveBatch("8930001", new Batch("B04", d(14, 9), d(14, 10), 80))
                && inv.getProductBST().search("8930001").batchQueue.size() == 4);
        check("T3.1 B05 quantity = 0 bị từ chối", !inv.receiveBatch("8930001", new Batch("B05", d(14, 9), d(14, 10), 0)));
        check("T3.1 barcode không tồn tại bị từ chối", !inv.receiveBatch("8930999", new Batch("B06", d(14, 9), d(14, 10), 10)));
        check("T3.1 HSD <= ngày nhập bị từ chối", !inv.receiveBatch("8930001", new Batch("B07", d(14, 9), d(14, 9), 10)));
        check("T3.1 hàng đợi vẫn 4 lô", inv.getProductBST().search("8930001").batchQueue.size() == 4);

        // Trace 3.2 - xuất qua nhiều lô
        List<String> issued = new ArrayList<>(), expired = new ArrayList<>();
        check("T3.2 issueStock 70 tại 30/09", inv.issueStock("8930001", 70, d(30, 9), issued, expired));
        check("T3.2 issuedList = [B01:50, B02:20]", issued.toString().equals("[B01:50, B02:20]") && expired.isEmpty());
        BatchQueue mq = inv.getProductBST().search("8930001").batchQueue;
        check("T3.2 trạng thái: B02(40) B03(40) B04(80)", mq.size() == 3 && mq.peek().getBatchId().equals("B02") && mq.peek().getQuantity() == 40);

        // Trace 3.3 - vượt tồn / hết hạn hết / dữ liệu sai
        check("T3.3 xuất 500 > tồn 160 bị từ chối, hàng đợi giữ nguyên", !inv.issueStock("8930001", 500, d(30, 9), null, null) && mq.size() == 3);
        check("T3.3 tại 20/10 mọi lô hết hạn -> từ chối, lô vẫn nằm trong hàng", !inv.issueStock("8930001", 10, d(20, 10), null, null) && mq.size() == 3);
        check("T3.3 quantity = 0 bị từ chối", !inv.issueStock("8930001", 0, d(30, 9), null, null));

        // Trace 3.4 - gặp lô hết hạn
        issued.clear(); expired.clear();
        check("T3.4 issueStock 60 tại 07/10", inv.issueStock("8930001", 60, d(7, 10), issued, expired));
        check("T3.4 expiredList = [B02:40], issuedList = [B03:40, B04:20]", expired.toString().equals("[B02:40]") && issued.toString().equals("[B03:40, B04:20]"));
        check("T3.4 còn B04(60)", mq.size() == 1 && mq.peek().getQuantity() == 60);

        // Trace 4 - CheckExpiry
        Inventory inv2 = milkInventory();
        inv2.receiveBatch("8930001", new Batch("B04", d(14, 9), d(14, 10), 80));
        List<String> ex = inv2.checkExpiry(d(7, 10), 5);
        check("T4 checkExpiry(07/10, K=5): 2 hết hạn + 1 sắp hết hạn", ex.size() == 3 && ex.get(0).endsWith("B01|50|EXPIRED")
                && ex.get(1).endsWith("B02|60|EXPIRED") && ex.get(2).endsWith("B03|40|NEAR_EXPIRY"));
        check("T4 hàng đợi không đổi (4 lô)", inv2.getProductBST().search("8930001").batchQueue.size() == 4);
        List<String> edge = inv2.checkExpiry(d(10, 10), 0);
        check("T4 biên: HSD = currentDate, K = 0 -> B03 NEAR_EXPIRY (chưa hết hạn)", edge.size() == 3
                && edge.get(2).endsWith("B03|40|NEAR_EXPIRY") && edge.get(1).endsWith("B02|60|EXPIRED"));

        // Trace 5 - báo cáo
        List<String> rp = inv2.generateInventoryReport();
        check("T5 report: tổng 230, 4 lô, HSD sớm nhất 2026-10-01", rp.size() == 1 && rp.get(0).equals("8930001|Sua tuoi|hop|20|230|4|2026-10-01"));
        inv2.getProductBST().insert(new Product("8930002", "Nuoc", "chai", 5));
        check("T5 sản phẩm chưa có lô: tổng 0, số lô 0", inv2.generateInventoryReport().get(1).equals("8930002|Nuoc|chai|5|0|0|-"));

        System.out.println("\nKết quả: " + pass + " PASS, " + fail + " FAIL");
        if (fail > 0) System.exit(1);
    }
}
