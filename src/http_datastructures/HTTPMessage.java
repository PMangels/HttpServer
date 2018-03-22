package http_datastructures;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a transaction in the HTTP protocol.
 * Support for different versions, headers and contents.
 */
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

    /**
     * Get the headers for this message.
     * @return A map containing the headers for this message.
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * Get the contents of this message.
     * @return A string containing the contents of the body this message
     */
    public String getContent() {
        return content;
    }

    /**
     * Add a new header to this message.
     * @param key A string containing the left side of this header.
     * @param value A string containing the right side of this header.
     */
   public void addHeader(String key, String value){
        this.headers.put(key.toLowerCase(), value);
   }

    /**
     * Retrieves the value of the header with the provided key.
     * @param key A string containing the left side of the requested header.
     * @return A string containing the right hand side of the requested header,
     *         or null if this header does not exist in this message.
     */
   public String getHeader(String key){
        return this.headers.get(key.toLowerCase());
    }

    public boolean hasHeader(String key){
        return this.headers.containsKey(key.toLowerCase());
    }

    /**
     * Returns the headers of this message as a string.
     * @return The headers of this message, as a string. The format is as
     *         specified in the HTTP standard
     */
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

    /**
     * Returns the version of the HTTP protocol being used for this message.
     * @return The HTTPVersion used for this message.
     */
    public HTTPVersion getVersion() {
        return version;
    }

    /**
     * Initialize a new HTTP message with the provided data,
     * with the content-length header set to the length of the provided content
     * and the content-type header set to the provided contentType string.
     * @param version The HTTPVersion used for this message.
     * @param content The body contents for this message.
     * @param contentType The content-type header for this message.
     */
    public HTTPMessage(HTTPVersion version, String content, String contentType){
        this(version);
        this.setContent(content, contentType);
    }

    /**
     * Initialize a new HTTP message with a provided version.
     * No headers will be set and content will be null.
     * @param version The HTTPVersion used for this message.
     */
    public HTTPMessage(HTTPVersion version){
        this.version = version;
    }

    /**
     * Initialize a new HTTP message by parsing it from the raw HTTP message string.
     * The first line of the request will be saved as is, headers and content will be parsed and set,
     * @param rawMessage The raw HTTP message to be parsed.
     * @throws IllegalHeaderException When the headers in the raw message are malformed and could not be parsed.
     */
    HTTPMessage(String rawMessage) throws IllegalHeaderException {
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

    /**
     * Return this HTTP message as a string.
     * @return This HTTP message as a HTTP transaction string.
     */
    @Override
    public String toString() {
        return firstLine + "\r\n" + headerString() + "\r\n" + content + this.getTerminationString();
    }

    /**
     * Set the content of this message,
     * with the content-length header set to the length of the provided content
     * and the content-type header set to the provided contentType string.
     * @param content The new contents of the body of this message.
     * @param contentType The content-type of this body.
     */
    public void setContent(String content, String contentType) {
        this.content = content;
        this.headers.put("content-length", String.valueOf(content.getBytes().length));
        this.headers.put("content-type", contentType);
    }
}
