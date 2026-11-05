/** Thông tin định danh của sản phẩm (Table 8). */
public class Product {
    private final String barcode;
    private String name;
    private String unit;
    private int minStock;

    public Product(String barcode, String name, String unit, int minStock) {
        this.barcode = barcode;
        this.name = name;
        this.unit = unit;
        this.minStock = minStock;
    }

    public String getBarcode() { return barcode; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public int getMinStock() { return minStock; }
    public void setName(String name) { this.name = name; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setMinStock(int minStock) { this.minStock = minStock; }
}
