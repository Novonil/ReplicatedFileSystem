import java.net.Socket;
import java.security.InvalidParameterException;
import java.time.Instant;
import java.util.*;
import java.io.*;


public class Clients extends Process
{
    //Variable Declaration
    //List of all Servers
    List<Process> allServerList = new ArrayList<Process>();

    //Set Process ID by calling constructor of super class
    public Clients(String Id)
    {
        super(Id);
    }
    
    //Entry Point (Main Method)
    public static void main(String[] args) throws Exception
    {
        //Variable Declaration
        //Default Value of totalMessages = 30 for each client unless entered by user
        int totalMessages = 30;
        String[] listOfFiles = {"f1", "f2", "f3", "f4"};
        String appendMessage;
        Random random = new Random();
        PrintWriter writer;
        BufferedReader reader;
        Instant instantNow = Instant.now();
        
        //Check if user entered client name in the argument (mandatory)
        if (args.length < 1)
        {
            throw new InvalidParameterException("Incorrect parameters for Clients. Enter Parameter <Client Name> <Number of Messages(Optional)>");
        }
        
        
        Clients client = new Clients(args[0]);

        //Check if user entered number of messages (optional)
        if (args.length == 2)
        {
            totalMessages = Integer.parseInt(args[1]);
        }

        //Log Client Start Time
        System.out.println(String.format("CLIENT %s starts at TIME: %s", client.id, instantNow.toEpochMilli()));
        
        //Read config file to learn about servers - hardcoded file name - implement on a config file and read
        client.readFromConfig("config.txt");

        Socket socket = null;

        //Make the client send the required number of messages by selecting random servers and files
        for (int msgNumber = 1; msgNumber <= totalMessages; msgNumber++)
        {
            // Sleep for a random amount of time (Maximum : 1 second)
            Thread.sleep(random.nextInt(1000)); 
            
            // Select random server for sending append request
            int serverInd = random.nextInt(client.allServerList.size());
            Process chosenServer = client.allServerList.get(serverInd);

            //Variable Declaration
            socket = new Socket(chosenServer.ipAddress, chosenServer.portNumber);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Select a random file to append message
            String message = String.format("client %s message %s to server %s", client.id, msgNumber, chosenServer.id);
            String fileName = listOfFiles[random.nextInt(listOfFiles.length)];

            instantNow = Instant.now();

            // Send Client Marker to the server
            writer.println(String.format("client:%s:%s:%s", client.id, fileName, instantNow.toEpochMilli()));

            //Create the Request Message
            appendMessage = String.format(
                "REQ:%s:%s:%s:%s:%s", client.id, chosenServer.id, message, fileName, instantNow.toEpochMilli()
            );

            // Log Client Request Message Details
            System.out.println(String.format("client %s requests: %s for file %s at time: %s",
                    client.id, message, fileName, instantNow.toEpochMilli()
                )
            );

            //Send request message to server
            writer.println(appendMessage);              

            //Read Response from the server
            String reply = reader.readLine();

            //Check if the request raised was completed successfully at the server end
            //ACK - Denotes Success
            //ELSE  FAILURE
            if (reply.equals("ACK")) {
                System.out.println(
                    String.format("Success! Client %s receives an acknowledgment message from %s", client.id, chosenServer.id)
                );
            }
            else {
                System.out.println(String.format("Failure! Client %s receives a failure message from %s: %s", client.id, chosenServer.id, reply));
            }
        }
        System.out.println(String.format("Client %s is switching off", client.id));       
    }

    public void readFromConfig(String fileName) {
        try {
            
            //Variable Declaration
            BufferedReader inputFileBuffer = new BufferedReader(new FileReader(fileName));
            String newLine;
            String[] info;

            System.out.println("Reading Server Info from Config file");

            //Read each Line to learn about servers and add them to the client's server list
            while ((newLine = inputFileBuffer.readLine()) != null) {
                info = newLine.split(" ");
                //Log Server Info
                System.out.println(String.format("Loading server %s, ip=%s, port=%s", info[0], info[1], info[2]));
                this.allServerList.add(new Process(info[0], info[1], Integer.parseInt(info[2])));
            }
            //Close the file reader buffer
            inputFileBuffer.close();
        }
        catch (Exception e)
        {
            System.out.println(String.format("Failed to read server information from file: %s", fileName));
        }  
    }
}

