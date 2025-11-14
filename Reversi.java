import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Scanner;

public class Reversi {
    // main method
    public static void main(String[] args) {
        // check if user enters the correct amount of command line arguements, exiting the program if they haven't
        if (args.length != 2) {
            System.err.println("Usage: java Client <broadcast address> <broadcast port>");
            System.err.println("ERROR: Found " + args.length + " command-line arguments, program must have exactly 2 command-line arguments");
            System.exit(1);
        }
        // try catch block to catch all general exceptions that occur when running the program
        try {
            // getting the broadcast port based on the command line arguements, throwing a number format exception if 
            int broadcastPort = Integer.parseInt(args[1]);
            // checking for valid user input for port number, where if not throw exception
            if (broadcastPort <= 0) {
                throw new NumberFormatException();
            }
            // getting the IP address entered by the user as a command-line argument
            InetAddress broadcastAddress = InetAddress.getByName(args[0]);
            // initialising the variables for colour type for user and socket with its input and output streams
            char currentColour = 'E';
            char opponentsColour = 'E';
            Socket playerSocket = null;
            BufferedReader in = null;
            PrintWriter out = null;
            boolean player1 = true;
            // creating UPD socket bounded to broadcast port and address from command-line arguments
            DatagramSocket socket = new DatagramSocket(broadcastPort, broadcastAddress);
            // setting socket timeout to 5 seconds
            socket.setSoTimeout(5000);
            // creating packet for socket to receive
            byte[] buf = new byte[14];
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            // infinite loop so it continously runs, until a connection is found
            while (true) {
                // try catch block for if UDP socket times out
                try {
                    System.out.println("listening for UDP messages at on port " + socket.getLocalPort());
                    // listening for UDP messages at broadcast address on broadcast port
                    socket.receive(packet);
                    System.out.println("Connection established, initialising game...");
                    // converting to String, because it is easier to use than byte array
                    String message = new String(buf, StandardCharsets.UTF_8);
                    // checking if message is of correct format
                    if (message.startsWith("NEW GAME")) {
                        // try catch block to check if valid port number, which must be between 9000 and 9100, has been received
                        try {
                            // extracting the port number from the message
                            int messageGameplayPort = Integer.parseInt(message.substring(9, message.length()-1));
                            if (messageGameplayPort < 9000 || messageGameplayPort > 9100) {
                                throw new NumberFormatException("Integer not between 9000 and 9100");
                            }
                        } catch (NumberFormatException e) {
                            System.err.println("ERROR: Received invalid gameplay port in message, gameplay port must be an integer between 9000 and 9100");
                            continue;
                        }
                        // timeout to allow sending of message to start server socket
                        Thread.sleep(500);
                        // extracting the port number from the message
                        int receivedTCPGameplayPort = Integer.parseInt(message.substring(9, message.length()-1));
                        // creating client socket bound to broadcast address at received port number
                        playerSocket = new Socket(broadcastAddress, receivedTCPGameplayPort);
                        // creating input and output streams
                        in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
                        out = new PrintWriter(playerSocket.getOutputStream(), true);
                        // setting client as player2
                        player1 = false;
                        // setting colours for user and opponent, with L for lights and D for darks
                        currentColour = 'L';
                        opponentsColour = 'D';
                        // break loop becuase connection is established
                        break;
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("UDP socket timed out");
                    // generate random port number between 9000 and 9100
                    Random rand = new Random();
                    int tcpGameplayPort = rand.nextInt(101) + 9000;
                    // creating message and converting to byte array for sending
                    String newGameMessage = "NEW GAME:" + tcpGameplayPort + "\n";
                    byte[] request = newGameMessage.getBytes();
                    // limited broadcast address to send message
                    InetAddress broadcast = InetAddress.getByName("255.255.255.255");
                    // creating packet and sending it
                    DatagramPacket outPacket = new DatagramPacket(request, 14, broadcast, broadcastPort);
                    socket.send(outPacket);
                    System.out.println("sent UDP message: " + newGameMessage + "to " + outPacket.getAddress() + " at port " + outPacket.getPort());
                    // try catch block for TCP socket timeout
                    ServerSocket gameplaySocket = new ServerSocket(tcpGameplayPort);
                    try {
                        // creating server socket and setting timeout to 5 seconds
                        gameplaySocket.setSoTimeout(5000);
                        System.out.println("listening for TCP connections on port " + gameplaySocket.getLocalPort());
                        // listening on gameplay port
                        playerSocket = gameplaySocket.accept();
                        System.out.println("Connection established, initialising game...");
                        // creating input and output streams for TCP socket
                        in = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
                        out = new PrintWriter(playerSocket.getOutputStream(), true);
                        // setting colours for user and opponent, with L for lights and D for darks
                        currentColour = 'D';
                        opponentsColour = 'L';
                        // breaking loop becuase connection is established
                        break;
                    } catch (SocketTimeoutException timeoutError) {
                        System.out.println("TCP socket timed out");
                        gameplaySocket.close();
                        // closing socket to reset and creating new UDP socket, so client doesn't try to connect to itself
                        socket.close();
                        socket = new DatagramSocket(broadcastPort, broadcastAddress);
                        socket.setSoTimeout(5000);
                        continue;
                    }
                } catch (InterruptedException threadError) {
                    System.out.println("ERROR: Connection lost");
                }
            }
            // creating empty Reversi board
            char[][] board = new char[8][8];
            for (int i = 0; i < 8; i++) {
                for (int a = 0; a < 8; a++) {
                    board[a][i] = ' ';
                }
            }
            // setting Othello opening
            board[3][3] = 'L';
            board[3][4] = 'D';
            board[4][3] = 'D';
            board[4][4] = 'L';
            // outputting Reversi board
            String output = "   1 2 3 4 5 6 7 8\n  -----------------\n";
            for (int b = 0; b < 8; b++) {
                output += Integer.toString(b+1) + " |";
                for (int c = 0; c < 8; c++) {
                    output += board[b][c] + "|";
                }
                output += "\n";
                output += "  -----------------\n";
            }
            if (currentColour == 'D') {
                System.out.println("\nGame Initialised, you are darks(D)");
            }
            else if (currentColour == 'L') {
                System.out.println("\nGame Initialised, you are lights(L)");
            }
            System.out.println("\n" + output);
            // creating array list to store all possible moves
            ArrayList<String> possibleMoves = new ArrayList<String>();
            // if client is player1 they go first as they are darks
            if (player1) {
                for (int d = 0; d < 8; d++) {
                    for (int e = 0; e < 8; e++) {
                        // checking for a tiles with the opponents colour, as new piece must neighbour, one of the opponent's tiles
                        if (board[e][d] == opponentsColour) {
                            // checking if the player has room to place tile above opponent's tile
                            if (e != 0) {
                                if (board[e-1][d] == ' ') {
                                    // checking if player placed tile, whether it would bound another of their tiles, hence being a valid move
                                    for (int f = e+1; f < 8; f++) {
                                        if (board[f][d] == opponentsColour) {
                                            continue;
                                        }
                                        else if (board[f][d] == currentColour) {
                                            // checking if this move has already been determined
                                            if (!possibleMoves.contains(Integer.toString(e) + "," + Integer.toString(d+1))) {
                                                // adding move to possible moves array list
                                                possibleMoves.add(Integer.toString(e) + "," + Integer.toString(d+1));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile below opponent's tile
                            if (e != 7) {
                                if (board[e+1][d] == ' ') {
                                    for (int g = e-1; g >= 0; g--) {
                                        if (board[g][d] == opponentsColour) {
                                            continue;
                                        }
                                        else if (board[g][d] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e+2) + "," + Integer.toString(d+1))) {
                                                possibleMoves.add(Integer.toString(e+2) + "," + Integer.toString(d+1));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile to the left of opponent's tile
                            if (d != 0) {
                                if (board[e][d-1] == ' ') {
                                    for (int h = d+1; h < 8; h++) {
                                        if (board[e][h] == opponentsColour) {
                                            continue;
                                        }
                                        else if (board[e][h] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e+1) + "," + Integer.toString(d))) {
                                                possibleMoves.add(Integer.toString(e+1) + "," + Integer.toString(d));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                } 
                            }
                            // checking if the player has room to place tile to the right of opponent's tile
                            if (d != 7) {
                                if (board[e][d+1] == ' ') {
                                    for (int j = d-1; j >= 0; j--) {
                                        if (board[e][j] == opponentsColour) {
                                            continue;
                                        }
                                        else if (board[e][j] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e+1) + "," + Integer.toString(d+2))) {
                                                possibleMoves.add(Integer.toString(e+1) + "," + Integer.toString(d+2));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile to the top left diagonal of opponent's tile
                            if (e != 0 && d != 0) {
                                if (board[e-1][d-1] == ' ') {
                                    int l = d+1;
                                    for (int k = e+1; k < 8 && l < 8; k++) {
                                        if (board[k][l] == opponentsColour) {
                                            l++;
                                            continue;
                                        }
                                        else if (board[k][l] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e) + "," + Integer.toString(d))) {
                                                possibleMoves.add(Integer.toString(e) + "," + Integer.toString(d));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile to the top right diagonal of opponent's tile
                            if (e != 0 && d != 7) {
                                if (board[e-1][d+1] == ' ') {
                                    int m = d-1;
                                    for (int n = e+1; n < 8 && m >= 0; n++) {
                                        if (board[n][m] == opponentsColour) {
                                            m--;
                                            continue;
                                        }
                                        else if (board[n][m] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e) + "," + Integer.toString(d+2))) {
                                                possibleMoves.add(Integer.toString(e) + "," + Integer.toString(d+2));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile to the bottom left diagonal of opponent's tile
                            if (e != 7 && d != 0) {
                                if (board[e+1][d-1] == ' ') {
                                    int o = d+1;
                                    for (int p = e-1; p >= 0 && o < 8; p--) {
                                        if (board[p][o] == opponentsColour) {
                                            o++;
                                            continue;
                                        }
                                        else if (board[p][o] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e+2) + "," + Integer.toString(d))) {
                                                possibleMoves.add(Integer.toString(e+2) + "," + Integer.toString(d));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                            // checking if the player has room to place tile to the bottom right diagonal of opponent's tile
                            if (e != 7 && d != 7) {
                                if (board[e+1][d+1] == ' ') {
                                    int q = d-1;
                                    for (int r = e-1; r >= 0 && q >= 0; r--) {
                                        if (board[r][q] == opponentsColour) {
                                            q--;
                                            continue;
                                        }
                                        else if (board[r][q] == currentColour) {
                                            if (!possibleMoves.contains(Integer.toString(e+2) + "," + Integer.toString(d+2))) {
                                                possibleMoves.add(Integer.toString(e+2) + "," + Integer.toString(d+2));
                                            }
                                            break;
                                        }
                                        else {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                System.out.print("Possible Moves: ");
                // printing out possible moves with tab in between each move to separate them
                for (int s = 0; s < possibleMoves.size(); s++) {
                    System.out.print(possibleMoves.get(s) + "\t");
                }
                // checking if there are any possible moves
                if (possibleMoves.size() > 0) {
                    System.out.println("\nPlease enter your move (format: <row_number>,<column_number>): ");
                    Scanner console = new Scanner(System.in);
                    String userInput = "";
                    // try catch block to receive user's input, where if they exit the program using Ctrl+C, error message is sent and program is terminated
                    try {
                        userInput = console.nextLine().trim();
                    } catch (NoSuchElementException terminationError) {
                        out.println("ERROR");
                        System.out.println("Terminated Game");
                        System.exit(0);
                    }
                    // loop until the user input is a valid move
                    while (!possibleMoves.contains(userInput)) {
                        System.err.println("ERROR: Incorrect format for move selection, please try again");
                        System.out.println("\nPlease enter your move (format: <row_number>,<column_number>): ");
                        userInput = console.nextLine().trim();
                    }
                    // get the row and column of the user's move to use to edit board based on new move
                    int rowMove = Character.getNumericValue(userInput.charAt(0));
                    int columnMove = Character.getNumericValue(userInput.charAt(2));
                    // adding tile based on user's move
                    board[rowMove-1][columnMove-1] = currentColour;
                    // checking if there are tiles above the player's move
                    if (rowMove != 1) {
                        // checking if the tile above the newly place tile is one of the opponent's
                        if (board[rowMove-2][columnMove-1] == opponentsColour) {
                            // flip all opponent tiles above the new tile that is now bounded by the new tile and another of the player's tiles
                            for (int v = rowMove-3; v >= 0; v--) {
                                if (board[v][columnMove-1] == opponentsColour) {
                                    continue;
                                }
                                else if (board[v][columnMove-1] == currentColour) {
                                    for (int w = rowMove-2; w > v; w--) {
                                        board[w][columnMove-1] = currentColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles below the player's move
                    if (rowMove != 8) {
                        // checking if the tile below the newly place tile is one of the opponent's
                        if (board[rowMove][columnMove-1] == opponentsColour) {
                            // flip all opponent tiles below the new tile that is now bounded by the new tile and another of the player's tiles
                            for (int x = rowMove+1; x < 8; x++) {
                                if (board[x][columnMove-1] == opponentsColour) {
                                    continue;
                                }
                                else if (board[x][columnMove-1] == currentColour) {
                                    for (int y = rowMove; y < x; y++) {
                                        board[y][columnMove-1] = currentColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the left of the player's move
                    if (columnMove != 1) {
                        // checking if the tile to the left of the newly place tile is one of the opponent's
                        if (board[rowMove-1][columnMove-2] == opponentsColour) {
                            // flip all opponent tiles to the left of the new tile that is now bounded by the new tile and another of the player's tiles
                            for (int z = columnMove-3; z >= 0; z--) {
                                if (board[rowMove-1][z] == opponentsColour) {
                                    continue;
                                }
                                else if (board[rowMove-1][z] == currentColour) {
                                    for (int aa = columnMove-2; aa > z; aa--) {
                                        board[rowMove-1][aa] = currentColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the right of the player's move
                    if (columnMove != 8) {
                        // checking if the tile to the right of the newly place tile is one of the opponent's
                        if (board[rowMove-1][columnMove] == opponentsColour) {
                            // flip all opponent tiles to the right of the new tile that is now bounded by the new tile and another of the player's tiles
                            for (int ab = columnMove+1; ab < 8; ab++) {
                                if (board[rowMove-1][ab] == opponentsColour) {
                                    continue;
                                }
                                else if (board[rowMove-1][ab] == currentColour) {
                                    for (int ac = columnMove; ac < ab; ac++) {
                                        board[rowMove-1][ac] = currentColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the top left diagonal of the player's move
                    if (rowMove != 1 && columnMove != 1) {
                        // checking if the tile to the top left diagonal of the newly place tile is one of the opponent's
                        if (board[rowMove-2][columnMove-2] == opponentsColour) {
                            // flip all opponent tiles to the top left diagonal of the new tile that is now bounded by the new tile and another of the player's tiles
                            int ad = columnMove-3;
                            for (int v = rowMove-3; v >= 0 && ad >= 0; v--) {
                                if (board[v][ad] == opponentsColour) {
                                    ad--;
                                    continue;
                                }
                                else if (board[v][ad] == currentColour) {
                                    int ae = columnMove-2;
                                    for (int w = rowMove-2; w > v; w--) {
                                        board[w][ae] = currentColour;
                                        ae--;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the top right diagonal of the player's move
                    if (rowMove != 1 && columnMove != 8) {
                        // checking if the tile to the top right diagonal of the newly place tile is one of the opponent's
                        if (board[rowMove-2][columnMove] == opponentsColour) {
                            // flip all opponent tiles to the top right diagonal of the new tile that is now bounded by the new tile and another of the player's tiles
                            int af = columnMove+1;
                            for (int v = rowMove-3; v >= 0 && af < 8; v--) {
                                if (board[v][af] == opponentsColour) {
                                    af++;
                                    continue;
                                }
                                else if (board[v][af] == currentColour) {
                                    int ag = columnMove;
                                    for (int w = rowMove-2; w > v; w--) {
                                        board[w][ag] = currentColour;
                                        ag++;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the bottom left diagonal of the player's move
                    if (rowMove != 8 && columnMove != 1) {
                        // checking if the tile to the bottom left diagonal of the newly place tile is one of the opponent's
                        if (board[rowMove][columnMove-2] == opponentsColour) {
                            // flip all opponent tiles to the bottom left diagonal of the new tile that is now bounded by the new tile and another of the player's tiles
                            int ai = columnMove-3;
                            for (int ah = rowMove+1; ah < 8 && ai >= 0; ah++) {
                                if (board[ah][ai] == opponentsColour) {
                                    ai--;
                                    continue;
                                }
                                else if (board[ah][ai] == currentColour) {
                                    int aj = columnMove-2;
                                    for (int ak = rowMove; ak < ah; ak++) {
                                        board[ak][aj] = currentColour;
                                        aj--;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    // checking if there are tiles to the bottom right diagonal of the player's move
                    if (rowMove != 8 && columnMove != 8) {
                        // checking if the tile to the bottom right diagonal of the newly place tile is one of the opponent's
                        if (board[rowMove][columnMove] == opponentsColour) {
                            // flip all opponent tiles to the bottom right diagonal of the new tile that is now bounded by the new tile and another of the player's tiles
                            int al = columnMove+1;
                            for (int am = rowMove+1; am < 8 && al < 8; am++) {
                                if (board[am][al] == opponentsColour) {
                                    al++;
                                    continue;
                                }
                                else if (board[am][al] == currentColour) {
                                    int an = columnMove;
                                    for (int ao = rowMove; ao < am; ao++) {
                                        board[ao][an] = currentColour;
                                        an++;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    System.out.println("\nMoved to " + userInput);
                    // sending move message to opponent
                    out.println("MOVE:" + userInput);
                    // clearing all possible moves becuase player has moved
                    possibleMoves.clear();
                    // printing board status after move
                    output = "   1 2 3 4 5 6 7 8\n  -----------------\n";
                    for (int t = 0; t < 8; t++) {
                        output += Integer.toString(t+1) + " |";
                        for (int u = 0; u < 8; u++) {
                            output += board[t][u] + "|";
                        }
                        output += "\n";
                        output += "  -----------------\n";
                    }
                    System.out.println("\n"+output);
                }
                else {
                    // sending pass message if there are no possible moves
                    out.println("PASS");
                }
            }
            // infinite loop for Reversi game, that will break once an error occurs (e.g. a player leaves, invalid move received by other player), or the game finishes
            while (true) {
                System.out.println("\nWaiting on opponent's move...\n");
                String response = "";
                // try catch block for receiving opponent's response and throws exception if connection is lost
                try {
                    response = in.readLine().trim();
                } catch (SocketException connectionError) {
                    System.out.println("ERROR: Error occurred");
                    System.out.println("Terminated Game");
                    System.exit(1);
                }
                // checking if game is over and printing result
                if (response.equals("YOU WIN") || response.equals("YOU LOSE") || response.equals("DRAW")) {
                    System.out.println(response);
                    break;
                }
                // checking if error occurs and aborting if it has
                if (response.equals("ERROR")) {
                    System.out.println("ERROR: Error occurred");
                    System.out.println("Terminated Game");
                    System.exit(1);
                }
                // checking for opponent's move
                if (response.startsWith("MOVE:")) {
                    // get the row and column of the opponent's move to use to edit board based on new move
                    int rowResponse = Character.getNumericValue(response.charAt(5));
                    int columnResponse = Character.getNumericValue(response.charAt(7));
                    // checking if moves requested by opponent is valid and if not return error to opponent and abort program
                    if (rowResponse < 1 || columnResponse < 1 || rowResponse > 8 || columnResponse > 8) {
                        System.out.println("Invalid move made by opponent");
                        out.println("ERROR");
                        System.out.println("Terminated Game");
                        System.exit(1);
                    }
                    if (board[rowResponse-1][columnResponse-1] != ' ') {
                        System.out.println("Invalid move made by opponent");
                        out.println("ERROR");
                        System.out.println("Terminated Game");
                        System.exit(1);
                    }
                    // adding tile based on opponent's move
                    board[rowResponse-1][columnResponse-1] = opponentsColour;
                    // updating board based on new move using same process as for player's new move except now conducting it for opponent
                    if (rowResponse != 1) {
                        if (board[rowResponse-2][columnResponse-1] == currentColour) {
                            for (int v = rowResponse-3; v >= 0; v--) {
                                if (board[v][columnResponse-1] == currentColour) {
                                    continue;
                                }
                                else if (board[v][columnResponse-1] == opponentsColour) {
                                    for (int w = rowResponse-2; w > v; w--) {
                                        board[w][columnResponse-1] = opponentsColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (rowResponse != 8) {
                        if (board[rowResponse][columnResponse-1] == currentColour) {
                            for (int x = rowResponse+1; x < 8; x++) {
                                if (board[x][columnResponse-1] == currentColour) {
                                    continue;
                                }
                                else if (board[x][columnResponse-1] == opponentsColour) {
                                    for (int y = rowResponse; y < x; y++) {
                                        board[y][columnResponse-1] = opponentsColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (columnResponse != 1) {
                        if (board[rowResponse-1][columnResponse-2] == currentColour) {
                            for (int z = columnResponse-3; z >= 0; z--) {
                                if (board[rowResponse-1][z] == currentColour) {
                                    continue;
                                }
                                else if (board[rowResponse-1][z] == opponentsColour) {
                                    for (int aa = columnResponse-2; aa > z; aa--) {
                                        board[rowResponse-1][aa] = opponentsColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (columnResponse != 8) {
                        if (board[rowResponse-1][columnResponse] == currentColour) {
                            for (int ab = columnResponse+1; ab < 8; ab++) {
                                if (board[rowResponse-1][ab] == currentColour) {
                                    continue;
                                }
                                else if (board[rowResponse-1][ab] == opponentsColour) {
                                    for (int ac = columnResponse; ac < ab; ac++) {
                                        board[rowResponse-1][ac] = opponentsColour;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (rowResponse != 1 && columnResponse != 1) {
                        if (board[rowResponse-2][columnResponse-2] == currentColour) {
                            int ad = columnResponse-3;
                            for (int v = rowResponse-3; v >= 0 && ad >= 0; v--) {
                                if (board[v][ad] == currentColour) {
                                    ad--;
                                    continue;
                                }
                                else if (board[v][ad] == opponentsColour) {
                                    int ae = columnResponse-2;
                                    for (int w = rowResponse-2; w > v; w--) {
                                        board[w][ae] = opponentsColour;
                                        ae--;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (rowResponse != 1 && columnResponse != 8) {
                        if (board[rowResponse-2][columnResponse] == currentColour) {
                            int af = columnResponse+1;
                            for (int v = rowResponse-3; v >= 0 && af < 8; v--) {
                                if (board[v][af] == currentColour) {
                                    af++;
                                    continue;
                                }
                                else if (board[v][af] == opponentsColour) {
                                    int ag = columnResponse;
                                    for (int w = rowResponse-2; w > v; w--) {
                                        board[w][ag] = opponentsColour;
                                        ag++;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (rowResponse != 8 && columnResponse != 1) {
                        if (board[rowResponse][columnResponse-2] == currentColour) {
                            int ai = columnResponse-3;
                            for (int ah = rowResponse+1; ah < 8 && ai >= 0; ah++) {
                                if (board[ah][ai] == currentColour) {
                                    ai--;
                                    continue;
                                }
                                else if (board[ah][ai] == opponentsColour) {
                                    int aj = columnResponse-2;
                                    for (int ak = rowResponse; ak < ah; ak++) {
                                        board[ak][aj] = opponentsColour;
                                        aj--;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                    if (rowResponse != 8 && columnResponse != 8) {
                        if (board[rowResponse][columnResponse] == currentColour) {
                            int al = columnResponse+1;
                            for (int am = rowResponse+1; am < 8 && al < 8; am++) {
                                if (board[am][al] == currentColour) {
                                    al++;
                                    continue;
                                }
                                else if (board[am][al] == opponentsColour) {
                                    int an = columnResponse;
                                    for (int ao = rowResponse; ao < am; ao++) {
                                        board[ao][an] = opponentsColour;
                                        an++;
                                    }
                                    break;
                                }
                                else {
                                    break;
                                }
                            }
                        }
                    }
                }
                // checking if the opponent has mad a move or passed, meaning it is now the player's move
                if (response.startsWith("MOVE:") || response.equals("PASS")) {
                    // printing current status of board
                    output = "   1 2 3 4 5 6 7 8\n  -----------------\n";
                    for (int ap = 0; ap < 8; ap++) {
                        output += Integer.toString(ap+1) + " |";
                        for (int aq = 0; aq < 8; aq++) {
                            output += board[ap][aq] + "|";
                        }
                        output += "\n";
                        output += "  -----------------\n";
                    }
                    System.out.println(output);
                    // getting all possible moves for player using same process that was used earlier
                    for (int ar = 0; ar < 8; ar++) {
                        for (int as = 0; as < 8; as++) {
                            if (board[as][ar] == opponentsColour) {
                                if (as != 0) {
                                    if (board[as-1][ar] == ' ') {
                                        for (int at = as+1; at < 8; at++) {
                                            if (board[at][ar] == opponentsColour) {
                                                continue;
                                            }
                                            else if (board[at][ar] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as) + "," + Integer.toString(ar+1))) {
                                                    possibleMoves.add(Integer.toString(as) + "," + Integer.toString(ar+1));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (as != 7) {
                                    if (board[as+1][ar] == ' ') {
                                        for (int au = as-1; au >= 0; au--) {
                                            if (board[au][ar] == opponentsColour) {
                                                continue;
                                            }
                                            else if (board[au][ar] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as+2) + "," + Integer.toString(ar+1))) {
                                                    possibleMoves.add(Integer.toString(as+2) + "," + Integer.toString(ar+1));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (ar != 0) {
                                    if (board[as][ar-1] == ' ') {
                                        for (int av = ar+1; av < 8; av++) {
                                            if (board[as][av] == opponentsColour) {
                                                continue;
                                            }
                                            else if (board[as][av] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as+1) + "," + Integer.toString(ar))) {
                                                    possibleMoves.add(Integer.toString(as+1) + "," + Integer.toString(ar));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    } 
                                }
                                if (ar != 7) {
                                    if (board[as][ar+1] == ' ') {
                                        for (int aw = ar-1; aw >= 0; aw--) {
                                            if (board[as][aw] == opponentsColour) {
                                                continue;
                                            }
                                            else if (board[as][aw] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as+1) + "," + Integer.toString(ar+2))) {
                                                    possibleMoves.add(Integer.toString(as+1) + "," + Integer.toString(ar+2));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (as != 0 && ar != 0) {
                                    if (board[as-1][ar-1] == ' ') {
                                        int ax = ar+1;
                                        for (int ay = as+1; ay < 8 && ax < 8; ay++) {
                                            if (board[ay][ax] == opponentsColour) {
                                                ax++;
                                                continue;
                                            }
                                            else if (board[ay][ax] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as) + "," + Integer.toString(ar))) {
                                                    possibleMoves.add(Integer.toString(as) + "," + Integer.toString(ar));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (as != 0 && ar != 7) {
                                    if (board[as-1][ar+1] == ' ') {
                                        int az = ar-1;
                                        for (int ba = as+1; ba < 8 && az >= 0; ba++) {
                                            if (board[ba][az] == opponentsColour) {
                                                az--;
                                                continue;
                                            }
                                            else if (board[ba][az] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as) + "," + Integer.toString(ar+2))) {
                                                    possibleMoves.add(Integer.toString(as) + "," + Integer.toString(ar+2));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (as != 7 && ar != 0) {
                                    if (board[as+1][ar-1] == ' ') {
                                        int bb = ar+1;
                                        for (int bc = as-1; bc >= 0 && bb < 8; bc--) {
                                            if (board[bc][bb] == opponentsColour) {
                                                bb++;
                                                continue;
                                            }
                                            else if (board[bc][bb] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as+2) + "," + Integer.toString(ar))) {
                                                    possibleMoves.add(Integer.toString(as+2) + "," + Integer.toString(ar));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                                if (as != 7 && ar != 7) {
                                    if (board[as+1][ar+1] == ' ') {
                                        int bd = ar-1;
                                        for (int be = as-1; be >= 0 && bd >= 0; be--) {
                                            if (board[be][bd] == opponentsColour) {
                                                bd--;
                                                continue;
                                            }
                                            else if (board[be][bd] == currentColour) {
                                                if (!possibleMoves.contains(Integer.toString(as+2) + "," + Integer.toString(ar+2))) {
                                                    possibleMoves.add(Integer.toString(as+2) + "," + Integer.toString(ar+2));
                                                }
                                                break;
                                            }
                                            else {
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // checking if there are any possible moves
                    if (possibleMoves.size() > 0) {
                        // printing out possible moves with tab in between each move to separate them
                        System.out.print("Possible Moves: ");
                        for (int bf = 0; bf < possibleMoves.size(); bf++) {
                            System.out.print(possibleMoves.get(bf) + "\t");
                        }
                        System.out.println("\nPlease enter your move (format: <row_number>,<column_number>): ");
                        Scanner console = new Scanner(System.in);
                        String userInput = "";
                        // try catch block to receive user's input, where if they exit the program using Ctrl+C, error message is sent and program is terminated
                        try {
                            userInput = console.nextLine().trim();
                        } catch (NoSuchElementException terminationError) {
                            out.println("ERROR");
                            System.out.println("Terminated Game");
                            System.exit(1);
                        }
                        // loop until the user input is a valid move
                        while (!possibleMoves.contains(userInput)) {
                            System.err.println("ERROR: Incorrect format for move selection, please try again");
                            System.out.println("\nPlease enter your move (format: <row_number>,<column_number>): ");
                            userInput = console.nextLine().trim();
                        }
                        // get the row and column of the user's move to use to edit board based on new move
                        int rowMove = Character.getNumericValue(userInput.charAt(0));
                        int columnMove = Character.getNumericValue(userInput.charAt(2));
                        // adding tile based on user's move
                        board[rowMove-1][columnMove-1] = currentColour;
                        // editing board based on player's move, flipping any tiles using the same process as above when the player made a move
                        if (rowMove != 1) {
                            if (board[rowMove-2][columnMove-1] == opponentsColour) {
                                for (int v = rowMove-3; v >= 0; v--) {
                                    if (board[v][columnMove-1] == opponentsColour) {
                                        continue;
                                    }
                                    else if (board[v][columnMove-1] == currentColour) {
                                        for (int w = rowMove-2; w > v; w--) {
                                            board[w][columnMove-1] = currentColour;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (rowMove != 8) {
                            if (board[rowMove][columnMove-1] == opponentsColour) {
                                for (int x = rowMove+1; x < 8; x++) {
                                    if (board[x][columnMove-1] == opponentsColour) {
                                        continue;
                                    }
                                    else if (board[x][columnMove-1] == currentColour) {
                                        for (int y = rowMove; y < x; y++) {
                                            board[y][columnMove-1] = currentColour;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (columnMove != 1) {
                            if (board[rowMove-1][columnMove-2] == opponentsColour) {
                                for (int z = columnMove-3; z >= 0; z--) {
                                    if (board[rowMove-1][z] == opponentsColour) {
                                        continue;
                                    }
                                    else if (board[rowMove-1][z] == currentColour) {
                                        for (int aa = columnMove-2; aa > z; aa--) {
                                            board[rowMove-1][aa] = currentColour;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (columnMove != 8) {
                            if (board[rowMove-1][columnMove] == opponentsColour) {
                                for (int ab = columnMove+1; ab < 8; ab++) {
                                    if (board[rowMove-1][ab] == opponentsColour) {
                                        continue;
                                    }
                                    else if (board[rowMove-1][ab] == currentColour) {
                                        for (int ac = columnMove; ac < ab; ac++) {
                                            board[rowMove-1][ac] = currentColour;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (rowMove != 1 && columnMove != 1) {
                            if (board[rowMove-2][columnMove-2] == opponentsColour) {
                                int ad = columnMove-3;
                                for (int v = rowMove-3; v >= 0 && ad >= 0; v--) {
                                    if (board[v][ad] == opponentsColour) {
                                        ad--;
                                        continue;
                                    }
                                    else if (board[v][ad] == currentColour) {
                                        int ae = columnMove-2;
                                        for (int w = rowMove-2; w > v; w--) {
                                            board[w][ae] = currentColour;
                                            ae--;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (rowMove != 1 && columnMove != 8) {
                            if (board[rowMove-2][columnMove] == opponentsColour) {
                                int af = columnMove+1;
                                for (int v = rowMove-3; v >= 0 && af < 8; v--) {
                                    if (board[v][af] == opponentsColour) {
                                        af++;
                                        continue;
                                    }
                                    else if (board[v][af] == currentColour) {
                                        int ag = columnMove;
                                        for (int w = rowMove-2; w > v; w--) {
                                            board[w][ag] = currentColour;
                                            ag++;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (rowMove != 8 && columnMove != 1) {
                            if (board[rowMove][columnMove-2] == opponentsColour) {
                                int ai = columnMove-3;
                                for (int ah = rowMove+1; ah < 8 && ai >= 0; ah++) {
                                    if (board[ah][ai] == opponentsColour) {
                                        ai--;
                                        continue;
                                    }
                                    else if (board[ah][ai] == currentColour) {
                                        int aj = columnMove-2;
                                        for (int ak = rowMove; ak < ah; ak++) {
                                            board[ak][aj] = currentColour;
                                            aj--;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        if (rowMove != 8 && columnMove != 8) {
                            if (board[rowMove][columnMove] == opponentsColour) {
                                int al = columnMove+1;
                                for (int am = rowMove+1; am < 8 && al < 8; am++) {
                                    if (board[am][al] == opponentsColour) {
                                        al++;
                                        continue;
                                    }
                                    else if (board[am][al] == currentColour) {
                                        int an = columnMove;
                                        for (int ao = rowMove; ao < am; ao++) {
                                            board[ao][an] = currentColour;
                                            an++;
                                        }
                                        break;
                                    }
                                    else {
                                        break;
                                    }
                                }
                            }
                        }
                        System.out.println("\nMoved to " + userInput);
                        // sending move message to opponent
                        out.println("MOVE:" + userInput);
                        // clearing all possible moves becuase player has moved
                        possibleMoves.clear();
                        // printing board status after move
                        output = "   1 2 3 4 5 6 7 8\n  -----------------\n";
                        for (int bg = 0; bg < 8; bg++) {
                            output += Integer.toString(bg+1) + " |";
                            for (int bh = 0; bh < 8; bh++) {
                                output += board[bg][bh] + "|";
                            }
                            output += "\n";
                            output += "  -----------------\n";
                        }
                        System.out.println("\n"+output);
                    }
                    else {
                        // checking if opponent passed becuase we have passed and if they have also passed, game is over
                        if (response.equals("PASS")) {
                            // calcultaing how many tiles for each player there are
                            int currentTiles = 0;
                            int opponentTiles = 0;
                            for (int bi = 0; bi < 8; bi++) {
                                for (int bj = 0; bj < 8; bj++) {
                                    if (board[bi][bj] == currentColour) {
                                        currentTiles++;
                                    }
                                    else if (board[bi][bj] == opponentsColour) {
                                        opponentTiles++;
                                    }
                                }
                            }
                            // print result responses based on tile count and also send result to opponent, as well as break loop becuase game is over
                            if (currentTiles > opponentTiles) {
                                out.println("YOU LOSE");
                                System.out.println("YOU WIN");
                                break;
                            }
                            else if (currentTiles < opponentTiles) {
                                out.println("YOU WIN");
                                System.out.println("YOU LOSE");
                                break;
                            }
                            else {
                                out.println("DRAW");
                                System.out.println("DRAW");
                                break;
                            }
                        }
                        else {
                            // player has passed and send pass message to opponent
                            System.out.println("No valid moves, you pass");
                            out.println("PASS");
                        }
                    }
                }
            }
        } catch (NumberFormatException e) {
            // exception handling if user enters invalid broadcast port number as command-line argument
            System.err.println("Usage: java Client <broadcast address> <broadcast port>");
            System.err.println("ERROR: Didn't find positive integer for broadcast port command-line argument");
            System.exit(1);
        } catch (UnknownHostException e) {
            // exception handling if user enters invalid broadcast address as command-line argument
            System.err.println("Usage: java Client <broadcast address> <broadcast port>");
            System.err.println("ERROR: No IP address for the host could be found based on broadcast address command-line argument");
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (SocketException e) {
            // exception handling if TCP or UDP sockets cannot bind or connect
            System.err.println("Usage: java Client <broadcast address> <broadcast port>");
            System.err.println("ERROR: Socket unable to bind to " + args[0] + " at port " + args[1]);
            System.err.println(e.getMessage());
        } catch (IOException e) {
            // exception handling if TCP or UDP sockets has error in sending or receiving packets
            System.err.println("ERROR: Unable to create socket or unable to receive or send packet");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}