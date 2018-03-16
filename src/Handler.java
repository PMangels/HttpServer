import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.Socket;

//TODO: Threadpool??
class Handler implements Runnable {
    Socket socket;

    public Handler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader inFromClient = new BufferedReader(new
                    InputStreamReader(socket.getInputStream()));
            DataOutputStream outToClient = new DataOutputStream
                    (socket.getOutputStream());

            String line = inFromClient.readLine();
            StringBuffer requestBuffer = new StringBuffer(line);
            String lastLine = "";
            while (!(line.isEmpty() && lastLine.isEmpty())) {
                line = inFromClient.readLine();
                requestBuffer.append(line + "\n");
                lastLine = line;
            }
            System.out.println(requestBuffer.toString());

        }catch (Exception exception){
            System.out.println(exception.getMessage());
        }
    }
}
