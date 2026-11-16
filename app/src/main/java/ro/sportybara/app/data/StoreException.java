package ro.sportybara.app.data;

public class StoreException extends RuntimeException {

    public enum Reason {
        EMPTY_SLIP,
        INVALID_STAKE,
        INSUFFICIENT_FUNDS,
        NOT_FOUND
    }

    private final Reason reason;

    public StoreException(Reason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public Reason getReason() {
        return reason;
    }
}
