# Reversi-Network-Program
Peer-To-Peer Java program that performs matchmaking with UDP messaging that allows players to connect to each other over the local network. Upon connection, players are able to play Reversi against each other, where the moves are communciated between players through TCP communication. The program provides players with their possible moves when it is their turn and presents a game board in the terminal showing the current state of the board for the game.

To compile in terminal:
javac Reversi.java

To run program in terminal:
java Reversi <broadcast_address> <broadcast_port>

In above, <broadcast_address> is the address that the UDP datagram socket
listens on and the <broadcast_port> is the port that the UDP datagram
socket listens on.

For running, you can use two local copies to test program, however both
broadcast addresses must be a valid IP address and must be different,
while the broadcast port must be a valid port number that is the same for
both.

For assumptions made, this Reversi game has a standard 8x8 board and uses
the standard Othello opening where the light tiles are in the middle 2x2
square along the left diagonal, with the dark tiles being in the middle
2x2 square along the right diagonal. The client that sends the UDP New
Game message, is player 1 and is assigned the dark tiles, so they go
first, with the client that receives the UDP New Game message being player
2 and is assigned the white tiles.
