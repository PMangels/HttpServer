package http_datastructures;

/**
 * Contains the different supported HTTP request methods.
 */
public enum RequestType {
    GET("GET"), POST("POST"), HEAD("HEAD"), PUT("PUT");

    final String typeString;

    RequestType(String typeString) {
        this.typeString = typeString;
    }
}
