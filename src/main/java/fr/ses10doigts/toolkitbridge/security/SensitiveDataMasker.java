package fr.ses10doigts.toolkitbridge.security;

public final class SensitiveDataMasker {

    private static final int VISIBLE_SUFFIX_LENGTH = 4;

    private SensitiveDataMasker() {
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }

        if (value.isEmpty()) {
            return "";
        }

        int length = value.length();
        if (length <= VISIBLE_SUFFIX_LENGTH) {
            return "*".repeat(length);
        }

        int maskedLength = length - VISIBLE_SUFFIX_LENGTH;
        return "*".repeat(maskedLength) + value.substring(maskedLength);
    }
}
