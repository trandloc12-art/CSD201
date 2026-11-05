/** Nút của cây tìm kiếm: một Product và BatchQueue của sản phẩm đó (Table 8). */
public class BSTNode {
    public Product product;
    public BatchQueue batchQueue;
    public BSTNode left;
    public BSTNode right;

    public BSTNode(Product product) {
        this.product = product;
        this.batchQueue = new BatchQueue();
    }

    public Product getProduct() { return product; }
    public BatchQueue getBatchQueue() { return batchQueue; }
}
