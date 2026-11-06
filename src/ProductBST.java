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

    /**
     * Mục 1.4 - Θ(n): danh sách nút theo barcode tăng dần (LNR).
     * Phase 4 (Hướng 1): duyệt bằng vòng lặp với ngăn xếp mảng tự cài đặt (tự giãn gấp đôi khi đầy)
     * thay cho đệ quy, nên không còn tràn call stack của JVM trên cây lệch lớn. Ngăn xếp nằm trên heap,
     * không cấp phát đối tượng cho từng nút; kết quả giống hệt bản đệ quy. Không gian phụ O(h).
     */
    public List<BSTNode> inOrderTraversal() {
        List<BSTNode> list = new ArrayList<>();
        BSTNode[] stack = new BSTNode[64];     // ngăn xếp: các nút đã đi qua nhưng chưa thăm
        int top = 0;                           // số phần tử hiện có trong ngăn xếp
        BSTNode current = root;
        while (current != null || top > 0) {
            while (current != null) {          // đi hết nhánh trái, đẩy từng nút vào ngăn xếp
                if (top == stack.length) {     // đầy: giãn gấp đôi (sao chép thủ công), chi phí chia đều O(1)
                    BSTNode[] bigger = new BSTNode[top * 2];
                    for (int i = 0; i < top; i++) bigger[i] = stack[i];
                    stack = bigger;
                }
                stack[top++] = current;
                current = current.left;
            }
            BSTNode node = stack[--top];       // lấy nút ở đỉnh ngăn xếp
            stack[top] = null;                 // không giữ tham chiếu thừa
            list.add(node);                    // thăm nút (N)
            current = node.right;              // chuyển sang cây con phải (R)
        }
        return list;
    }
}
