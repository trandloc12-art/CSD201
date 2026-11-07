# Source code notes (for the defense / oral questions)

## Project structure
`model` (Product, Batch) | `structure` (BSTNode, BatchQueue, ProductBST) | `service` (Inventory); the menu program is in `app` (`WarehouseApp`, `CsvStore`); tests are in `src/test`, performance measurement in `src/bench`.

## Menu program and CSV storage
`WarehouseApp` only calls `Inventory` / `ProductBST` and contains no new business logic. `CsvStore` writes products in **pre-order (NLR)** so that reloading rebuilds the tree with exactly the same shape; writing them in ascending barcode order would make the reloaded tree degenerate into a linked list (h = n). Batches are written in their exact FIFO queue order. A batch that `issueStock` already removed because it was expired is no longer in the saved file.

## Big picture
`Inventory` -> uses `ProductBST` (find a product by barcode) -> each `BSTNode` holds one `Product` + one `BatchQueue` -> the queue holds the `Batch` objects.
The BST handles **finding a product** (O(h)); the queue handles the **FIFO order of batches** (O(1)). The two concerns are kept separate.

## Points likely to be asked
1. **Why does `issueStock` have two phases?** Phase 1 is read-only (it sums the quantity of unexpired batches). If there is not enough stock the request is rejected immediately and the queue is not changed.
   Phase 2 can then be sure there is enough stock, so `peek()` never meets an empty queue. There is no "half-issued" state.
2. **An expired batch at the front** is only removed (dequeue + recorded in `expiredList`) when an issue is accepted. If the issue is rejected the expired batch stays in the queue so `checkExpiry` can flag it.
3. **Expiry rule:** a batch is expired only if `expiryDate < currentDate`. Expiry date = currentDate is still valid (boundary tests T3.4b and T4).
4. **Partial issue of a batch:** `b.setQuantity(b.getQuantity() - remaining)`; the batch stays at the front, no dequeue.
5. **`deleteNode` for a node with two children:** take the smallest node of the right subtree (`FindMin`), copy its `product` and **`batchQueue`** into the node being deleted, then delete the successor node.
   Copying `batchQueue` is safe because the precondition of `delete` is that the queue of the deleted node is **empty** (no batch is lost); there is a test "the successor's queue loses no batch".
6. **Why are `search` / `insert` / `inOrderTraversal` iterative while `delete` is recursive?** Loops use no JVM call stack. The recursive `delete` is easier to read but uses O(h) stack and could still overflow on a very large skewed tree (not tested).
   `inOrderTraversal` was originally recursive too (so `checkExpiry` could only be measured on random trees); the final version is a loop, see point 9.
7. **Two things in the code that Table 8 does not list:** `ProductBST.getRoot()` and `Inventory.getProductBST()` (used by tests and the benchmark). The status labels `EXPIRED` / `NEAR_EXPIRY` correspond to "expired" / "near expiry" in the pseudocode.
8. **Known limitations (stated in section VI.B.4):** the BST is not balanced -> worst case O(n); FIFO by import date does not always match expiry order (FEFO with a priority queue is suggested).
   `issueStock` also builds `toList()` in O(k) on every call; this cost is clearly visible when k is large (Table 14).

9. **Optimization (direction 1): iterative in-order traversal.** The outer loop runs while `current != null` or the stack is not empty. The inner loop walks down the left branch and pushes nodes onto the stack (a `BSTNode[]` array that starts at 64 and doubles by a copy loop when full);
   then it pops the top node, appends it to the list (N) and moves to the right child (R). Every node is pushed and popped exactly once, so the time is Θ(n); the stack holds at most h+1 nodes, so the extra space is O(h), on the heap.
   - *Why change it?* The recursive version overflows the call stack (`StackOverflowError`) at roughly 12,000-16,000 nodes on a skewed tree (h = n); the iterative version works up to 3,000,000 nodes. The benefit is **robustness**, not speed on random trees (measured: no significant change).
   - *The first version* used a linked-list stack and was about 1.3-1.5x slower on a random tree with n = 100,000 because it allocated one object per node; it was changed to an array.
   - *Typical questions:* "Why not upgrade to AVL?" -> AVL adds a height field to `BSTNode`, rotations and a new class diagram; high risk, listed under Future Work. "Does the array stack cost extra space?" -> O(h) <= O(n), the same as the call stack of the recursive version but on the heap.
   - *Tests:* T6a-T6h in `MainTest` (empty tree, one node, 2,000 random nodes compared node by node with the recursive version, an 8-node tree, two calls, right/left skewed trees of 200,000 nodes, `checkExpiry` and the report on a skewed tree of 200,000 nodes).

## How to run
See `README.txt` (Windows PowerShell and Linux/macOS commands).
