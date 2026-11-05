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
6. **Vì sao `search`/`insert` cài lặp, `delete`/`inOrderTraversal` cài đệ quy?** Lặp giúp không tốn stack (không gian phụ O(1)); `delete` và duyệt LNR đệ quy cho dễ đọc nhưng tốn stack O(h) và có nguy cơ tràn stack trên cây lệch rất lớn
   (đó là lý do `checkExpiry` chỉ được đo trên cây ngẫu nhiên, và là Hướng 1 trong phần tối ưu).
7. **Hai chỗ code có mà Table 8 chưa liệt kê:** `ProductBST.getRoot()` và `Inventory.getProductBST()` (phục vụ kiểm thử/benchmark). Nhãn trạng thái `EXPIRED` / `NEAR_EXPIRY` tương ứng "Đã hết hạn" / "Sắp hết hạn" trong pseudocode.
8. **Hạn chế đã biết (đã nêu ở Mục VI.B.4):** BST không cân bằng → worst case O(n); FIFO theo ngày nhập không luôn trùng thứ tự hết hạn (gợi ý FEFO bằng priority queue).
   `issueStock` còn tạo `toList()` O(k) mỗi lần gọi — đây là chi phí thấy rõ khi k lớn (Table 14).

## Cách chạy
```
cd src && sh build.sh                    # hoặc: javac -d out *.java
java -cp out MainTest                    # 48 PASS, 0 FAIL
java -Xms1g -Xmx2g -cp out Benchmark > ../bench.csv   # 5-8 phút
```
