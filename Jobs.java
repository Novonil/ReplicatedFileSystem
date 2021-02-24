import java.io.*;
import java.util.*;


public class Jobs
{
    //Variable Declaration
    public long logicalTimestamp;
    String  ownerServerId,
            fileName,
            message;

    //Constructor
    public Jobs(String ownerserverid, String fname, String message) {
        this.ownerServerId = ownerserverid;
        this.fileName = fname;
        this.message = message;
    }

    //Critical Section - Write Messages to Files
    public void execute() throws IOException
    {
        PrintWriter fileWriter = new PrintWriter(new FileWriter("FileSystem/" + this.fileName, true));
        fileWriter.println(this.message);
        fileWriter.close();
    }

}

//Create a custom Comparator class
class JobsComparator implements Comparator<Jobs>
{
    // Jobs are compared on the basis of logical timestamp (ascending), ties broken in favour of lower Server ID
    public int compare(Jobs j1, Jobs j2)
    { 
        if (j1.logicalTimestamp > j2.logicalTimestamp)
        {
            return 1;
        }
        else if (j1.logicalTimestamp < j2.logicalTimestamp)
        {
            return -1; 
        }
        else
        {
            if (j1.ownerServerId.compareTo(j2.ownerServerId) > 0)
            {
                return 1;
            }
            else
            {
                return -1;
            }
        }
    }
} 