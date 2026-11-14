package searchoteca.components;

/**
 * Validação de ISBN (RF05). Aceita ISBN-10 e ISBN-13, com ou sem hífens e espaços,
 * e confere o dígito verificador: um ISBN digitado errado é recusado antes de qualquer consulta.
 */
public final class Isbn {

    private Isbn() {}

    /** Tira hífens e espaços: "978-85-359-1484-9" -> "9788535914849". O X final do ISBN-10 fica maiúsculo. */
    public static String normalize(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("[\\s-]", "").toUpperCase();
    }

    /** O valor (já normalizado) é um ISBN-10 ou ISBN-13 com dígito verificador correto? */
    public static boolean isValid(String isbn) {
        if (isbn == null) return false;
        if (isbn.matches("\\d{13}")) return validIsbn13(isbn);
        if (isbn.matches("\\d{9}[\\dX]")) return validIsbn10(isbn);
        return false;
    }

    /** ISBN-13: pesos 1 e 3 alternados; a soma tem que ser múltipla de 10. */
    private static boolean validIsbn13(String isbn) {
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }

    /** ISBN-10: pesos de 10 a 1 (o X vale 10); a soma tem que ser múltipla de 11. */
    private static boolean validIsbn10(String isbn) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn.charAt(i);
            int value = (c == 'X') ? 10 : c - '0';
            sum += value * (10 - i);
        }
        return sum % 11 == 0;
    }
}
