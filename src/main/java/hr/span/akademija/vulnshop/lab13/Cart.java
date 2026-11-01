package hr.span.akademija.vulnshop.lab13;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Lab13 – legitimna klasa kosarice koja se (ranjivo) prima kao Java-serijalizirani
 * objekt. Sama po sebi nije opasna; problem je u tome sto {@code ObjectInputStream}
 * moze deserijalizirati BILO KOJU klasu s classpatha (vidi {@link DiagnosticsTask}).
 */
public class Cart implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<CartItem> items = new ArrayList<>();

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public void addItem(CartItem item) {
        this.items.add(item);
    }
}
