package http_datastructures;

public class Request extends HTTPMessage {

    private final RequestType type;
    private final String path;

    public RequestType getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Request(RequestType type, String path, HTTPVersion version) {
        super(version);

        this.type = type;
        if (!path.startsWith("/"))
            path = "/" + path;

        this.path = path;
    }

    public Request(RequestType type, String path, HTTPVersion version, String content, String contentType) {
        super(version, content, contentType);

        this.type = type;
        if (!path.startsWith("/"))
            path = "/" + path;

        this.path = path;
    }

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

    @Override
    public String toString() {
        this.firstLine = this.type.typeString + " " + this.path + " " + this.getVersion().versionString;
        return super.toString();
    }
}
