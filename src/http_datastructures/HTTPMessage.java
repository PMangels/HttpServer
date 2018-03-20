package http_datastructures;

import java.util.HashMap;
import java.util.Map;

public class HTTPMessage {

    private String content;
    HTTPVersion version;
    private Map<String, String> headers = new HashMap<>();
    String firstLine;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getContent() {
        return content;
    }

   public void addHeader(String key, String value){
        this.headers.put(key, value);
   }

   public String getHeader(String key){
        return this.headers.get(key.toLowerCase());
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

    public HTTPMessage(HTTPVersion version, String content){
        this.version = version;
        this.content = content;
    }

    public HTTPMessage(String rawMessage) throws IllegalHeaderException{
        String[] parts = rawMessage.split("\r\n\r\n", 2);
        if (parts.length == 2){
            this.content = parts[1];
        }

        String[] headerParts = parts[0].split("\r\n", 2);
        this.firstLine = headerParts[0];

        if (headerParts.length > 1) {
            for (String line : headerParts[1].split("\r\n")) {
                String[] lineParts = line.split(":", 2);
                if (lineParts.length == 1){
                    throw new IllegalHeaderException(line);
                }
                headers.put(lineParts[0].toLowerCase(), lineParts[1]);
            }
        }
    }

    @Override
    public String toString() {
        return firstLine + "\r\n" + headerString() + "\r\n" + content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
