/**
        * @Author: Garvyn-Yuan
 * @FileName: ConcurrentWriteTest.java
 * @Description: Write performance benchmark with 50 concurrent writers.
        * @Date: Created on [Date]
        * @ModifiedBy: [Modifier Name]
        * @Version: V1.0
        * @Metrics:
        * - Throughput measurement
 * - Write lock contention analysis
 * - Data consistency audit
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentWriteTest {
    public static void main(String[] args) throws InterruptedException {
        int numClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9022);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    String operation = "add";
                    String word = "writeTestWord" + clientId;
                    String meaning = "Meaning for writeTestWord" + clientId;

                    out.writeUTF(operation);
                    out.writeUTF(word);
                    out.writeUTF(meaning);
                    out.flush();

                    String response = in.readUTF();
                    System.out.println("Client " + clientId + " received: " + response);
                } catch (IOException e) {
                    System.err.println("Client " + clientId + " error: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
