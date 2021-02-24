# ReplicatedFileSystem
Design a Replicated File System 

Client-Server Model
In this project, you are expected to implement Lamport's Mutual Exclusion Algorithm. Knowledge of threads
and socket programming and its APIs in the language you choose is expected. Each process (server/client)
must execute on a separate machine (dcxx).

Description
Implement a solution that mimics a replicated le system. The le system consists of four text les:
f1; f2; f3; and f4. All four les are replicated at three le servers. To achieve le replication, choose any
three of the dcxx machines. Each of these machines will store a copy of all the les. Five clients, executing
at dierent sites (and dierent from the three sites that replicate the les), may issue append to le requests
for any of these les. The clients know the locations of all the other clients as well as the locations of all
le servers where the les are replicated. All replicas of a le are consistent, to begin with. The desired
operations are as follows:
 A client initiates at most one append to le at a time.
 The client may send a append to le REQUEST to any of the le replication sites (randomly selected)
along with the text to be appended. In this case, the site must report a successful message to the client
if all copies of the le, across the all sites, are updated consistently. Otherwise, the append should not
happen at any site and the site must report a failure message to the client. We do not expect a failure
to happen in this project unless a le is not replicated at exactly four dierent sites.
 On receiving the append to le REQUEST, the receiving site initiates a REQUEST to enter critical
section, as per Lamport's mutual exclusion algorithm. Obviously each REQUEST should result in a
critical section execution regarding that particular le. In the critical section the text provided by the
client must be appended to all copies of the le.
 Concurrent appends initiated by dierent clients to dierent les must be supported as such updates
do not violate the mutual exclusion condition.
 Your program does NOT need to support the creation of new les or deletion of existing les. However,
it must report an error if an attempt is made to append to a le that does not exist in the le system.
 All clients are started simultaneously.
 Each client loops through the following sequence of actions 100 times: (a) waits for a random amount of
time between 0 to 1 second, (b) issues an append to le REQUEST, (c) waits for a successful or failure
message. So, for this project you need to worry about concurrent read/write requests for the same le
but issued by dierent clients. Your client should gracefully terminate when all 100 REQUESTS have
been served (either successfully or unsuccessfully).
The teaching assistant will upload a sample conguration le for the servers. You must follow that
standard. Your code is evaluated in terms of functionality, readability and documentation (comments and
README le). A separate le describing your code is appreciated but is not mandatory.
