import java.util.ArrayList;
import java.util.List;

/** Hàng đợi FIFO các lô, cài đặt bằng danh sách liên kết đơn front/rear (không dùng java.util.Queue). */
public class BatchQueue {
    private static class Node {
        Batch batch;
        Node next;
        Node(Batch batch) { this.batch = batch; }
    }

    private Node front;
    private Node rear;
    private int size;

    /** O(1): thêm vào cuối hàng đợi. */
    public void enqueue(Batch batch) {
        Node node = new Node(batch);
        if (isEmpty()) {
            front = node;
            rear = node;
        } else {
            rear.next = node;
            rear = node;
        }
        size++;
    }

    /** O(1): lấy và xóa lô ở đầu; trả về null nếu rỗng. */
    public Batch dequeue() {
        if (isEmpty()) return null;
        Batch b = front.batch;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return b;
    }

    /** O(1): xem lô ở đầu; trả về null nếu rỗng. */
    public Batch peek() { return isEmpty() ? null : front.batch; }

    public boolean isEmpty() { return size == 0; }

    public int size() { return size; }

    /** O(k): danh sách các lô theo thứ tự hàng đợi, không làm thay đổi hàng đợi. */
    public List<Batch> toList() {
        List<Batch> list = new ArrayList<>(size);
        for (Node p = front; p != null; p = p.next) list.add(p.batch);
        return list;
    }
}
