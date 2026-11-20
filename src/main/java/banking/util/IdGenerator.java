package banking.util;

import java.security.SecureRandom;
import java.util.UUID;

public final class IdGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();

    private IdGenerator() { }

    public static String userId() {
        return "USR" + digits(6);
    }

    public static String customerId() {
        return "CUS" + digits(6);
    }

    public static String accountNumber() {
        return "10" + digits(8);
    }

    public static String transactionId() {
        return "TXN" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private static String digits(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
