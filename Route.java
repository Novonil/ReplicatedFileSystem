import java.io.*;
import java.net.*;

public class Route
{
    //Variable Declaration
    Socket socket;
    PrintWriter writer;
    BufferedReader reader;

    //Constructors
    Route(String ipAddress, int portNumber) throws IOException, UnknownHostException
    {
        this.socket = new Socket(ipAddress, portNumber);
        this.writer = new PrintWriter(this.socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    Route(Socket psocket) throws IOException, UnknownHostException
    {
        this.socket = psocket;
        this.writer = new PrintWriter(this.socket.getOutputStream(), true);
        this.reader = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
    }

    //Send Message
    public void sendMessage(String message)
    {
        this.writer.println(message);
    }

    //Receive Message
    public String receiveMessage() throws IOException
    {
        return this.reader.readLine();
    }

    //Close Socket
    public void closeSocket() throws IOException
    {
        this.socket.close();
    }
}