package http_datastructures;

public class IllegalHeaderException extends Throwable {
    public String getLine() {
        return line;
    }

    private String line;

    public IllegalHeaderException(String line) {
        this.line = line;
    }
}
