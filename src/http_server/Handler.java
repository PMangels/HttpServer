package http_server;

import http_datastructures.*;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import static java.util.Base64.getDecoder;

/**
 * Simple handler class that handles a single HTTP connection.
 */
class Handler implements Runnable {

    private static final List<String> imageExtensions = Arrays.asList("jpeg", "jpg","png", "bmp", "wbmp", "gif");
    private static final List<String> textExtensions = Arrays.asList("txt", "html", "js", "css");
    Socket socket;

    /**
     * Initialise a Handler and give it the socket as its socket.
     * @param socket The socket this class handles.
     */
    public Handler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Let's the handler handle the HTTP connection.
     */
    @Override
    public void run(){
        try {
            DataInputStream inFromClient = new DataInputStream(socket.getInputStream());
            DataOutputStream outToClient = new DataOutputStream(socket.getOutputStream());
            boolean shouldClose = false;
            while (!shouldClose) {
                Response response;
                try {
                    StringBuilder requestBuffer = new StringBuilder();

                    while (!requestBuffer.toString().endsWith("\r\n\r\n")) {
                        requestBuffer.append((char) inFromClient.readByte());
                    }

                    int length = 0;
                    boolean chunked= false;
                    for (String line : requestBuffer.toString().split("\r\n")) {
                        if (line.toLowerCase().startsWith("content-length:")) {
                            String[] lineParts = line.split(":");
                            if (lineParts.length != 2)
                                throw new IllegalHeaderException(line);
                            try {
                                length = Integer.parseInt(lineParts[1].trim());
                            }catch (NumberFormatException e){
                                throw new IllegalHeaderException(line);
                            }
                        }
                        if (line.toLowerCase().startsWith("transfer-encoding:") && line.toLowerCase().contains("chunked")){
                            chunked = true;
                        }
                    }

                    byte[] bytes;
                    if (chunked){
                        bytes = readBodyChunked(inFromClient);
                        do{
                            requestBuffer.insert(requestBuffer.length()-2,(char) inFromClient.readByte());
                        }while (!requestBuffer.toString().endsWith("\r\n\r\n\r\n"));
                        requestBuffer.delete(requestBuffer.length()-2,requestBuffer.length());
                    }else{
                        bytes = readBody(inFromClient, length);
                    }

                    requestBuffer.append(new String(bytes, "UTF-8"));
                    Request request = new Request(requestBuffer.toString());
                    response = getResponse(request);
                    if ("close".equals(request.getHeader("connection")) || request.getVersion() == HTTPVersion.HTTP10) {
                        shouldClose = true;
                    }
                } catch (IllegalHeaderException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your HTTP request headers were malformed and could not be parsed. Error produced on line: " + e.getLine() + "\r\n", "text/plain");
                } catch (IllegalRequestException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your request was not a valid HTTP request and could not be parsed.\r\n", "text/plain");
                } catch (UnsupportedHTTPVersionException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "The provided HTTP version is not supported by this server.\r\n", "text/plain");
                } catch (UnsupportedHTTPCommandException e) {
                    response = new Response(HTTPVersion.HTTP11, 501, "Not Implemented");
                }catch (SocketTimeoutException | SocketException | EOFException e){
                    break;
                }
                catch (Throwable e) {
                    response = new Response(HTTPVersion.HTTP11, 500, "Server Error", "An internal server error occurred while processing your request. Please try again.\r\n", "text/plain");
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                response.addHeader("Date", ZonedDateTime.now(ZoneId.of("GMT")).format(formatter));
                if (response.hasHeader("content-type")&& response.getHeader("content-type").contains("image"))  {
                    byte[] bytes = getDecoder().decode(response.getContent().getBytes());
                    response.addHeader("content-length", String.valueOf(bytes.length));
                    outToClient.writeBytes(response.getVersion().toString() + " " + response.getStatusCode() + " " + response.getStatus() + "\r\n");
                    outToClient.writeBytes(response.headerString());
                    outToClient.writeBytes("\r\n");
                    outToClient.write(bytes, 0, bytes.length);
                }else {
                    System.out.println(response.toString());
                    outToClient.writeBytes(response.toString());
                }
            }

            socket.close();
            System.out.println("Socket was closed.");

        }catch (Throwable exception){
            exception.printStackTrace();
        }
    }

    /**
     * Processes the request and returns a response object of this servers response to that request.
     * @param request The given request
     * @return the response from this server.
     * @throws IOException There is a exception when accessing the file that is requested
     * @throws IllegalHeaderException The request has an illegal or malformed header
     * @throws IllegalRequestException The request is illegal or malformed
     */
    private Response getResponse(Request request) throws IOException, IllegalHeaderException, IllegalRequestException {

        if (request.getVersion() == HTTPVersion.HTTP11 && request.getHeader("host") == null)
            return new Response(HTTPVersion.HTTP11, 400, "Bad Request", "HTTP 1.1 requests must include the Host: header\r\n", "text/plain");

        String path = request.getPath();
        if (path.startsWith("/"))
            path = path.substring(1);
        if (path.startsWith("http://")) {
            try {
                path = new URI(path).getPath();
            } catch (URISyntaxException e) {
                throw new IllegalRequestException();
            }
        }

        if (path.isEmpty() && HTTPVersion.HTTP11.equals(request.getVersion())){
            Response response = new Response(request.getVersion(),303,"See Other");
            response.addHeader("Location","/index.html");
            return response;
        }

        String absolutePath = System.getProperty("user.dir") + "/public_html/" + path;
        File f = new File(absolutePath);

        switch (request.getType()){
            case GET:
                return fetchPage(request, f, false);
            case POST:
                if (f.isDirectory()) {
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.\r\n", "text/plain");
                }
                String writingContent = request.getContent();
                if(!f.createNewFile()){
                    writingContent = "\r\n"+writingContent;
                }
                try (BufferedWriter output = new BufferedWriter(new FileWriter(absolutePath, true))) {
                    output.append(writingContent);
                }
                return new Response(request.getVersion(), 200, "OK");
            case PUT:
                if (f.isDirectory()) {
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.\r\n", "text/plain");
                }
                f.createNewFile();
                try (BufferedWriter output = new BufferedWriter(new FileWriter(absolutePath, false))) {
                    output.append(request.getContent());
                }
                return new Response(request.getVersion(), 200, "OK");
            case HEAD:
                return fetchPage(request, f, true);
            default:
                return new Response(HTTPVersion.HTTP11, 501, "Not Implemented");
        }
    }

    /**
     * Returns the response with the content of the given file requested by a given request. If headersOnly is true,
     * only the headers will be included in the response, not the actual content.
     * @param request The request which asked for this file.
     * @param file The file we want to fetch
     * @param headersOnly A boolean to indicate a HEAD request.
     * @return a Response with it's content the content of the file, except when headersOnly is true - in this case
     * only the headers are returned without actual content - or when the request has a header "if-modified-since" or
     * "if-unmodified-since" and the file is not modified since, respectively modified since the given date - then it
     * will return a response with a statuscode 304 or with a 412 statuscode respectively.
     * @throws IOException There is a exception when accessing the file that is requested
     * @throws IllegalHeaderException The request has an illegal or malformed header
     */
    public Response fetchPage(Request request, File file, boolean headersOnly) throws IOException, IllegalHeaderException{
        if(file.exists() && !file.isDirectory()) {
            boolean unModified = false;
            String dateString = null;
            String modifiedString = request.getHeader("if-modified-since");
            String unModifiedString = request.getHeader("if-unmodified-since");
            if (unModifiedString != null){
                dateString = unModifiedString;
                unModified = true;
            }
            if (modifiedString != null){
                dateString = modifiedString;
            }
            if (dateString != null) {
                DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH);
                DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
                long ims;
                try {
                    ims = LocalDateTime.parse(dateString, formatter1).toEpochSecond(ZoneOffset.UTC);
                } catch (DateTimeParseException e1) {
                    try {
                        ims = LocalDateTime.parse(dateString, formatter2).toEpochSecond(ZoneOffset.UTC);
                    } catch (DateTimeParseException e2) {
                        try {
                            ims = LocalDateTime.parse(dateString, formatter3).toEpochSecond(ZoneOffset.UTC);
                        } catch (DateTimeParseException e3) {
                            String headerKey = "if-modified-since";
                            if (unModified){
                                headerKey = "if-unmodified-since";
                            }
                            throw new IllegalHeaderException(headerKey + ": " + request.getHeader(headerKey));
                        }

                    }
                }
                if (file.lastModified()/1000 < ims && !unModified) {
                    return new Response(request.getVersion(), 304, "Not Modified");
                }else if (file.lastModified()/1000 > ims && unModified){
                    return new Response(request.getVersion(), 412, "Precondition Failed");
                }
            }
            String content;
            String contentType = "undefined";
            String extension = parseExtension(file.getName());
            if(imageExtensions.contains(extension)){
                contentType = "image/"+extension+"; charset=utf-8";
                content = new String(Base64.getEncoder().encode(Files.readAllBytes(Paths.get(file.getAbsolutePath()))));
            }else {
                content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                if (textExtensions.contains(extension)) {
                    switch (extension) {
                        case "txt":
                            contentType = "text/plain"+"; charset=utf-8";
                            break;
                        case "js":
                            contentType = "text/javascript"+"; charset=utf-8";
                            break;
                        default:
                            contentType = "text/" + extension+"; charset=utf-8";
                            break;
                    }
                }
            }

            Response response = new Response(request.getVersion(), 200, "OK", content, contentType);
            if (headersOnly){
                String length = response.getHeader("content-length");
                response.setContent("", response.getHeader("content-type"));
                response.addHeader("content-length",  length);
            }
            return response;
        }else{
            return new Response(request.getVersion(), 404, "Not Found", "The requested file could not be found on this server.\r\n", "text/plain");
        }
    }

    /**
     * Returns the extension a file with the given path has. If it has no extension an empty string is returned.
     * @param path The given path
     * @return The substring behind the last "/" and behind the last "."
     */
    public String parseExtension(String path){
        String filename;
        try {
            String[] pathSplit = path.split("/");
            filename = pathSplit[pathSplit.length - 1];
        } catch (IndexOutOfBoundsException e){
            filename = path;
        }
        if (filename.contains(".")){
            try {
                String[] filenameSplit = filename.split("\\.");
                return filenameSplit[filenameSplit.length - 1].toLowerCase();
            } catch (IndexOutOfBoundsException e){
                return  "";
            }
        } else {
            return  "";
        }
    }

    /**
     * This will read the body of a Chunked HTTP message from the given inputStream and returns it in a buffer.
     * @param inputStream The inputStream from which to read
     * @return a byte array with the input from a Chunked HTTP message in the inputStream.
     * @throws IOException Reading from the inputStream failed.
     */
    private byte[] readBodyChunked(DataInputStream inputStream) throws IOException {
        int length = -1;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (length != 0){
            StringBuilder responseBuffer = new StringBuilder();
            while (!responseBuffer.toString().endsWith("\r\n")) {
                responseBuffer.append((char) inputStream.readByte());
            }
            String[] firstline = responseBuffer.toString().split(";");
            length = Integer.parseInt(firstline[0].replace("\r\n",""), 16);
            buffer.write(readBody(inputStream, length));
            if (length!=0) {
                inputStream.readByte();
                inputStream.readByte();
            }
        }
        return buffer.toByteArray();
    }

    /**
     * Read from the inputStream for a length "length" and returns it in a byte array.
     * @param inputStream The inputStream from which to read
     * @param length The length for which to read
     * @return a byte array with the input from the inputStream and length "length".
     * @throws IOException
     */
    private byte[] readBody(DataInputStream inputStream, int length) throws IOException {
        int byteCount = 0;
        byte[] bytes = new byte[length];
        while (byteCount != length) {
            byteCount += inputStream.read(bytes, byteCount, length - byteCount);
        }
        return bytes;
    }
}
