import java.util.ArrayList;
import java.util.List;

/** Cây nhị phân tìm kiếm (không tự cân bằng) theo barcode (Mục V.A 1.1 - 1.4). */
public class ProductBST {
    private BSTNode root;

    public BSTNode getRoot() { return root; }

    /** Mục 1.2 - O(h), cài đặt lặp. */
    public BSTNode search(String barcode) {
        BSTNode current = root;
        while (current != null) {
            int c = barcode.compareTo(current.product.getBarcode());
            if (c == 0) return current;
            current = (c < 0) ? current.left : current.right;
        }
        return null;
    }

    /** Mục 1.1 - O(h); trả về false nếu trùng barcode. */
    public boolean insert(Product product) {
        if (root == null) {
            root = new BSTNode(product);
            return true;
        }
        BSTNode current = root, parent = null;
        while (current != null) {
            parent = current;
            int c = product.getBarcode().compareTo(current.product.getBarcode());
            if (c == 0) return false;
            current = (c < 0) ? current.left : current.right;
        }
        BSTNode node = new BSTNode(product);
        if (product.getBarcode().compareTo(parent.product.getBarcode()) < 0) parent.left = node;
        else parent.right = node;
        return true;
    }

    /**
     * Mục 1.3 - O(h). Tiền điều kiện: sản phẩm tồn tại và BatchQueue của nó rỗng;
     * nếu không thì từ chối (trả về false) và cây không đổi.
     */
    public boolean delete(String barcode) {
        BSTNode node = search(barcode);
        if (node == null || !node.batchQueue.isEmpty()) return false;
        root = deleteNode(root, barcode);
        return true;
    }

    private BSTNode deleteNode(BSTNode node, String barcode) {
        if (node == null) return null;
        int c = barcode.compareTo(node.product.getBarcode());
        if (c < 0) {
            node.left = deleteNode(node.left, barcode);
        } else if (c > 0) {
            node.right = deleteNode(node.right, barcode);
        } else {
            if (node.left == null) return node.right;
            if (node.right == null) return node.left;
            BSTNode min = node.right;
            while (min.left != null) min = min.left;      // FindMin
            node.product = min.product;
            node.batchQueue = min.batchQueue;               // sao chép tham chiếu
            node.right = deleteNode(node.right, min.product.getBarcode());
        }
        return node;
    }

    /** Mục 1.4 - Θ(n): danh sách nút theo barcode tăng dần (đệ quy LNR). */
    public List<BSTNode> inOrderTraversal() {
        List<BSTNode> list = new ArrayList<>();
        visit(root, list);
        return list;
    }

    private void visit(BSTNode node, List<BSTNode> list) {
        if (node == null) return;
        visit(node.left, list);
        list.add(node);
        visit(node.right, list);
    }
}
