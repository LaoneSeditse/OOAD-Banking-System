package banking.exception;

public class DataAccessException extends BankException {
    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
