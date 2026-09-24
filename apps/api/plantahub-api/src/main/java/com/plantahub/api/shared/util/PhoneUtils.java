package com.plantahub.api.shared.util;

public final class PhoneUtils {

    private PhoneUtils() {
    }

    public static String normalize(String phone) {
        if (phone == null) return null;
        return phone.replaceAll("[^0-9]", "");
    }

    public static boolean isValid(String phone) {
        if (phone == null) return false;

        String normalized = normalize(phone);

        // Brasil: geralmente 10 ou 11 dígitos sem DDI
        // com DDI pode chegar a 13
        return normalized.length() >= 10 && normalized.length() <= 13;
    }

    /**
     * Formato E.164 exigido pelos provedores de SMS. Sem DDI (10 ou 11 digitos), assume
     * Brasil. Nulo se o numero nao for valido.
     */
    public static String toE164(String phone) {
        if (!isValid(phone)) return null;

        String normalized = normalize(phone);
        return normalized.length() <= 11 ? "+55" + normalized : "+" + normalized;
    }

    public static String mask(String phone) {
        if (phone == null || phone.isBlank()) return phone;

        String normalized = normalize(phone);

        if (normalized.length() == 11) {
            return "(" + normalized.substring(0, 2) + ") "
                    + normalized.substring(2, 7) + "-"
                    + normalized.substring(7);
        }

        if (normalized.length() == 10) {
            return "(" + normalized.substring(0, 2) + ") "
                    + normalized.substring(2, 6) + "-"
                    + normalized.substring(6);
        }

        return phone;
    }
}