import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;
import java.time.Instant;
import java.util.concurrent.*;
import javax.naming.NameNotFoundException;


public class Server extends Process
{
    //Variable Declaration
    public static long lgclTime;
    //List of all other Servers
    List<Process> otherServerList = new ArrayList<Process>();
    
    //A concurrent hashmap of PriorityBlockingQueue for each file on each server to keep sorted sequence of the jobs based on timestamp and server id
    Map<String, PriorityBlockingQueue<Jobs>> jobQueue = new ConcurrentHashMap<String, PriorityBlockingQueue<Jobs>>(Process.listOfFiles.length);

    //Constructor to set the server details
    public Server(String ServerID, String ipAddress, int portNumber) 
    {
        super(ServerID, ipAddress, portNumber);

        for (String fileName : Process.listOfFiles) {
            this.jobQueue.put(fileName, new PriorityBlockingQueue<Jobs>(30, new JobsComparator()));
        }
    }

    //Entry Point (Main Method)
    public static void main(String[] args) throws IOException
    {
        //Check if the number of parameters for the servers are correct
        if (args.length != 3) {
            throw new InvalidParameterException("Incorrect parameters for Server. Enter <Server Name> <IP Address> <Port Number>");
        }

        Server serverDetails = new Server(args[0], args[1], Integer.parseInt(args[2]));

        Instant instantNow = Instant.now();

        // Log Start Time of Server 
        System.out.println(String.format("SERVER %s starts at TIME: %s", serverDetails.id, instantNow.toEpochMilli()));

        // Get list of available file servers from config.txt file
        serverDetails.readFromConfig("config.txt");

        //Sert up server 
        ServerSocket srvrSock = new ServerSocket(serverDetails.portNumber); 

        //Set Thread Pool Size
        int MAX_THREAD_POOL_SIZE = 7;

        //Executor Service
        final ExecutorService exser = Executors.newFixedThreadPool(MAX_THREAD_POOL_SIZE);

        //Future Variable
        Future<Integer> ftr = null;

        while (true) {

            Socket clntSock = srvrSock.accept();
            Route clntroute = new Route(clntSock);

            //Log New Connection Requests
            System.out.println(String.format("New Connection request from IP = %s, Port = %s",
                clntSock.getInetAddress(),
                clntSock.getPort()
            ));

            System.out.println("Connection has been set up");

            eventHandler callEvent = new eventHandler(
                clntroute,
                serverDetails
            );

            //Thread Spawning to handle requests
            ftr = exser.submit(callEvent);
        }
    }


    //Create a New Job Occurence for itself
    public Occurence createNewOccurence(Process serverProcess, Jobs job) 
    {
        int sequenceNumber;
        long Timestamp;

        //Allow only one thread to enter this section at a time and create a unique sequence number and timestamp
        synchronized(serverProcess.fileLockSendReference.get(job.fileName)) 
        {
            //Fetch time stamp and sequence number of the job
            Timestamp = Server.fetchLogicalTimestamp();
            sequenceNumber = serverProcess.fileSequenceSendReference.getOrDefault(job.fileName, 0);
            serverProcess.fileSequenceSendReference.put(job.fileName, sequenceNumber + 1);
        }

        return new Occurence(Timestamp, sequenceNumber);
    }

    //Create a new Job Occurence for all other Servers
    public Occurence createNewBroadcastOccurence(List<Process> serverProcesses, Jobs job)
    {
        int sequenceNumber;
        long Timestamp = Server.fetchLogicalTimestamp();
        List<Integer> sequenceNumbers = new ArrayList<Integer>();

        for (Process serverProcess : serverProcesses)
        {
            //Allow only one thread to enter this section at a time and create a unique sequence number and timestamp for each server in the network
            synchronized(serverProcess.fileLockSendReference.get(job.fileName))
            {
                //Fetch time stamp and sequence number of the job for each server in the network
                Timestamp = Server.fetchLogicalTimestamp();
                sequenceNumber = serverProcess.fileSequenceSendReference.getOrDefault(job.fileName, 0);
                sequenceNumbers.add(sequenceNumber);
                serverProcess.fileSequenceSendReference.put(job.fileName, sequenceNumber + 1);    
            }
        }
        return new Occurence(Timestamp, sequenceNumbers); 
    }

    //Fetch Logical Time Stamp
    public static synchronized long fetchLogicalTimestamp()
    {
        return Long.max(Instant.now().toEpochMilli(),Server.lgclTime);
    }

    //Update the Logical Time Stamp
    public static synchronized void updateLogicalTimestamp(long timestmp)
    {
        Server.lgclTime = Long.max(timestmp + 1, Server.lgclTime);
    }

    //Get Server details
    public Process getProcessDetailsFromId(String processid) {
        Process p = null;

        for (Process server : this.otherServerList) {
            if (server.id.equals(processid)) {
                p = server;
            }
        }
        return p;
    }

    //Broadcast message from the owner server to all other server in the network
    public void broadcastMessage(List<Route> routes, List<Integer> sequenceNumbers, String message) throws IOException 
    {
        for (int serverCounter = 0; serverCounter < this.otherServerList.size(); serverCounter++) {
            this.otherServerList.get(serverCounter)
                .sendMess(
                    routes.get(serverCounter),
                    sequenceNumbers.get(serverCounter),
                    message
                );
        }
    }

    //Receive Response of the broadcast message
    public void broadcastMessageReply(List<Route> routes, String fileName) throws IOException, Exception
    {
        String reply;

        for (int serverCounter = 0; serverCounter < this.otherServerList.size(); serverCounter++) {
            reply = this.otherServerList.get(serverCounter).receiveMess(routes.get(serverCounter), fileName);

            if (!reply.equals("ACK")) {
                throw new Exception(String.format("Error from Server = %s in response to broadcast",
                    this.otherServerList.get(serverCounter).id));
            }
        }
    }

    public void readFromConfig(String fileName) throws FileNotFoundException, IOException 
    {
        BufferedReader inputFileBuffer = new BufferedReader(new FileReader(fileName));
        String line;
        String[] params;

        System.out.println("Loading servers from config file");

        while ((line = inputFileBuffer.readLine()) != null) {
            params = line.split(" ");

            if (!params[0].equals(this.id)) 
            { 
                // Skip adding itself to the server list
                System.out.println(String.format("Found server %s, ip= %s, port= %s", params[0], params[1], params[2]));
                this.otherServerList.add(new Process(params[0], params[1], Integer.parseInt(params[2])));
            }
        }
        inputFileBuffer.close();
    }
}

class eventHandler implements Callable<Integer>
{
    private Route requestingProcessRoute;
    Server ownerProcess;
    String  requestingProcessId,
            requestingProcessType;

    public eventHandler(Route rt, Server ownr) {
        this.requestingProcessRoute = rt;
        this.ownerProcess = ownr;
    }

    private String[] readRequest(String request) {
        return request.split(":");
    }

    public Integer call() throws IOException, FileNotFoundException {
        String requestMessage = this.requestingProcessRoute.receiveMessage();
        String[] req = requestMessage.split(":");

        this.requestingProcessType = req[0];
        this.requestingProcessId = req[1];
        String fileName = req[2];

        // Update external event time based on request timestamp
        Server.updateLogicalTimestamp(Long.parseLong(req[3]));

        System.out.println(requestMessage);

        //Identify if the request is from a client based on the marker
        if (this.requestingProcessType.equals("client")) {
            
            //Log Requesting Process Type
            System.out.println("Client Request");

            // Check whether file exists
            if (!Arrays.asList(Process.listOfFiles).contains(fileName)) {
                System.out.println(String.format("Failure! File %s does not exist", fileName));
                this.requestingProcessRoute.sendMessage("ERR: File not found");
                return 0;
            }

            //Call Client Handler for Client Request
            try {
                
                this.clientRequestHandler();
                //Log Success
                System.out.println(String.format("Success! Server %s sends an acknowledgement message to client %s", this.ownerProcess.id, this.requestingProcessId));
                //Send ACK Message to Client
                this.requestingProcessRoute.sendMessage("ACK");
            }
            catch (Exception e) {

                //Log Error in Client Request Handling
                System.out.println(String.format("Failure! Client Request Handling caused error : %s", e));

                //Send ERR Message to Client
                this.requestingProcessRoute.sendMessage("ERR");

                return 0;
            }
            
        }
        //Identify if the request is from a server based on the marker
        else if (this.requestingProcessType.equals("server")) {
            //Log Requesting Process Type
            System.out.println("Server Request");

            //Call Server Handler for Server Request
            try {
                this.serverRequestHandler(fileName);
            }

            catch (Exception e) {

                //Log Error in Server Request Handling
                System.out.println(String.format("Failure! Server %s Request Handling caused error : %s", this.requestingProcessId, e));

                return 0;
            }
        }

        return 1;
    }

    private void clientRequestHandler() throws IOException, UnknownHostException, InterruptedException, Exception {
        
        String jobMsg;
        Occurence brdcstOcrnc;
        List<Route> serverRoutes = new ArrayList<Route>();

        String clntReq = this.requestingProcessRoute.receiveMessage();

        //Break request to extract required info
        String[] reqMsg = this.readRequest(clntReq);

        //Log Client Request Received at Server
        System.out.println(
            String.format("Request received at Server %s with Message : %s to be appended to file %s at time: %s",
                this.ownerProcess.id, reqMsg[3], reqMsg[4], Server.fetchLogicalTimestamp()
            )
        );

        // Create job that contains message meant for the file
        Jobs job = new Jobs(this.ownerProcess.id, reqMsg[4], reqMsg[3]);

        for (Process srvr : this.ownerProcess.otherServerList) {
            Route rt = new Route(srvr.ipAddress, srvr.portNumber);
            // Send Server Marker to the other servers
            rt.sendMessage(String.format("server:%s:%s:%s", this.ownerProcess.id, job.fileName, Server.fetchLogicalTimestamp()));
            serverRoutes.add(rt);
        }
        
        //Create New Broadcase Event with the timestamp and sequence number
        brdcstOcrnc = this.ownerProcess.createNewBroadcastOccurence(this.ownerProcess.otherServerList, job);

        //Assign job a time stamp
        job.logicalTimestamp = brdcstOcrnc.logicalTimestamp;
        
        //Add the job to the queue in a sorted fashion as per the comparator
        this.ownerProcess.jobQueue.get(job.fileName).add(job);

        //Frame Request message to be sent to the other servers in the network
        jobMsg = String.format("REQ:%s:%s:%s:%s:%s", this.requestingProcessId, job.ownerServerId, job.message, job.fileName, brdcstOcrnc.logicalTimestamp
        );

        //Log Send of Request from Server to other servers
        System.out.println(String.format("Sending Request Message to other servers : %s", jobMsg));

        //Broadcast Request of job from owner server to other servers
        this.ownerProcess.broadcastMessage(serverRoutes, brdcstOcrnc.sequenceNumbers, jobMsg);

        //Wait and receive a reply from all other servers on the network before writing to file
        this.ownerProcess.broadcastMessageReply(serverRoutes, job.fileName);

        //Make thread sleep till the job reaches the front of the queue
        while (!this.ownerProcess.jobQueue.get(job.fileName).peek().equals(job)) {
            Thread.sleep(10);  // Wait for 10 ms
        }

        //Critical Section, Write to File
        job.execute();

        //Create a new sequence number and time stamp for Release message
        brdcstOcrnc = this.ownerProcess.createNewBroadcastOccurence(this.ownerProcess.otherServerList, job);

        //Frame Release message to be sent to other servers in the network
        jobMsg = String.format("REL:%s:%s:%s:%s:%s",
            this.requestingProcessId, job.ownerServerId, job.message, job.fileName, brdcstOcrnc.logicalTimestamp
        );

        //Log send of Release from owner server to other servers
        System.out.println(String.format("Send Release Message to other servers : %s", jobMsg));

        //Broadcast Release Message to all other servers in the network
        this.ownerProcess.broadcastMessage(serverRoutes, brdcstOcrnc.sequenceNumbers, jobMsg);

        //Wait and receive acknowledgement from all other servers to which broadcasted
        this.ownerProcess.broadcastMessageReply(serverRoutes, job.fileName);

        //Remove the task from the queue
        if (!this.ownerProcess.jobQueue.get(job.fileName).poll().equals(job)) {
            throw new Exception(String.format("Removed incorrect job from the front of the queue %s", job));
        }

        //Close Socket
        for (Route rt : serverRoutes) {
            rt.closeSocket();
        }
    }

    private void serverRequestHandler(String fileName) throws Exception {
        
        Occurence ocrnc;

        //Server Details of Owner or Requesting Process
        Process SrvrReq = this.ownerProcess.getProcessDetailsFromId(this.requestingProcessId);

        //Receive the request being broadcast
        String serverRequest = SrvrReq.receiveMess(this.requestingProcessRoute, fileName);

        //Break the request message and get info out of it
        String[] reqMsg = this.readRequest(serverRequest);

        //Check if from an outsider(server)
        if (SrvrReq.equals(null)) {
            throw new NameNotFoundException(String.format("Server not present in the config with id=%s", reqMsg[2]));
        }

        //Create New Job for the request
        Jobs job = new Jobs(SrvrReq.id, fileName, reqMsg[3]);

        //Use time stamp of the owner process to enqueue the job
        job.logicalTimestamp = Long.parseLong(reqMsg[5]); 

        //Enqueue the Job
        this.ownerProcess.jobQueue.get(job.fileName).add(job);

        //Create the occurence for the request with proper timestamp and sequenec number
        ocrnc = this.ownerProcess.createNewOccurence(SrvrReq, job);

        //Send Acknowledgement of the Request Received
        SrvrReq.sendMess(this.requestingProcessRoute, ocrnc.sequenceNumbers.get(0), "ACK");

        //Wait till the server receives the release message for the job from the owner server
        String releaseMessage = SrvrReq.receiveMess(this.requestingProcessRoute, job.fileName);
        
        //Wait till the job reaches the front of the queue
        while (!this.ownerProcess.jobQueue.get(job.fileName).peek().equals(job)) {
            Thread.sleep(10); 
        }

        //Critical Section, Write to  file
        job.execute();

        //Remove task from front of queue
        if (!this.ownerProcess.jobQueue.get(job.fileName).poll().equals(job)) {
            throw new Exception(String.format("Removed Incorrect job from front of the queue %s", job));
        }

        //Create the occurence for the release acknowledgemnet with proper timestamp and sequenec number
        ocrnc = this.ownerProcess.createNewOccurence(SrvrReq, job);

        //Send Acknowledgement for the release
        SrvrReq.sendMess(this.requestingProcessRoute, ocrnc.sequenceNumbers.get(0), "ACK");

        //Close Socket
        this.requestingProcessRoute.closeSocket();
    }
}