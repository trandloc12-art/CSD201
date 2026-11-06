# Ghi chú đọc hiểu mã nguồn (để bảo vệ / trả lời vấn đáp)

## Bức tranh chung
`Inventory` → dùng `ProductBST` (tìm sản phẩm theo barcode) → mỗi `BSTNode` giữ 1 `Product` + 1 `BatchQueue` → hàng đợi chứa các `Batch`.
BST lo **tìm sản phẩm** (O(h)); Queue lo **thứ tự FIFO của lô** (O(1)). Hai việc tách rời nhau.

## Các điểm dễ bị hỏi
1. **Vì sao `issueStock` chia 2 giai đoạn?** Giai đoạn 1 chỉ đọc (cộng tồn của các lô chưa hết hạn). Nếu không đủ thì từ chối ngay, hàng đợi không bị đổi.
   Nhờ vậy giai đoạn 2 chắc chắn đủ hàng nên `peek()` không bao giờ gặp hàng đợi rỗng. Không có "xuất dở".
2. **Lô hết hạn ở đầu hàng** chỉ bị loại (dequeue + ghi `expiredList`) khi một lần xuất được chấp nhận. Nếu bị từ chối thì lô hết hạn vẫn nằm trong hàng cho `checkExpiry` gắn cờ.
3. **Quy ước hết hạn:** `expiryDate < currentDate` mới hết hạn. HSD = currentDate vẫn còn hạn (có test biên T3.4b và T4).
4. **Xuất một phần lô:** `b.setQuantity(b.getQuantity() - remaining)`, lô vẫn ở đầu hàng, không dequeue.
5. **`deleteNode` khi nút có 2 con:** lấy nút nhỏ nhất của cây con phải (`FindMin`), chép `product` và **`batchQueue`** sang nút cần xóa rồi xóa nút kế nhiệm.
   Chép `batchQueue` là an toàn vì tiền điều kiện của `delete` là hàng đợi của nút bị xóa **rỗng** (không mất lô nào) — có test "hàng đợi của nút kế nhiệm không mất lô".
6. **Vì sao `search`/`insert`/`inOrderTraversal` cài lặp còn `delete` cài đệ quy?** Lặp không tốn call stack của JVM. `delete` đệ quy cho dễ đọc nhưng tốn stack O(h) và vẫn có nguy cơ tràn stack trên cây lệch rất lớn (chưa kiểm).
   `inOrderTraversal` ban đầu cũng đệ quy (nên `checkExpiry` trước đây chỉ đo được trên cây ngẫu nhiên); Phase 4 đã đổi sang vòng lặp, xem điểm 9.
7. **Hai chỗ code có mà Table 8 chưa liệt kê:** `ProductBST.getRoot()` và `Inventory.getProductBST()` (phục vụ kiểm thử/benchmark). Nhãn trạng thái `EXPIRED` / `NEAR_EXPIRY` tương ứng "Đã hết hạn" / "Sắp hết hạn" trong pseudocode.
8. **Hạn chế đã biết (đã nêu ở Mục VI.B.4):** BST không cân bằng → worst case O(n); FIFO theo ngày nhập không luôn trùng thứ tự hết hạn (gợi ý FEFO bằng priority queue).
   `issueStock` còn tạo `toList()` O(k) mỗi lần gọi — đây là chi phí thấy rõ khi k lớn (Table 14).

9. **Tối ưu Phase 4 (Hướng 1): duyệt in-order lặp.** Vòng lặp ngoài chạy khi `current != null` hoặc ngăn xếp còn phần tử. Vòng trong đi hết nhánh trái và đẩy nút vào ngăn xếp (mảng `BSTNode[]` khởi đầu 64, đầy thì nhân đôi bằng vòng sao chép);
   sau đó lấy nút ở đỉnh, thêm vào danh sách (N), rồi sang con phải (R). Mỗi nút đẩy và lấy đúng một lần nên Θ(n); ngăn xếp tối đa h+1 phần tử nên không gian phụ O(h), nằm trên heap.
   - *Vì sao đổi?* Bản đệ quy tràn call stack (`StackOverflowError`) từ khoảng 12.000–16.000 nút trên cây lệch (h = n); bản lặp chạy đến 3.000.000 nút. Lợi ích là **độ bền**, không phải tốc độ trên cây ngẫu nhiên (số đo: không đổi đáng kể).
   - *Phiên bản đầu* dùng ngăn xếp danh sách liên kết, chậm hơn ~1,3–1,5 lần trên cây ngẫu nhiên n = 100.000 do cấp phát một đối tượng cho mỗi nút; đã đổi sang mảng.
   - *Câu hỏi hay gặp:* "Tại sao không nâng lên AVL?" → AVL thêm chiều cao vào `BSTNode`, phép quay, đổi class diagram; rủi ro cao, ghi ở Future Work. "Ngăn xếp mảng có tốn thêm không?" → O(h) ≤ O(n), tương đương call stack của bản đệ quy nhưng nằm trên heap.
   - *Test:* T6a–T6h trong `MainTest` (cây rỗng, một nút, 2.000 nút ngẫu nhiên đối chiếu từng nút với bản đệ quy, cây 8 nút, gọi hai lần, cây lệch phải/trái 200.000 nút, `checkExpiry` và báo cáo trên cây lệch 200.000 nút).

## Cách chạy
```
cd src && sh build.sh                    # hoặc: javac -d out *.java
java -cp out MainTest                    # 56 PASS, 0 FAIL
java -Xms1g -Xmx2g -cp out Benchmark > ../bench.csv   # 5-8 phút
```
