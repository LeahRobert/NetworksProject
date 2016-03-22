This application simulates a peer-to-peer network on a local machine.
It takes in as arguments the number of peers (as stated in the assignment
details) and an identification number for the Chair (main peer).
The chair passes to all peers a file that contains a question to be
voted upon.
The peers then organize themselves into a ring topology.
The vote is then passed from the Chair around the ring, until it 
reaches the Chair again. The Chair then passes the results around the 
ring.