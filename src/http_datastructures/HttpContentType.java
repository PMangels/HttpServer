package http_datastructures;

/**
 * Defines different
 */
public enum HttpContentType {
    UNDEFINED("Undefined"), IMAGE("Image");

    private String typeString;

    HttpContentType(String typeString) {
        this.typeString = typeString;
    }

    public String getTypeString() {
        return typeString;
    }
}
