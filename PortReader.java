import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.TreeMap;
import redis.clients.jedis.Jedis;

public class PortReader {
    public static void main(String[] args) {
        String pathToCsv = "service-names-port-numbers.csv"; // replace with your CSV file path
        String targetHost = "localhost"; // replace with your target host

        // Use TreeMap to automatically sort entries by the key (port number)
        Map<Integer, String> portMap = new TreeMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(pathToCsv))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Use comma as separator
                String[] columns = line.split(",");

                // Check if there are enough columns and if the port number is not null
                if (columns.length >= 4 && !columns[1].trim().isEmpty()) {
                    // Assuming columns[1] contains port numbers and columns[3] contains port descriptions
                    String portNumberString = columns[1].trim();
                    String portDescription = columns[3].trim();

                    try {
                        // Parse the port number as an integer
                        int portNumber = Integer.parseInt(portNumberString);

                        // Put the data into the map
                        portMap.put(portNumber, portDescription);
                    } catch (NumberFormatException e) {
                        // Handle the case where the port number is not a valid integer
                        System.err.println("Invalid port number: " + portNumberString);
                    }
                } else {
                    System.err.println("Invalid line or null port number: " + line);
                }
            }

            // Perform port scanning for non-null ports
            System.out.println("Scanning ports on " + targetHost + "...");
            try (Jedis jedis = new Jedis("localhost", 6379)) {
                for (int port : portMap.keySet()) {
                    try (Socket socket = new Socket(targetHost, port)) {
                        System.out.println("Port " + port + " is open - " + portMap.get(port));

                        // Write to Redis only if the port is open
                        jedis.hset("open_ports", String.valueOf(port), portMap.get(port));
                        jedis.hset("open_ports_description", String.valueOf(port), portMap.get(port));
                    } catch (IOException e) {
                        // Port is likely closed or unreachable
                    }
                }
            }

            System.out.println("Port scanning finished.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}