import java.util.*;

public class Occurence
{
    //Variable Declaration
    long logicalTimestamp;
    List<Integer> sequenceNumbers;

    //Constructors
    Occurence(long tstamp, int seqNumber) {
        this.logicalTimestamp = tstamp;
        this.sequenceNumbers = new ArrayList<Integer>();
        this.sequenceNumbers.add(seqNumber);
    }

    Occurence(long tstamp, List<Integer> seqNumber) {
       this.logicalTimestamp = tstamp;
       this.sequenceNumbers = seqNumber;
    }
}