package hr.span.akademija.vulnshop.lab13;

import java.io.Serializable;

/** Lab13 – legitimna stavka kosarice (naziv proizvoda i kolicina). */
public class CartItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productName;
    private int quantity;

    public CartItem() {
    }

    public CartItem(String productName, int quantity) {
        this.productName = productName;
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
