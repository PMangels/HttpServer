package http_datastructures;

/**
 * Enum describing versions of the HTTP protocol: HTTP 1.1 and HTTP 1.0
 */
public enum HTTPVersion {
    HTTP11("HTTP/1.1"), HTTP10("HTTP/1.0");

    final String versionString;

    HTTPVersion(String s) {
        this.versionString = s;
    }

    @Override
    public String toString() {
        return this.versionString;
    }
}
