package warehouse.structure;

import warehouse.model.Batch;

import java.util.ArrayList;
import java.util.List;

/** FIFO queue of batches, implemented as a singly linked list with front/rear pointers (java.util.Queue is not used). */
public class BatchQueue {
    private static class Node {
        Batch batch;
        Node next;
        Node(Batch batch) { this.batch = batch; }
    }

    private Node front;
    private Node rear;
    private int size;

    /** O(1): append to the rear of the queue. */
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

    /** O(1): remove and return the front batch; returns null if the queue is empty. */
    public Batch dequeue() {
        if (isEmpty()) return null;
        Batch b = front.batch;
        front = front.next;
        if (front == null) rear = null;
        size--;
        return b;
    }

    /** O(1): return the front batch without removing it; returns null if the queue is empty. */
    public Batch peek() { return isEmpty() ? null : front.batch; }

    public boolean isEmpty() { return size == 0; }

    public int size() { return size; }

    /** O(k): batches in queue order; the queue is not modified. */
    public List<Batch> toList() {
        List<Batch> list = new ArrayList<>(size);
        for (Node p = front; p != null; p = p.next) list.add(p.batch);
        return list;
    }
}
