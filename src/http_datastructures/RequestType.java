package http_datastructures;

public enum RequestType {
    GET("GET"), POST("POST"), HEAD("HEAD"), PUT("PUT");

    final String typeString;

    RequestType(String typeString) {
        this.typeString = typeString;
    }
}
