package http_datastructures;

public class Request extends HTTPMessage {

    private final RequestType type;
    private final String path;

    /**
     * Get the HTTP request type.
     * @return The RequestType for this request.
     */
    public RequestType getType() {
        return type;
    }

    /**
     * Get the path of this request.
     * @return The path of the resource requested.
     */
    public String getPath() {
        return path;
    }

    /**
     * Create a request with the provided data. Contents will be set to null,
     * no headers will be set.
     * @param type The HTTP request type.
     * @param path The path for this request.
     * @param version The HTTP version to be used with this request.
     */
    public Request(RequestType type, String path, HTTPVersion version) {
        super(version);

        this.type = type;
        if (!path.startsWith("/"))
            path = "/" + path;

        this.path = path;
    }

    /**
     * Create a request with the provided data.
     * Content-type headers will be set with the provided contentType string,
     * Content-length headers will be set based on the provided contents length.
     * @param type The HTTP request type.
     * @param path The path for this request.
     * @param version The HTTP version to be used with this request.
     * @param content The contents for the body of this request.
     * @param contentType The content-type header for this request.
     */
    public Request(RequestType type, String path, HTTPVersion version, String content, String contentType) {
        super(version, content, contentType);

        this.type = type;
        if (!path.startsWith("/"))
            path = "/" + path;

        this.path = path;
    }

    /**
     * Create a new request by parsing it from a raw HTTP transaction string.
     * @param requestString The raw HTTP request string.
     * @throws UnsupportedHTTPCommandException When the provided HTTP request type is not supported.
     * @throws UnsupportedHTTPVersionException When the provided HTTP version is not supported.
     * @throws IllegalHeaderException When malformed headers were found.
     * @throws IllegalRequestException When the request was malformed.
     */
    public Request(String requestString) throws UnsupportedHTTPCommandException, UnsupportedHTTPVersionException, IllegalHeaderException, IllegalRequestException {
        super(requestString);
        String[] firstLine = this.firstLine.split(" ");
        if (firstLine.length < 3){
            throw new IllegalRequestException();
        }
        try {
            this.type = RequestType.valueOf(firstLine[0]);
        }catch (IllegalArgumentException e){
            throw new UnsupportedHTTPCommandException();
        }

        this.path = firstLine[1];

        switch (firstLine[2]){
            case "HTTP/1.1":
                this.version = HTTPVersion.HTTP11;
                break;
            case "HTTP/1.0":
                this.version = HTTPVersion.HTTP10;
                break;
            default:
                throw new UnsupportedHTTPVersionException();
        }

    }

    /**
     * Return this request object as a HTTP request string.
     * @return The HTTP request string representing this request.
     */
    @Override
    public String toString() {
        this.firstLine = this.type.typeString + " " + this.path + " " + this.version.versionString;
        return super.toString();
    }

}
