package warehouse.app;

import warehouse.model.Batch;
import warehouse.model.Product;
import warehouse.service.Inventory;
import warehouse.structure.BSTNode;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Saves and loads the warehouse from two CSV files (UTF-8):
 *   products.csv: barcode,name,unit,minStock
 *   batches.csv : barcode,batchId,importDate,expiryDate,quantity   (in the exact FIFO queue order of each product)
 * Products are written in pre-order (NLR) so that reloading rebuilds the BST with exactly the same shape
 * (writing them in ascending barcode order would make the reloaded tree degenerate into a linked list).
 */
public class CsvStore {
    private static final String PRODUCT_HEADER = "barcode,name,unit,minStock";
    private static final String BATCH_HEADER = "barcode,batchId,importDate,expiryDate,quantity";

    private final Path productFile;
    private final Path batchFile;

    public CsvStore(Path dir) {
        this.productFile = dir.resolve("products.csv");
        this.batchFile = dir.resolve("batches.csv");
    }

    /** Loads the data into the inventory and returns a list of warnings (rows that were skipped). A missing file is treated as an empty warehouse. */
    public List<String> load(Inventory inv) throws IOException {
        List<String> warnings = new ArrayList<>();
        if (Files.exists(productFile)) {
            int no = 0;
            for (String line : readLines(productFile)) {
                no++;
                if (no == 1 || line.trim().isEmpty()) continue;
                List<String> f = parse(line);
                try {
                    if (f.size() != 4) throw new IllegalArgumentException("expected 4 columns");
                    boolean ok = inv.getProductBST().insert(
                            new Product(f.get(0).trim(), f.get(1), f.get(2), Integer.parseInt(f.get(3).trim())));
                    if (!ok) warnings.add(productFile.getFileName() + " line " + no + ": duplicate barcode, skipped");
                } catch (RuntimeException e) {
                    warnings.add(productFile.getFileName() + " line " + no + ": " + e.getMessage());
                }
            }
        }
        if (Files.exists(batchFile)) {
            int no = 0;
            for (String line : readLines(batchFile)) {
                no++;
                if (no == 1 || line.trim().isEmpty()) continue;
                List<String> f = parse(line);
                try {
                    if (f.size() != 5) throw new IllegalArgumentException("expected 5 columns");
                    Batch b = new Batch(f.get(1).trim(), LocalDate.parse(f.get(2).trim()),
                            LocalDate.parse(f.get(3).trim()), Integer.parseInt(f.get(4).trim()));
                    if (!inv.receiveBatch(f.get(0).trim(), b))
                        warnings.add(batchFile.getFileName() + " line " + no + ": invalid batch or unknown product, skipped");
                } catch (RuntimeException e) {
                    warnings.add(batchFile.getFileName() + " line " + no + ": " + e.getMessage());
                }
            }
        }
        return warnings;
    }

    /** Writes the whole warehouse to the two CSV files (creates the directory if needed). */
    public void save(Inventory inv) throws IOException {
        Files.createDirectories(productFile.toAbsolutePath().getParent());
        List<BSTNode> preorder = preorder(inv);
        try (BufferedWriter w = Files.newBufferedWriter(productFile, StandardCharsets.UTF_8)) {
            w.write(PRODUCT_HEADER); w.newLine();
            for (BSTNode n : preorder) {
                Product p = n.getProduct();
                w.write(join(p.getBarcode(), p.getName(), p.getUnit(), String.valueOf(p.getMinStock()))); w.newLine();
            }
        }
        try (BufferedWriter w = Files.newBufferedWriter(batchFile, StandardCharsets.UTF_8)) {
            w.write(BATCH_HEADER); w.newLine();
            for (BSTNode n : preorder) {
                for (Batch b : n.getBatchQueue().toList()) {
                    w.write(join(n.getProduct().getBarcode(), b.getBatchId(), b.getImportDate().toString(),
                            b.getExpiryDate().toString(), String.valueOf(b.getQuantity()))); w.newLine();
                }
            }
        }
    }

    /** Pre-order (NLR) traversal with a loop (no recursion, so no stack overflow on skewed trees). */
    private static List<BSTNode> preorder(Inventory inv) {
        List<BSTNode> out = new ArrayList<>();
        Deque<BSTNode> stack = new ArrayDeque<>();
        if (inv.getProductBST().getRoot() != null) stack.push(inv.getProductBST().getRoot());
        while (!stack.isEmpty()) {
            BSTNode n = stack.pop();
            out.add(n);
            if (n.right != null) stack.push(n.right);
            if (n.left != null) stack.push(n.left);
        }
        return out;
    }

    private static List<String> readLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String s;
            while ((s = r.readLine()) != null) lines.add(s);
        }
        if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) lines.set(0, lines.get(0).substring(1)); // Excel byte-order mark
        return lines;
    }

    private static String join(String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quote(fields[i]));
        }
        return sb.toString();
    }

    private static String quote(String s) {
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0) return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    /** Splits one CSV line; supports fields wrapped in double quotes. */
    static List<String> parse(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else inQuotes = false;
                } else cur.append(c);
            } else if (c == '"') inQuotes = true;
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); }
            else cur.append(c);
        }
        out.add(cur.toString());
        return out;
    }
}
