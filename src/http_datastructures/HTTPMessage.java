package http_datastructures;

import java.util.HashMap;
import java.util.Map;

public class HTTPMessage {

    private String content = "";
    HTTPVersion version;
    private Map<String, String> headers = new HashMap<>();
    String firstLine;
    private String terminationString = "";

    public String getTerminationString() {
        return terminationString;
    }

    public void setTerminationString(String terminationString) {
        this.terminationString = terminationString;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getContent() {
        return content;
    }

    public void addHeader(String key, String value){
        this.headers.put(key.toLowerCase(), value);
    }

    public String getHeader(String key){
        return this.headers.get(key.toLowerCase());
    }

    public boolean hasHeader(String key){
        return this.headers.containsKey(key.toLowerCase());
    }

    public String headerString(){
        StringBuilder output = new StringBuilder();
        for (Map.Entry<String, String> header : this.headers.entrySet()) {
            output.append(header.getKey());
            output.append(": ");
            output.append(header.getValue());
            output.append("\r\n");
        }
        return output.toString();
    }

    public HTTPVersion getVersion() {
        return version;
    }

    public HTTPMessage(HTTPVersion version, String content, String contentType){
        this(version);
        this.setContent(content, contentType);
    }

    public HTTPMessage(HTTPVersion version){
        this.version = version;
    }

    public HTTPMessage(String rawMessage) throws IllegalHeaderException {
        String[] parts = rawMessage.split("\r\n\r\n", 2);
        if (parts.length == 2){
            this.content = parts[1];
        }

        String[] headerParts = parts[0].split("\r\n", 2);
        this.firstLine = headerParts[0];

        if (headerParts.length > 1) {
            String lastHeader = "";
            for (String line : headerParts[1].split("\r\n")) {
                String[] lineParts = line.split(":", 2);
                if (lineParts.length == 1){
                    if (headers.size() == 0)
                        throw new IllegalHeaderException(line);

                    headers.put(lastHeader.toLowerCase(), headers.get(lastHeader) + lineParts[0].trim());
                }
                lastHeader = lineParts[1].trim();
                headers.put(lineParts[0].toLowerCase(), lastHeader);
            }
        }
    }

    @Override
    public String toString() {
        return firstLine + "\r\n" + headerString() + "\r\n" + content + this.getTerminationString();
    }

    public void setContent(String content, String contentType) {
        this.content = content;
        this.headers.put("content-length", String.valueOf(content.getBytes().length));
        this.headers.put("content-type", contentType);
    }
}
