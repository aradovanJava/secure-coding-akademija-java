package hr.span.akademija.vulnshop.lab13;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Lab13 – "gadget" klasa. Simulira stvarne gadget lance (npr. CommonsCollections
 * iz ysoserial): njezin {@code readObject} izvrsava opasnu operaciju JOS TIJEKOM
 * DESERIJALIZACIJE, prije nego sto aplikacija uopce vidi objekt.
 *
 * Ovdje: zapisuje {@code content} u datoteku {@code reportPath} (proizvoljan zapis
 * datoteke). Dovoljan dokaz da je napadac preuzeo kontrolu nad tokom izvrsavanja.
 */
public class DiagnosticsTask implements Serializable {

    private static final long serialVersionUID = 1L;

    private String reportPath;
    private String content;

    public DiagnosticsTask(String reportPath, String content) {
        this.reportPath = reportPath;
        this.content = content;
    }

    public String getReportPath() {
        return reportPath;
    }

    public String getContent() {
        return content;
    }

    /** RANJIVO: izvrsava se automatski pri deserijalizaciji. */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if (reportPath != null && content != null) {
            Files.writeString(Path.of(reportPath), content);
        }
    }
}
