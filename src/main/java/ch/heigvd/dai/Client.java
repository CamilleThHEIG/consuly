package ch.heigvd.dai;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import picocli.CommandLine;

@CommandLine.Command(name = "client", description = "Launch the client side of the application.")
public class Client implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-h", "--host"},
            description = "Host to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "localhost")
    protected String host;

    @CommandLine.Option(
            names = {"-p", "--port"},
            description = "Port to connect to (default: ${DEFAULT-VALUE}).",
            defaultValue = "4446")
    protected int port;

    protected String message = "Hello, server! I'm the client. 🤖";

    @Override
    public Integer call() {
        System.out.println("Connecting to " + host + ":" + port + "...");
        try (DatagramSocket socket = new DatagramSocket()) {
            // Get the server address
            InetAddress serverAddress = InetAddress.getByName(host);

            // Transform the message into a byte array - always specify the encoding
            byte[] buffer = message.getBytes(StandardCharsets.UTF_8);

            // Create a packet with the message, the server address and the port
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, serverAddress, port);

            // Send the packet
            socket.send(packet);

            System.out.println("[Client] Request sent: " + message);

            // Create a buffer for the incoming response
            byte[] responseBuffer = new byte[1024];

            // Create a packet for the incoming response
            DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);

            // Receive the packet - this is a blocking call
            socket.receive(responsePacket);

            // Transform the message into a string
            String response
                    = new String(
                            responsePacket.getData(),
                            responsePacket.getOffset(),
                            responsePacket.getLength(),
                            StandardCharsets.UTF_8);

            System.out.println("[Client] Received response: " + response);
        } catch (Exception e) {
            System.err.println("[Client] An error occurred: " + e.getMessage());
        }
        return 0;
    }
}
