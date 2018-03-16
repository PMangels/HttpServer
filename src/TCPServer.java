import java.net.ServerSocket;
import java.net.Socket;

class TCPServer
{
    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(8000);
        while (true)
        {
            Socket connectionSocket = welcomeSocket.accept();
            if (connectionSocket != null) {
                Handler h = new Handler(connectionSocket);
                Thread thread = new Thread(h);
                thread.start();
            }
        }
    }
}



