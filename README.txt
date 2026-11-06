Group 6 - Warehouse Inventory Management (Phase 4 - Final) - ma nguon + chuong trinh kiem thu + chuong trinh do

Bien dich:    cd src && sh build.sh        (hoac: javac -d out *.java)
Kiem thu:     java -cp out MainTest        (56 kiem thu: 29 tai hien Trace 1, 2, 3.1-3.4, 4, 5 + 19 kiem thu bien bo sung o Phase 4 + 8 kiem thu duyet in-order lap)
              Neu tieng Viet bi loi hien thi tren Windows: chcp 65001 truoc, hoac java -Dstdout.encoding=UTF-8 -cp out MainTest
Do hieu nang: java -Xms1g -Xmx2g -cp out Benchmark > bench.csv   (mat khoang 5-8 phut; khong chay viec khac trong luc do)

Ghi chu:
- Co so cua ma nguon la cai dat tham chieu viet theo thiet ke Phase 2 (Table 8, pseudocode Muc V.A).
  Phase 4 bo sung: kiem tra dau vao null (importDate/expiryDate/currentDate) trong receiveBatch/issueStock,
  19 kiem thu bien (xoa nut la/1 con/goc, so luong am, HSD = currentDate, xuat dung bang ton...), do lai hieu nang.
  Toi uu (Muc VII.B): ProductBST.inOrderTraversal doi tu de quy sang duyet lap bang ngan xep mang tu cai dat
  (het tran stack tren cay lech lon; khong doi chu ky), kem 8 kiem thu (T6a-T6h trong MainTest).
  Viec dung AI de sinh/tham khao ma phai duoc ghi trong AI Audit Log (nop rieng).
- bench.csv hien tai do tren may ao Linux 1 loi (Xeon 2.80 GHz, ~4 GB RAM), OpenJDK 21.0.12.1.
  Hay chay lai Benchmark tren may cua nhom, roi cap nhat Table 13-14 va Figure 9 neu khac.
- Ket qua do tren may ao 1 loi dao dong giua cac lan chay (xem Table 14 va ghi chu o Muc VI.C); hay so sanh xu huong theo n va k.
