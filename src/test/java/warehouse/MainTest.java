package warehouse;

import warehouse.model.Batch;
import warehouse.model.Product;
import warehouse.service.Inventory;
import warehouse.structure.BSTNode;
import warehouse.structure.BatchQueue;
import warehouse.structure.ProductBST;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Tests that reproduce the traces of report section V.B (dates are dd/MM in 2026). */
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


    // ---- Helpers for testing the iterative in-order traversal (section VII.B) ----
    static void refInOrder(BSTNode n, List<BSTNode> out) {      // reference recursive version used for comparison
        if (n == null) return;
        refInOrder(n.left, out); out.add(n); refInOrder(n.right, out);
    }

    static String code(int i) {                                   // 8-digit barcode: string comparison equals numeric comparison
        return String.format("%08d", i);
    }

    /** Builds a skewed tree of n nodes by linking directly (avoids the O(n^2) cost of insert); right = true: right-skewed. */
    static ProductBST skewed(int n, boolean right) throws Exception {
        return skewedInto(new ProductBST(), n, right);
    }

    static ProductBST skewedInto(ProductBST t, int n, boolean right) throws Exception {
        BSTNode first = new BSTNode(new Product(code(right ? 0 : n), "p", "u", 1)), last = first;
        for (int i = 1; i < n; i++) {
            BSTNode x = new BSTNode(new Product(code(right ? i : n - i), "p", "u", 1));
            if (right) last.right = x; else last.left = x;
            last = x;
        }
        java.lang.reflect.Field f = ProductBST.class.getDeclaredField("root");
        f.setAccessible(true);
        f.set(t, first);
        return t;
    }

    static boolean ascending(List<BSTNode> l) {
        for (int i = 1; i < l.size(); i++)
            if (l.get(i - 1).product.getBarcode().compareTo(l.get(i).product.getBarcode()) >= 0) return false;
        return true;
    }

    public static void main(String[] args) throws Exception {
        // Trace 1 - BST
        ProductBST t = new ProductBST();
        check("T1 insert BC-04/02/06/05", t.insert(new Product("BC-04", "a", "u", 0)) && t.insert(new Product("BC-02", "b", "u", 0))
                && t.insert(new Product("BC-06", "c", "u", 0)) && t.insert(new Product("BC-05", "d", "u", 0)));
        check("T1 insert with a duplicate barcode is rejected", !t.insert(new Product("BC-05", "x", "u", 0)));
        check("T1 search BC-05 / BC-09", t.search("BC-05") != null && t.search("BC-09") == null);
        check("T1 delete BC-04 (2 con) -> root = BC-05", t.delete("BC-04") && t.getRoot().product.getBarcode().equals("BC-05")
                && t.getRoot().left.product.getBarcode().equals("BC-02") && t.getRoot().right.product.getBarcode().equals("BC-06")
                && t.getRoot().right.left == null);
        check("T1 in-order is ascending", t.inOrderTraversal().size() == 3
                && t.inOrderTraversal().get(0).product.getBarcode().equals("BC-02"));
        // Delete precondition (handles point 5)
        t.search("BC-06").batchQueue.enqueue(new Batch("X", d(1, 9), d(1, 10), 5));
        check("Delete rejected while the queue still holds batches; tree unchanged", !t.delete("BC-06") && t.search("BC-06") != null);
        check("Delete of a non-existent barcode returns false", !t.delete("BC-99"));

        // Trace 2 / 2.1 / 2.2 - BatchQueue
        BatchQueue q = new BatchQueue();
        check("T2.2 empty queue: peek/dequeue = null, isEmpty = TRUE, size = 0",
                q.peek() == null && q.dequeue() == null && q.isEmpty() && q.size() == 0);
        q.enqueue(new Batch("B01", d(1, 9), d(1, 10), 1));
        q.enqueue(new Batch("B02", d(2, 9), d(2, 10), 1));
        q.enqueue(new Batch("B03", d(3, 9), d(3, 10), 1));
        check("T2 enqueue B01,B02,B03 -> size 3, toList in order", q.size() == 3 && q.toList().get(0).getBatchId().equals("B01")
                && q.toList().get(2).getBatchId().equals("B03") && q.size() == 3);
        check("T2.1 dequeue theo FIFO B01,B02,B03", q.dequeue().getBatchId().equals("B01") && q.dequeue().getBatchId().equals("B02")
                && q.dequeue().getBatchId().equals("B03") && q.isEmpty());

        // Trace 3.1 - ReceiveBatch
        Inventory inv = milkInventory();
        check("T3.1 receiveBatch B04 is valid", inv.receiveBatch("8930001", new Batch("B04", d(14, 9), d(14, 10), 80))
                && inv.getProductBST().search("8930001").batchQueue.size() == 4);
        check("T3.1 B05 with quantity = 0 is rejected", !inv.receiveBatch("8930001", new Batch("B05", d(14, 9), d(14, 10), 0)));
        check("T3.1 unknown barcode is rejected", !inv.receiveBatch("8930999", new Batch("B06", d(14, 9), d(14, 10), 10)));
        check("T3.1 expiry date <= import date is rejected", !inv.receiveBatch("8930001", new Batch("B07", d(14, 9), d(14, 9), 10)));
        check("T3.1 queue still holds 4 batches", inv.getProductBST().search("8930001").batchQueue.size() == 4);

        // Trace 3.2 - issuing across several batches
        List<String> issued = new ArrayList<>(), expired = new ArrayList<>();
        check("T3.2 issueStock 70 on 30/09", inv.issueStock("8930001", 70, d(30, 9), issued, expired));
        check("T3.2 issuedList = [B01:50, B02:20]", issued.toString().equals("[B01:50, B02:20]") && expired.isEmpty());
        BatchQueue mq = inv.getProductBST().search("8930001").batchQueue;
        check("T3.2 state: B02(40) B03(40) B04(80)", mq.size() == 3 && mq.peek().getBatchId().equals("B02") && mq.peek().getQuantity() == 40);

        // Trace 3.3 - more than in stock / everything expired / invalid input
        check("T3.3 issuing 500 > 160 in stock is rejected, queue unchanged", !inv.issueStock("8930001", 500, d(30, 9), null, null) && mq.size() == 3);
        check("T3.3 on 20/10 every batch is expired -> rejected, batches stay in the queue", !inv.issueStock("8930001", 10, d(20, 10), null, null) && mq.size() == 3);
        check("T3.3 quantity = 0 is rejected", !inv.issueStock("8930001", 0, d(30, 9), null, null));

        // Trace 3.4 - encountering an expired batch
        issued.clear(); expired.clear();
        check("T3.4 issueStock 60 on 07/10", inv.issueStock("8930001", 60, d(7, 10), issued, expired));
        check("T3.4 expiredList = [B02:40], issuedList = [B03:40, B04:20]", expired.toString().equals("[B02:40]") && issued.toString().equals("[B03:40, B04:20]"));
        check("T3.4 B04(60) remains", mq.size() == 1 && mq.peek().getQuantity() == 60);

        // Trace 4 - CheckExpiry
        Inventory inv2 = milkInventory();
        inv2.receiveBatch("8930001", new Batch("B04", d(14, 9), d(14, 10), 80));
        List<String> ex = inv2.checkExpiry(d(7, 10), 5);
        check("T4 checkExpiry(07/10, K=5): 2 expired + 1 near expiry", ex.size() == 3 && ex.get(0).endsWith("B01|50|EXPIRED")
                && ex.get(1).endsWith("B02|60|EXPIRED") && ex.get(2).endsWith("B03|40|NEAR_EXPIRY"));
        check("T4 queue unchanged (4 batches)", inv2.getProductBST().search("8930001").batchQueue.size() == 4);
        List<String> edge = inv2.checkExpiry(d(10, 10), 0);
        check("T4 boundary: expiry = currentDate, K = 0 -> B03 NEAR_EXPIRY (not expired)", edge.size() == 3
                && edge.get(2).endsWith("B03|40|NEAR_EXPIRY") && edge.get(1).endsWith("B02|60|EXPIRED"));

        // Trace 5 - report
        List<String> rp = inv2.generateInventoryReport();
        check("T5 report: total 230, 4 batches, earliest expiry 2026-10-01", rp.size() == 1 && rp.get(0).equals("8930001|Sua tuoi|hop|20|230|4|2026-10-01"));
        inv2.getProductBST().insert(new Product("8930002", "Nuoc", "chai", 5));
        check("T5 product without batches: total 0, batch count 0", inv2.generateInventoryReport().get(1).equals("8930002|Nuoc|chai|5|0|0|-"));


        // ---- Boundary tests and additional branches ----
        // Delete: leaf node and node with one child
        ProductBST d = new ProductBST();
        for (String c : new String[]{"M", "F", "T", "A", "H", "P"}) d.insert(new Product(c, c, "u", 0));
        //          M
        //        /   \\
        //       F     T
        //      / \\   /
        //     A   H P
        check("T1b delete leaf (A): the BST property is kept", d.delete("A") && d.search("A") == null
                && d.getRoot().left.left == null && d.inOrderTraversal().size() == 5);
        check("T1b delete one-child node (T, left child P): P takes the place of T", d.delete("T")
                && d.getRoot().right.product.getBarcode().equals("P") && d.search("T") == null);
        check("T1b delete the root (2 children): the root becomes the successor", d.delete("M")
                && d.getRoot().product.getBarcode().equals("P"));
        StringBuilder order = new StringBuilder();
        for (BSTNode n : d.inOrderTraversal()) order.append(n.product.getBarcode());
        check("T1b in-order after the deletions is still ascending (F,H,P)", order.toString().equals("FHP"));
        ProductBST one = new ProductBST();
        one.insert(new Product("X", "x", "u", 0));
        check("T1b delete the only node -> empty tree", one.delete("X") && one.getRoot() == null && one.inOrderTraversal().isEmpty());
        check("T1b search/delete on an empty tree", one.search("X") == null && !one.delete("X"));

        // Delete moves the successor's BatchQueue (pseudocode V.A 1.3 step 4.5)
        ProductBST mv = new ProductBST();
        mv.insert(new Product("B", "b", "u", 0)); mv.insert(new Product("A", "a", "u", 0)); mv.insert(new Product("C", "c", "u", 0));
        mv.search("C").batchQueue.enqueue(new Batch("KEEP", d(1, 9), d(1, 12), 7));
        check("T1b delete a 2-child root: the successor's (C) queue loses no batch",
                mv.delete("B") && mv.getRoot().product.getBarcode().equals("C")
                && mv.getRoot().batchQueue.size() == 1 && mv.getRoot().batchQueue.peek().getBatchId().equals("KEEP"));

        // Invalid input
        Inventory inv3 = milkInventory();
        check("T3.1b receiveBatch with negative quantity is rejected", !inv3.receiveBatch("8930001", new Batch("N1", d(14, 9), d(14, 10), -5)));
        check("T3.1b receiveBatch with empty / null barcode or null batch is rejected",
                !inv3.receiveBatch("", new Batch("N2", d(14, 9), d(14, 10), 5))
                && !inv3.receiveBatch(null, new Batch("N3", d(14, 9), d(14, 10), 5))
                && !inv3.receiveBatch("8930001", null));
        check("T3.1b receiveBatch with a null date is rejected", !inv3.receiveBatch("8930001", new Batch("N4", null, d(14, 10), 5))
                && !inv3.receiveBatch("8930001", new Batch("N5", d(14, 9), null, 5)));
        check("T3.1b queue still holds 3 batches after the rejections", inv3.getProductBST().search("8930001").batchQueue.size() == 3);
        check("T3.3b issueStock with negative quantity / unknown barcode / empty barcode / null date is rejected",
                !inv3.issueStock("8930001", -1, d(30, 9), null, null) && !inv3.issueStock("0000000", 1, d(30, 9), null, null)
                && !inv3.issueStock("", 1, d(30, 9), null, null) && !inv3.issueStock("8930001", 1, null, null, null));
        check("T3.3b a rejected issueStock leaves the queue unchanged", inv3.getProductBST().search("8930001").batchQueue.size() == 3
                && inv3.getProductBST().search("8930001").batchQueue.peek().getQuantity() == 50);

        // Boundary: expiry = currentDate is still valid when issuing (expired only if expiryDate < currentDate)
        Inventory inv4 = new Inventory();
        inv4.getProductBST().insert(new Product("P1", "p", "u", 0));
        inv4.receiveBatch("P1", new Batch("E1", d(1, 9), d(10, 10), 30));
        List<String> iss4 = new ArrayList<>(), exp4 = new ArrayList<>();
        check("T3.4b boundary: expiry = currentDate (10/10) can still be issued and is not discarded",
                inv4.issueStock("P1", 30, d(10, 10), iss4, exp4) && iss4.toString().equals("[E1:30]") && exp4.isEmpty());
        inv4.receiveBatch("P1", new Batch("E2", d(1, 9), d(10, 10), 30));
        check("T3.4b boundary: expiry = currentDate - 1 day is expired and cannot be issued",
                !inv4.issueStock("P1", 1, d(11, 10), null, null));

        // Issue exactly the available stock -> queue empty, the product can then be deleted
        Inventory inv5 = milkInventory();
        check("T3.2b issuing exactly the total stock (150) -> queue empty",
                inv5.issueStock("8930001", 150, d(30, 9), null, null) && inv5.getProductBST().search("8930001").batchQueue.isEmpty());
        check("T3.2b empty queue -> deleting the product is allowed", inv5.getProductBST().delete("8930001"));

        // checkExpiry / report with several products: ascending barcode order
        Inventory inv6 = new Inventory();
        for (String c : new String[]{"300", "100", "200"}) inv6.getProductBST().insert(new Product(c, "p" + c, "u", 1));
        inv6.receiveBatch("200", new Batch("Z", d(1, 9), d(8, 10), 10));
        List<String> rp6 = inv6.generateInventoryReport();
        check("T5b report of several products in ascending barcode order (100,200,300)",
                rp6.size() == 3 && rp6.get(0).startsWith("100|") && rp6.get(1).startsWith("200|") && rp6.get(2).startsWith("300|"));
        check("T4b checkExpiry with no batch needing attention -> empty list", new Inventory().checkExpiry(d(7, 10), 5).isEmpty());

        // ===== Optimization: iterative inOrderTraversal (T6x) =====
        check("T6a inOrderTraversal: empty tree -> empty list", new ProductBST().inOrderTraversal().isEmpty());
        ProductBST single = new ProductBST();
        single.insert(new Product("X", "x", "u", 0));
        check("T6b inOrderTraversal: single-node tree", single.inOrderTraversal().size() == 1
                && single.inOrderTraversal().get(0) == single.getRoot());
        ProductBST rnd = new ProductBST();
        java.util.Random rg = new java.util.Random(7);
        for (int i = 0; i < 2000; i++) rnd.insert(new Product(code(rg.nextInt(100000)), "p", "u", 1));
        List<BSTNode> refRnd = new ArrayList<>();
        refInOrder(rnd.getRoot(), refRnd);
        List<BSTNode> itRnd = rnd.inOrderTraversal();
        boolean same = itRnd.size() == refRnd.size();
        for (int i = 0; same && i < refRnd.size(); i++) same = itRnd.get(i) == refRnd.get(i);
        check("T6c random tree (~2000 nodes): result matches the reference recursive version node by node", same && ascending(itRnd));
        ProductBST small = new ProductBST();
        for (String c : new String[]{"M", "F", "T", "A", "H", "P", "Z", "B"}) small.insert(new Product(c, "p", "u", 0));
        StringBuilder sb = new StringBuilder();
        for (BSTNode nd : small.inOrderTraversal()) sb.append(nd.product.getBarcode());
        check("T6d small tree with left and right branches: order ABFHMPTZ", sb.toString().equals("ABFHMPTZ"));
        List<BSTNode> before = small.inOrderTraversal();
        List<BSTNode> after2 = small.inOrderTraversal();
        check("T6e two calls give the same result and do not change the tree", before.equals(after2) && small.getRoot().product.getBarcode().equals("M"));
        int BIG = 200_000;
        List<BSTNode> sr = skewed(BIG, true).inOrderTraversal();
        check("T6f right-skewed tree of 200,000 nodes: no stack overflow, all nodes, ascending", sr.size() == BIG && ascending(sr));
        List<BSTNode> sl = skewed(BIG, false).inOrderTraversal();
        check("T6g left-skewed tree of 200,000 nodes: no stack overflow, all nodes, ascending", sl.size() == BIG && ascending(sl));
        Inventory invS = new Inventory();
        ProductBST sk = skewedInto(invS.getProductBST(), BIG, true);
        sk.getRoot().batchQueue.enqueue(new Batch("S1", d(1, 9), d(5, 10), 7));
        check("T6h checkExpiry and generateInventoryReport run on a skewed tree of 200,000 nodes",
                invS.checkExpiry(d(7, 10), 5).size() == 1 && invS.generateInventoryReport().size() == BIG);

        // T7: checkExpiry with a null currentDate must return an empty list and not throw NullPointerException
        Inventory invN = milkInventory();
        boolean noThrow = true;
        List<String> nullRes = null;
        try { nullRes = invN.checkExpiry(null, 5); } catch (RuntimeException e) { noThrow = false; }
        check("T7 checkExpiry(null, 5): returns an empty list, no NullPointerException, queue unchanged",
                noThrow && nullRes != null && nullRes.isEmpty()
                && invN.getProductBST().search("8930001").getBatchQueue().size() == 3);

        System.out.println("\nResult: " + pass + " PASS, " + fail + " FAIL");
        if (fail > 0) System.exit(1);
    }
}
