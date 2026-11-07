package warehouse.structure;

import warehouse.model.Product;

import java.util.ArrayList;
import java.util.List;

/** Binary search tree (not self-balancing) keyed by barcode (pseudocode V.A 1.1 - 1.4). */
public class ProductBST {
    private BSTNode root;

    public BSTNode getRoot() { return root; }

    /** Pseudocode 1.2 - O(h), iterative implementation. */
    public BSTNode search(String barcode) {
        BSTNode current = root;
        while (current != null) {
            int c = barcode.compareTo(current.product.getBarcode());
            if (c == 0) return current;
            current = (c < 0) ? current.left : current.right;
        }
        return null;
    }

    /** Pseudocode 1.1 - O(h); returns false if the barcode already exists. */
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
     * Pseudocode 1.3 - O(h). Precondition: the product exists and its BatchQueue is empty;
     * otherwise the request is rejected (returns false) and the tree is unchanged.
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
            node.batchQueue = min.batchQueue;               // copy the reference
            node.right = deleteNode(node.right, min.product.getBarcode());
        }
        return node;
    }

    /**
     * Pseudocode 1.4 - Θ(n): nodes in ascending barcode order (LNR).
     * Traversal uses a loop with a hand-written array stack (doubles its capacity when full)
     * instead of recursion, so the JVM call stack can no longer overflow on large skewed trees. The stack lives on the heap,
     * allocates no object per node, and yields exactly the same result as the recursive version. Extra space O(h).
     */
    public List<BSTNode> inOrderTraversal() {
        List<BSTNode> list = new ArrayList<>();
        BSTNode[] stack = new BSTNode[64];     // stack: nodes passed on the way down but not yet visited
        int top = 0;                           // number of elements currently in the stack
        BSTNode current = root;
        while (current != null || top > 0) {
            while (current != null) {          // walk down the left branch, pushing each node
                if (top == stack.length) {     // full: double the capacity (manual copy), amortized O(1)
                    BSTNode[] bigger = new BSTNode[top * 2];
                    for (int i = 0; i < top; i++) bigger[i] = stack[i];
                    stack = bigger;
                }
                stack[top++] = current;
                current = current.left;
            }
            BSTNode node = stack[--top];       // pop the node on top of the stack
            stack[top] = null;                 // do not keep a stale reference
            list.add(node);                    // visit the node (N)
            current = node.right;              // move to the right subtree (R)
        }
        return list;
    }
}
