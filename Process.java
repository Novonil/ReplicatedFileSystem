import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class Process
{
    //Variable Declaration
    public int portNumber;
    public String id, ipAddress;
    static String[] listOfFiles = {"f1", "f2", "f3", "f4"};
    public Map<String, Integer> fileSequenceSendReference;
    public Map<String, Integer> fileSequenceReceiveReference;
    public Map<String, Object> fileLockSendReference;
    public Map<String, Object> fileLockReceiveReference;


    //Client Constructor
    public Process(String Id) {
        this.id = Id;
    }

    //Serer Constructor
    public Process(String Id, String IpAddress, int pNum) { 
        this.id = Id;
        this.ipAddress = IpAddress;
        this.portNumber = pNum;
        
        this.fileSequenceSendReference = new ConcurrentHashMap<String, Integer>(Process.listOfFiles.length);
        this.fileSequenceReceiveReference = new ConcurrentHashMap<String, Integer>(Process.listOfFiles.length);
        this.fileLockSendReference = new ConcurrentHashMap<String, Object>(Process.listOfFiles.length);
        this.fileLockReceiveReference = new ConcurrentHashMap<String, Object>(Process.listOfFiles.length);

        for (String fileName : Process.listOfFiles) {
            fileLockSendReference.put(fileName, new Object());
            fileLockReceiveReference.put(fileName, new Object());
        }
    }

    //Send Message
    public void sendMess(Route rt, int seqNumber, String mess) throws IOException {
        rt.sendMessage(String.format("%s:%s", seqNumber, mess));
    }

    //Receive Message
    public String receiveMess(Route rt, String fileName) throws IOException, InterruptedException {
       
        String reply = rt.receiveMessage();

        // Separate the sequence Number and message
        String[] params = reply.split(":", 2);

        int seqNumber = Integer.parseInt(params[0]);
        String message = params[1];

        // Block Thread till messages with previous sequence numbers have been received
        while (this.fileSequenceReceiveReference.getOrDefault(fileName, 0) != seqNumber) {
            Thread.sleep(100);
        }

        synchronized(this.fileLockReceiveReference.get(fileName)) {
            int oldNumber = fileSequenceReceiveReference.getOrDefault(fileName, 0);
            this.fileSequenceReceiveReference.put(fileName, Integer.max(seqNumber + 1, oldNumber));
        }

        return message;
    }
}
