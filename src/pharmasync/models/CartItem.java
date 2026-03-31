package pharmasync.models;

public class CartItem<T, U> {
    private T item;
    private U quantity;

    public CartItem(T item, U quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public T getItem() { return item; }
    public void setItem(T item) { this.item = item; }

    public U getQuantity() { return quantity; }
    public void setQuantity(U quantity) { this.quantity = quantity; }
}
