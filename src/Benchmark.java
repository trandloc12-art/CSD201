import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Đo hiệu năng thực tế (Mục VI.C). Cách đo: System.nanoTime(); warm-up 5 lần (bỏ qua),
 * sau đó đo 10 lần và lấy trung bình; mỗi lần đo gồm nhiều lượt thao tác rồi chia lấy thời gian trung bình/1 thao tác.
 * Kết quả ghi ra stdout dạng CSV: operation,n,k,warmup,reps,mean_us,stdev_us,median_us,min_us
 */
public class Benchmark {
    static final int WARMUP = 5, REPS = 10;
    static volatile long sink;                       // chống JIT loại bỏ code
    static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    interface Rep { double run(); }                   // trả về micro-giây / 1 thao tác

    static void measure(String op, int n, int k, Rep rep) { measure(op, n, k, WARMUP, REPS, rep); }

    static void measure(String op, int n, int k, int warm, int reps, Rep rep) {
        for (int i = 0; i < warm; i++) rep.run();
        System.gc();
        double[] v = new double[reps];
        double sum = 0, min = Double.MAX_VALUE;
        for (int i = 0; i < reps; i++) { v[i] = rep.run(); sum += v[i]; min = Math.min(min, v[i]); }
        double mean = sum / reps, var = 0;
        for (double x : v) var += (x - mean) * (x - mean);
        double sd = Math.sqrt(var / (reps - 1));
        double[] sorted = v.clone();
        java.util.Arrays.sort(sorted);
        double median = (reps % 2 == 1) ? sorted[reps / 2] : (sorted[reps / 2 - 1] + sorted[reps / 2]) / 2;
        System.out.printf(Locale.ROOT, "%s,%d,%d,%d,%d,%.4f,%.4f,%.4f,%.4f%n", op, n, k, warm, reps, mean, sd, median, min);
        System.out.flush();
    }

    static String key(int i) {
        char[] c = new char[13];
        for (int p = 12; p >= 0; p--) { c[p] = (char) ('0' + i % 10); i /= 10; }
        return new String(c);
    }

    static int[] shuffled(int n, long seed) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        Random r = new Random(seed);
        for (int i = n - 1; i > 0; i--) { int j = r.nextInt(i + 1); int t = a[i]; a[i] = a[j]; a[j] = t; }
        return a;
    }

    static Product[] products(int n) {
        int[] order = shuffled(n, 42);
        Product[] ps = new Product[n];
        for (int i = 0; i < n; i++) ps[i] = new Product(key(order[i]), "P" + order[i], "pcs", 10);
        return ps;
    }

    static ProductBST randomTree(Product[] ps) {
        ProductBST t = new ProductBST();
        for (Product p : ps) t.insert(p);
        return t;
    }

    /** Dựng cây lệch (barcode tăng dần) bằng cách nối thẳng nút phải, tránh chi phí O(n^2) khi dựng cây lớn. */
    static ProductBST skewedTree(int n) throws Exception {
        ProductBST t = new ProductBST();
        BSTNode first = new BSTNode(new Product(key(0), "P0", "pcs", 10)), last = first;
        for (int i = 1; i < n; i++) { BSTNode x = new BSTNode(new Product(key(i), "P" + i, "pcs", 10)); last.right = x; last = x; }
        Field f = ProductBST.class.getDeclaredField("root");
        f.setAccessible(true);
        f.set(t, first);
        return t;
    }

    static String[] queryKeys(int n, int q, long seed) {
        Random r = new Random(seed);
        String[] ks = new String[q];
        for (int i = 0; i < q; i++) ks[i] = key(r.nextInt(n));
        return ks;
    }

    static double searchRep(ProductBST t, String[] qs) {
        long t0 = System.nanoTime();
        long acc = 0;
        for (String s : qs) acc += t.search(s).product.getMinStock();
        long t1 = System.nanoTime();
        sink += acc;
        return (t1 - t0) / 1000.0 / qs.length;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("# java=" + System.getProperty("java.version") + " vm=" + System.getProperty("java.vm.name")
                + " os=" + System.getProperty("os.name") + " " + System.getProperty("os.arch")
                + " cores=" + Runtime.getRuntime().availableProcessors()
                + " maxHeapMB=" + Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.println("operation,n,k,warmup,reps,mean_us,stdev_us,median_us,min_us");
        int[] sizes = {1_000, 10_000, 50_000, 100_000};

        // 1) search, cây ngẫu nhiên
        for (int n : sizes) {
            ProductBST t = randomTree(products(n));
            String[] qs = queryKeys(n, 100_000, 7);
            measure("search_random", n, 0, () -> searchRep(t, qs));
        }
        // 2) search, cây lệch (barcode tăng dần)
        for (int n : sizes) {
            ProductBST t = skewedTree(n);
            String[] qs = queryKeys(n, Math.max(1_000, 100_000_000 / n), 7);
            measure("search_skewed", n, 0, () -> searchRep(t, qs));
        }
        // 3) insert, cây ngẫu nhiên: dựng cây n phần tử từ rỗng, thời gian trung bình / 1 lần insert
        for (int n : sizes) {
            Product[] ps = products(n);
            int loops = Math.max(1, 200_000 / n);
            measure("insert_random", n, 0, () -> {
                long total = 0;
                for (int l = 0; l < loops; l++) {
                    ProductBST t = new ProductBST();
                    long t0 = System.nanoTime();
                    for (Product p : ps) t.insert(p);
                    total += System.nanoTime() - t0;
                    sink += t.getRoot().product.getMinStock();
                }
                return total / 1000.0 / ((double) loops * n);
            });
        }
        // 4) insert, barcode tăng dần (cây lệch)
        for (int n : new int[]{1_000, 5_000, 10_000, 20_000}) {
            Product[] ps = new Product[n];
            for (int i = 0; i < n; i++) ps[i] = new Product(key(i), "P" + i, "pcs", 10);
            measure("insert_ascending", n, 0, () -> {
                ProductBST t = new ProductBST();
                long t0 = System.nanoTime();
                for (Product p : ps) t.insert(p);
                long el = System.nanoTime() - t0;
                sink += t.getRoot().product.getMinStock();
                return el / 1000.0 / n;
            });
        }
        // 5) enqueue / dequeue: hàng đợi n lô
        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            Batch[] bs = new Batch[n];
            for (int i = 0; i < n; i++) bs[i] = new Batch("B" + i, TODAY, TODAY.plusDays(30), 1);
            int loops = Math.max(1, 2_000_000 / n);
            final double[] last = new double[2];
            Rep both = () -> {
                long te = 0, td = 0;
                for (int l = 0; l < loops; l++) {
                    BatchQueue q = new BatchQueue();
                    long t0 = System.nanoTime();
                    for (Batch b : bs) q.enqueue(b);
                    long t1 = System.nanoTime();
                    long acc = 0;
                    for (int i = 0; i < n; i++) acc += q.dequeue().getQuantity();
                    long t2 = System.nanoTime();
                    sink += acc;
                    te += t1 - t0; td += t2 - t1;
                }
                last[0] = te / 1000.0 / ((double) loops * n);
                last[1] = td / 1000.0 / ((double) loops * n);
                return last[0];
            };
            measure("enqueue", n, 0, both);
            measure("dequeue", n, 0, () -> { both.run(); return last[1]; });
        }
        // 6) issueStock xuất 1 đơn vị: phụ thuộc n (k = 10 lô) -> O(h + k)
        for (int n : sizes) {
            Inventory inv = new Inventory();
            Product[] ps = products(n);
            ProductBST t = inv.getProductBST();
            for (Product p : ps) t.insert(p);
            String[] probe = queryKeys(n, 1_000, 11);
            for (String s : probe) {
                BatchQueue q = t.search(s).batchQueue;
                if (q.isEmpty()) for (int j = 0; j < 10; j++) q.enqueue(new Batch("B" + j, TODAY, TODAY.plusDays(30), 1_000_000_000));
            }
            String[] qs = queryKeys(n, 1_000, 11);       // cùng tập khóa với probe
            measure("issueStock_1unit", n, 10, () -> {
                long t0 = System.nanoTime();
                int ok = 0;
                for (int i = 0; i < 100; i++) for (String s : qs) if (inv.issueStock(s, 1, TODAY, null, null)) ok++;
                long el = System.nanoTime() - t0;
                sink += ok;
                return el / 1000.0 / (100.0 * qs.length);
            });
        }
        // 7) issueStock: phụ thuộc k (n = 1000), (a) xuất 1 đơn vị, (b) xuất hết k lô
        for (int k : new int[]{10, 100, 1_000, 10_000}) {
            Inventory inv = new Inventory();
            for (Product p : products(1_000)) inv.getProductBST().insert(p);
            String target = key(500);
            BatchQueue q = inv.getProductBST().search(target).batchQueue;
            Batch[] bs = new Batch[k];
            for (int i = 0; i < k; i++) bs[i] = new Batch("B" + i, TODAY, TODAY.plusDays(30), 1);
            for (Batch b : bs) q.enqueue(b);
            int calls = Math.max(100, 2_000_000 / k);
            measure("issueStock_1unit", 1_000, k, () -> {
                // đưa tồn lên đủ lớn để không cạn: dùng lô đầu có số lượng rất lớn
                q.peek().setQuantity(1_000_000_000);
                long t0 = System.nanoTime();
                int ok = 0;
                for (int i = 0; i < calls; i++) if (inv.issueStock(target, 1, TODAY, null, null)) ok++;
                long el = System.nanoTime() - t0;
                sink += ok;
                return el / 1000.0 / calls;
            });
            int drains = Math.max(20, 200_000 / k);
            measure("issueStock_drain_all", 1_000, k, 10, 20, () -> {
                long total = 0;
                for (int i = 0; i < drains; i++) {
                    while (q.dequeue() != null) { }
                    for (Batch b : bs) { b.setQuantity(1); q.enqueue(b); }       // nạp lại (không tính giờ)
                    long t0 = System.nanoTime();
                    boolean ok = inv.issueStock(target, k, TODAY, null, null);
                    total += System.nanoTime() - t0;
                    if (!ok || !q.isEmpty()) throw new IllegalStateException("drain failed");
                }
                return total / 1000.0 / drains;
            });
        }
        // 8) checkExpiry và generateInventoryReport: n sản phẩm, mỗi sản phẩm 5 lô (B = 5n)
        for (int n : sizes) {
            Inventory inv = new Inventory();
            Random r = new Random(5);
            for (Product p : products(n)) {
                inv.getProductBST().insert(p);
                BatchQueue q = inv.getProductBST().search(p.getBarcode()).batchQueue;
                for (int j = 0; j < 5; j++) {
                    LocalDate imp = LocalDate.of(2026, 9, 1).plusDays(j);
                    q.enqueue(new Batch("B" + j, imp, LocalDate.of(2026, 10, 1).plusDays(r.nextInt(90)), 10 + r.nextInt(90)));
                }
            }
            measure("checkExpiry", n, 5, 20, 30, () -> {
                long t0 = System.nanoTime();
                int s = inv.checkExpiry(TODAY, 10).size();
                long el = System.nanoTime() - t0;
                sink += s;
                return el / 1000.0;
            });
            measure("generateInventoryReport", n, 5, 20, 30, () -> {
                long t0 = System.nanoTime();
                int s = inv.generateInventoryReport().size();
                long el = System.nanoTime() - t0;
                sink += s;
                return el / 1000.0;
            });
        }
        System.out.println("# sink=" + sink);
    }
}
