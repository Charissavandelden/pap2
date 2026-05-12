package nl.topicus.orm;

/**
 * Wordt gegooid wanneer een update geen rijen raakt omdat de version-kolom
 * intussen door een andere transactie is gewijzigd.
 */
public class OptimisticLockException extends RuntimeException {

    public OptimisticLockException(String message) {
        super(message);
    }
}
