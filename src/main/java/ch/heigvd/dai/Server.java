package ch.heigvd.dai;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Launch the server side of the application.")
public class Server implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    @Override
    public Integer call() {
        System.err.println("Connecting to port " + port + "...");
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("[Server] Success! Listening for unicast messages on port " + port + "...");
            while (!socket.isClosed()) {
                // Create a buffer for the incoming request
                byte[] requestBuffer = new byte[1024];

                // Create a packet for the incoming request
                DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length);

                // Receive the packet - this is a blocking call
                socket.receive(requestPacket);

                // Transform the request into a string
                String request
                        = new String(
                                requestPacket.getData(),
                                requestPacket.getOffset(),
                                requestPacket.getLength(),
                                StandardCharsets.UTF_8);

                System.out.println("[Server] Request received (at " + new Date() + "): " + request);

                // Prepare the response
                String response = "Hello, client! I'm the server. 👻";

                // Transform the message into a byte array - always specify the encoding
                byte[] responseBuffer = response.getBytes(StandardCharsets.UTF_8);

                // Create a packet with the message, the client address and the client port
                DatagramPacket responsePacket
                        = new DatagramPacket(
                                responseBuffer,
                                responseBuffer.length,
                                requestPacket.getAddress(),
                                requestPacket.getPort());

                // Send the packet
                socket.send(responsePacket);

                System.out.println("[Server] Response sent (at " + new Date() + "): " + response);
            }
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }
}
