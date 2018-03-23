package http_datastructures;

public class Response extends HTTPMessage {

    private final int statusCode;
    private final String status;

    /**
     * Get the status code for this response.
     * @return The status code for this response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Get the status message for this response.
     * @return The status message for this response.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Initialize a response from the raw HTTP response string.
     * @param rawResponse The HTTP response as a string.
     * @throws UnsupportedHTTPVersionException The HTTP version in this response is not supported.
     * @throws IllegalHeaderException A header in this response string was malformed.
     * @throws IllegalResponseException The provided response string was malformed.
     */
    public Response(String rawResponse) throws UnsupportedHTTPVersionException, IllegalHeaderException, IllegalResponseException {
        super(rawResponse);

        String[] firstLine = this.firstLine.split(" ", 3);
        if (firstLine.length < 3){
            throw new IllegalResponseException();
        }

        switch (firstLine[0]){
            case "HTTP/1.1":
                this.version = HTTPVersion.HTTP11;
                break;
            case "HTTP/1.0":
                this.version = HTTPVersion.HTTP10;
                break;
            default:
                throw new UnsupportedHTTPVersionException();
        }
        try {
            this.statusCode = Integer.parseInt(firstLine[1]);
        }catch (NumberFormatException e){
            throw new IllegalResponseException();
        }
        this.status = firstLine[2];
    }

    /**
     * Initialize a response with the provided data.
     * The content-length header will be set to the provided contents length.
     * The content-type header will be set to the provided contentType string.
     * @param version The HTTP version for this response.
     * @param statusCode The HTTP status code for this response.
     * @param status The HTTP status string for this response.
     * @param content The body contents for this response.
     * @param contentType The content-type header for this response.
     */
    public Response(HTTPVersion version, int statusCode, String status, String content, String contentType){
        super(version, content, contentType);

        this.statusCode = statusCode;
        this.status = status;
    }

    /**
     * Initialize a response with the provided data.
     * The content of this response will be null.
     * @param version The HTTP version for this response.
     * @param statusCode The HTTP status code for this response.
     * @param status The HTTP status string for this response.
     */
    public Response(HTTPVersion version, int statusCode, String status){
        super(version);

        this.statusCode = statusCode;
        this.status = status;
    }

    /**
     * Get this response as a HTTP response string.
     * @return This HTTP response as a string.
     */
    @Override
    public String toString() {
        this.firstLine = this.version.versionString + " " + this.statusCode + " " + this.status;
        return super.toString();
    }
}
