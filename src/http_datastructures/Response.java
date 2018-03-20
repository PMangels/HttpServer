package http_datastructures;

public class Response extends HTTPMessage {

    private final int statusCode;
    private final String status;

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatus() {
        return status;
    }

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

        this.statusCode = Integer.parseInt(firstLine[1]);
        this.status = firstLine[2];
    }

    public Response(HTTPVersion version, int statusCode, String status, String content){
        super(version, content);

        this.statusCode = statusCode;
        this.status = status;
    }

    @Override
    public String toString() {
        this.firstLine = this.version.versionString + " " + this.statusCode + " " + this.status;
        return super.toString();
    }
}
