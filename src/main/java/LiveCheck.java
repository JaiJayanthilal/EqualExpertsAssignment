import org.service.PriceApiClient;
import org.util.Cart;

public class LiveCheck {
    public static void main(String[] args) throws Exception {
        Cart cart = new Cart(new PriceApiClient());
        cart.addProduct("cornflakes", 1);
        cart.addProduct("cornflakes", 1);
        cart.addProduct("weetabix", 1);
        cart.getItems().forEach(i -> System.out.println("Cart contains " + i.getQuantity() + " x " + i.getProduct()));
        System.out.println("Subtotal = " + cart.getSubtotal() + ", Tax = " + cart.getTax() + ", Total = " + cart.getTotal());
        try { cart.addProduct("unknown", 1); } catch (Exception e) { System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage()); }
    }
}