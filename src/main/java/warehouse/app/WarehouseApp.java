package warehouse.app;

import warehouse.model.Batch;
import warehouse.model.Product;
import warehouse.service.Inventory;
import warehouse.structure.BSTNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * Command-line menu program for warehouse management. It only calls the business operations of
 * Inventory (pseudocode V.A) and adds no new business logic.
 * Run: java -cp out warehouse.app.WarehouseApp [data_directory]   (default: ./data)
 */
public class WarehouseApp {
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Inventory inv = new Inventory();
    private final Scanner in;
    private final PrintStream out;
    private final CsvStore store;

    public WarehouseApp(Scanner in, PrintStream out, Path dataDir) {
        this.in = in;
        this.out = out;
        this.store = new CsvStore(dataDir);
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "data");
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);
        new WarehouseApp(new Scanner(System.in, StandardCharsets.UTF_8), out, dir).run();
    }

    // ------------------------------------------------------------------ main loop
    public void run() {
        out.println("=== WAREHOUSE INVENTORY MANAGEMENT (BST + FIFO Queue) ===");
        try {
            List<String> warnings = store.load(inv);
            for (String w : warnings) out.println("Warning while loading data: " + w);
            out.println("Loaded " + inv.getProductBST().inOrderTraversal().size() + " product(s).");
        } catch (IOException e) {
            out.println("Could not read data: " + e.getMessage() + " (starting with an empty warehouse)");
        }
        try {
            while (true) {
                printMenu();
                String choice = ask("Choose an option");
                switch (choice) {
                    case "1": addProduct(); break;
                    case "2": lookupProduct(); break;
                    case "3": deleteProduct(); break;
                    case "4": receiveBatch(); break;
                    case "5": issueStock(); break;
                    case "6": checkExpiry(); break;
                    case "7": report(); break;
                    case "8": save(); break;
                    case "0": save(); out.println("Goodbye!"); return;
                    default: out.println("Invalid option.");
                }
            }
        } catch (NoSuchElementException e) {      // input ended (Ctrl+D / Ctrl+Z): save and exit
            out.println();
            save();
        }
    }

    private void printMenu() {
        out.println();
        out.println("----------------- MENU -----------------");
        out.println(" 1. Add a product");
        out.println(" 2. Look up a product by barcode");
        out.println(" 3. Delete a product (only when it has no batches)");
        out.println(" 4. Receive a batch");
        out.println(" 5. Issue stock (FIFO, expired batches are skipped)");
        out.println(" 6. Expiry alerts (expired / near expiry)");
        out.println(" 7. Inventory report");
        out.println(" 8. Save data");
        out.println(" 0. Save and exit");
    }

    // ------------------------------------------------------------------ features
    private void addProduct() {
        String barcode = ask("Barcode");
        if (barcode.isEmpty()) { out.println("Barcode must not be empty."); return; }
        String name = ask("Product name");
        String unit = ask("Unit");
        Integer min = askInt("Minimum stock (minStock)");
        if (min == null) return;
        boolean ok = inv.getProductBST().insert(new Product(barcode, name, unit, min));
        out.println(ok ? "Product " + barcode + " added." : "Barcode already exists, nothing added.");
    }

    private void lookupProduct() {
        BSTNode node = inv.getProductBST().search(ask("Barcode"));
        if (node == null) { out.println("Product not found."); return; }
        Product p = node.getProduct();
        out.println("Product: " + p.getBarcode() + " | " + p.getName() + " | unit " + p.getUnit()
                + " | minimum stock " + p.getMinStock());
        List<Batch> batches = node.getBatchQueue().toList();
        if (batches.isEmpty()) { out.println("  (no batches yet)"); return; }
        out.println("  Batches in issue order (front of the queue first):");
        for (Batch b : batches)
            out.println("  - " + b.getBatchId() + ": qty " + b.getQuantity() + ", received " + fmt(b.getImportDate())
                    + ", expires " + fmt(b.getExpiryDate()));
    }

    private void deleteProduct() {
        String barcode = ask("Barcode to delete");
        BSTNode node = inv.getProductBST().search(barcode);
        if (node == null) { out.println("Product not found."); return; }
        if (inv.getProductBST().delete(barcode)) out.println("Product " + barcode + " deleted.");
        else out.println("Cannot delete: the product still has " + node.getBatchQueue().size() + " batch(es) in stock.");
    }

    private void receiveBatch() {
        String barcode = ask("Product barcode");
        if (inv.getProductBST().search(barcode) == null) { out.println("The product is not in the catalog."); return; }
        String id = ask("Batch ID");
        LocalDate imp = askDate("Import date", true);
        if (imp == null) return;
        LocalDate exp = askDate("Expiry date", false);
        if (exp == null) return;
        Integer qty = askInt("Quantity");
        if (qty == null) return;
        boolean ok = inv.receiveBatch(barcode, new Batch(id, imp, exp, qty));
        out.println(ok ? "Batch " + id + " added to the rear of the queue."
                : "Receiving failed: quantity must be > 0 and the expiry date must be after the import date.");
    }

    private void issueStock() {
        String barcode = ask("Product barcode");
        if (inv.getProductBST().search(barcode) == null) { out.println("The product is not in the catalog."); return; }
        Integer qty = askInt("Quantity to issue");
        if (qty == null) return;
        LocalDate today = askDate("Issue date (Enter = today)", true);
        if (today == null) return;
        List<String> issued = new ArrayList<>(), expired = new ArrayList<>();
        if (inv.issueStock(barcode, qty, today, issued, expired)) {
            out.println("Stock issued. Taken from batches (batch:quantity): " + issued);
            if (!expired.isEmpty()) out.println("Expired batches removed from the queue: " + expired);
        } else {
            out.println("Rejected: invalid quantity or not enough unexpired stock. The warehouse is unchanged.");
        }
    }

    private void checkExpiry() {
        LocalDate today = askDate("Check date (Enter = today)", true);
        if (today == null) return;
        Integer k = askInt("Warning window in days (K)");
        if (k == null) return;
        List<String> list = inv.checkExpiry(today, k);
        if (list.isEmpty()) { out.println("No batch is expired or near expiry."); return; }
        out.println("barcode | batch | quantity | status");
        for (String s : list) out.println("  " + s.replace("|", " | "));
    }

    private void report() {
        List<String> rows = inv.generateInventoryReport();
        if (rows.isEmpty()) { out.println("The warehouse has no products."); return; }
        out.println("barcode | name | unit | min stock | total qty | batches | earliest expiry");
        for (String r : rows) out.println("  " + r.replace("|", " | "));
    }

    private void save() {
        try { store.save(inv); out.println("Data saved."); }
        catch (IOException e) { out.println("Error while saving: " + e.getMessage()); }
    }

    // ------------------------------------------------------------------ input helpers
    private String ask(String label) {
        out.print(label + ": ");
        out.flush();
        return in.nextLine().trim();           // throws NoSuchElementException when input ends
    }

    private Integer askInt(String label) {
        String s = ask(label);
        try { return Integer.parseInt(s); }
        catch (NumberFormatException e) { out.println("The value must be an integer."); return null; }
    }

    /** Accepts dd/MM/yyyy or yyyy-MM-dd. allowToday: an empty answer means today. */
    private LocalDate askDate(String label, boolean allowToday) {
        String s = ask(label + " [dd/MM/yyyy]");
        if (s.isEmpty() && allowToday) return LocalDate.now();
        try { return s.contains("/") ? LocalDate.parse(s, DMY) : LocalDate.parse(s); }
        catch (DateTimeParseException e) { out.println("Invalid date."); return null; }
    }

    private static String fmt(LocalDate d) { return d.format(DMY); }
}
