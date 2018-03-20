package http_server;

import http_datastructures.*;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

//TODO: Threadpool??
class Handler implements Runnable {
    Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    public Response fetchPage(Request request, File f, boolean headersOnly) throws IOException, IllegalHeaderException{
        if(f.exists() && !f.isDirectory()) {
            String dateString = request.getHeader("if-modified-since");
            DateTimeFormatter formatter1 = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
            DateTimeFormatter formatter2 = DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH);
            DateTimeFormatter formatter3 = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
            long ims;
            try{
                ims = LocalDateTime.parse(dateString, formatter1).toEpochSecond(ZoneOffset.UTC);
            }catch (DateTimeParseException e1){
                try {
                    ims = LocalDateTime.parse(dateString, formatter2).toEpochSecond(ZoneOffset.UTC);
                }catch (DateTimeParseException e2){
                    try {
                        ims = LocalDateTime.parse(dateString, formatter3).toEpochSecond(ZoneOffset.UTC);
                    }catch (DateTimeParseException e3){
                        throw new IllegalHeaderException("if-modified-since: " + request.getHeader("if-modified-since"));
                    }

                }
            }
            if (f.lastModified() < ims){
                return new Response(request.getVersion(), 304, "Not Modified", "");
            }
            String content = "";
            if (!headersOnly)
                content = new String(Files.readAllBytes(Paths.get(request.getPath())));
            return new Response(request.getVersion(), 200, "OK", content);
        }else{
            return new Response(request.getVersion(), 404, "Not Found", "The requested file could not be found on this server.");
        }
    }

    public Response getResponse(Request request) throws IOException, IllegalHeaderException {
        File f = new File(request.getPath());

        switch (request.getType()){
            case GET:
                return fetchPage(request, f, false);
            case POST:
                if (f.isDirectory()) {
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.");
                }
                f.createNewFile();
                try (BufferedWriter output = new BufferedWriter(new FileWriter(request.getPath(), true))) {
                    output.append(request.getContent());
                }
                return new Response(request.getVersion(), 200, "OK", "");
            case PUT:
                if (f.isDirectory()) {
                    return new Response(request.getVersion(), 400, "Bad Request", "The requested file could not be written to.");
                }
                f.createNewFile();
                try (BufferedWriter output = new BufferedWriter(new FileWriter(request.getPath(), false))) {
                    output.write(request.getContent());
                }
                return new Response(request.getVersion(), 200, "OK", "");
            case HEAD:
                return fetchPage(request, f, true);
            default:
                return new Response(HTTPVersion.HTTP11, 501, "Not Implemented", "");
        }
    }

    @Override
    public void run(){
        try {
            BufferedReader inFromClient = new BufferedReader(new
                    InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream
                    (socket.getOutputStream());

            String line = inFromClient.readLine();
            StringBuilder requestBuffer = new StringBuilder(line+"\r\n");
            String lastLine = "";
            while (!(line.isEmpty() && lastLine.isEmpty())) {
                line = inFromClient.readLine();
                requestBuffer.append(line + "\r\n");
                lastLine = line;
            }
            Response response;
            try {
                Request request = new Request(requestBuffer.toString());
                response = getResponse(request);
            }catch (IllegalHeaderException e){
                response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your HTTP request headers were malformed and could not be parsed. Error produced on line: " + e.getLine());
            }catch (IllegalRequestException e){
                response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "Your request was not a valid HTTP request and could not be parsed.");
            }catch (UnsupportedHTTPVersionException e){
                response = new Response(HTTPVersion.HTTP11, 400, "Bad Request", "The provided HTTP version is not supported by this server.");
            }catch (UnsupportedHTTPCommandException e){
                response = new Response(HTTPVersion.HTTP11, 501, "Not Implemented", "");
            }catch (Throwable e){
                response = new Response(HTTPVersion.HTTP11, 500, "Server Error", "An internal server error occurred while processing your request. Please try again.");
            }

            // Get the right response and write it to the server.

        }catch (Throwable exception){
            exception.printStackTrace();
        }
    }
}
