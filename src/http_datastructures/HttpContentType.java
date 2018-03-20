package http_datastructures;

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
