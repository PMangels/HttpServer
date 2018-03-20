package http_datastructures;

public enum HTTPVersion {
    HTTP11("HTTP/1.1"), HTTP10("HTTP/1.0");

    final String versionString;

    HTTPVersion(String s) {
        this.versionString = s;
    }
}
