package com.plantahub.api.shared.util;

public final class CpfUtils {

    private CpfUtils() {
    }

    public static boolean isValid(String cpf) {
        if (cpf == null) return false;

        String normalized = cpf.replaceAll("[^0-9]", "");

        if (normalized.length() != 11) return false;

        if (allDigitsEqual(normalized)) return false;

        int firstCheck = calculateCheckDigit(normalized, 9, 10);
        int secondCheck = calculateCheckDigit(normalized, 10, 11);

        return firstCheck == Character.getNumericValue(normalized.charAt(9))
                && secondCheck == Character.getNumericValue(normalized.charAt(10));
    }

    private static boolean allDigitsEqual(String cpf) {
        char first = cpf.charAt(0);
        for (int i = 1; i < cpf.length(); i++) {
            if (cpf.charAt(i) != first) {
                return false;
            }
        }
        return true;
    }

    private static int calculateCheckDigit(String cpf, int length, int weightStart) {
        int sum = 0;
        int weight = weightStart;

        for (int i = 0; i < length; i++) {
            int digit = Character.getNumericValue(cpf.charAt(i));
            sum += digit * weight;
            weight--;
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }


    public static String mask(String cpf) {
        if (cpf == null || cpf.length() != 11) return cpf;

        return cpf.substring(0, 3) + "."
                + cpf.substring(3, 6) + "."
                + cpf.substring(6, 9) + "-"
                + cpf.substring(9, 11);
    }
}