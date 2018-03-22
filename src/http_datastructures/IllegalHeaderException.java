package http_datastructures;

/**
 * A malformed header was found.
 */
public class IllegalHeaderException extends Throwable {
    /**
     * Get the line contents where the error occured.
     * @return The contents of the line where a malformed header was found.
     */
    public String getLine() {
        return line;
    }

    private String line;

    /**
     * Creates an IllegalHeaderException with the provided line.
     * @param line The contents of the line where the exception occurred.
     */
    public IllegalHeaderException(String line) {
        this.line = line;
    }
}
