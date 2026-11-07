package warehouse.structure;

import warehouse.model.Product;

/** BST node: one Product and that product's BatchQueue (Table 8). */
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
