package http_server;

import http_datastructures.*;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

//TODO: Threadpool??
class Handler implements Runnable {
    private static final List<String> imageExtensions = Arrays.asList("jpeg", "jpg","png", "bmp", "wbmp", "gif");
    private static final List<String> textExtensions = Arrays.asList("txt", "html", "js", "css");



    Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    //TODO: handle chunckes
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

                    int length = parseRequestLength(requestBuffer.toString());

                    int byteCount = 0;
                    byte[] bytes = new byte[length];
                    while (byteCount != length) {
                        byteCount += inFromClient.read(bytes, byteCount, length - byteCount);
                    }
                    String byteString = new String(bytes,"UTF-8");
                    requestBuffer.append(byteString);
                    Request request = new Request(requestBuffer.toString());
                    response = getResponse(request);
                    if ("close".equals(request.getHeader("connection")) || request.getVersion() == HTTPVersion.HTTP10) {
                        shouldClose = true;
                    }
                } catch (IllegalHeaderException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your HTTP request headers were malformed and could not be parsed. Error produced on line: " + e.getLine(), "text/plain");
                } catch (IllegalRequestException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your request was not a valid HTTP request and could not be parsed.", "text/plain");
                } catch (UnsupportedHTTPVersionException e) {
                    response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "The provided HTTP version is not supported by this server.", "text/plain");
                } catch (UnsupportedHTTPCommandException e) {
                    response = new Response(HTTPVersion.HTTP11, 501, "Not Implemented");
                }catch (SocketTimeoutException | SocketException | EOFException e){
                    break;
                }
                catch (Throwable e) {
                    response = new Response(HTTPVersion.HTTP11, 500, "Server Error", "An internal server error occurred while processing your request. Please try again.", "text/plain");
                }
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
                response.addHeader("Date", ZonedDateTime.now(ZoneId.of("GMT")).format(formatter));
                System.out.println(response.toString());
                outToClient.writeBytes(response.toString());
            }

            socket.close();

        }catch (Throwable exception){
            exception.printStackTrace();
        }
    }

    public Response getResponse(Request request) throws IOException, IllegalHeaderException {

        if (request.getVersion() == HTTPVersion.HTTP11 && request.getHeader("host") == null)
            return new Response(HTTPVersion.HTTP11, 400, "Bad Request", "HTTP 1.1 requests must include the Host: header", "text/plain");

        String path = request.getPath();
        if (path.startsWith("/"))
            path = path.substring(1);

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
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.", "text/plain");
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
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.", "text/plain");
                }
                f.createNewFile();
                try (BufferedWriter output = new BufferedWriter(new FileWriter(absolutePath, false))) {
                    output.write(request.getContent());
                }
                return new Response(request.getVersion(), 200, "OK");
            case HEAD:
                return fetchPage(request, f, true);
            default:
                return new Response(HTTPVersion.HTTP11, 501, "Not Implemented");
        }
    }

    public Response fetchPage(Request request, File f, boolean headersOnly) throws IOException, IllegalHeaderException{
        if(f.exists() && !f.isDirectory()) {
            String dateString = request.getHeader("if-modified-since");
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
                            throw new IllegalHeaderException("if-modified-since: " + request.getHeader("if-modified-since"));
                        }

                    }
                }
                if (f.lastModified() < ims) {
                    return new Response(request.getVersion(), 304, "Not Modified");
                }
            }
            String content = new String(Files.readAllBytes(Paths.get(f.getAbsolutePath())));
            String contentType = "undefined";
            String extension = parseExtension(f.getName());
            if(imageExtensions.contains(extension)){
                contentType = "image/"+extension;
            }else if (textExtensions.contains(extension)) {
                switch (extension) {
                    case "txt":
                        contentType = "text/plain";
                        break;
                    case "js":
                        contentType = "text/javascript";
                        break;
                    default:
                        contentType = "text/" + extension;
                        break;
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
            return new Response(request.getVersion(), 404, "Not Found", "The requested file could not be found on this server.", "text/plain");
        }
    }

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

    private int parseRequestLength(String request) throws IllegalHeaderException {
        for (String line : request.split("\r\n")) {
            if (line.toLowerCase().startsWith("content-length:")) {
                String[] lineParts = line.split(":");
                if (lineParts.length != 2)
                    throw new IllegalHeaderException(line);
                try {
                    return Integer.parseInt(lineParts[1].trim());
                }catch (NumberFormatException e){
                    throw new IllegalHeaderException(line);
                }
            }
        }
        return 0;
    }


}
