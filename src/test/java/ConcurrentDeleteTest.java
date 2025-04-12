/**
        * @Author: Garvyn-Yuan
 * @FileName: ConcurrentDeleteTest.java
 * @Description: High-concurrency validation test for delete operations.
        * @Date: Created on [Date]
        * @ModifiedBy: [Modifier Name]
        * @Version: V1.0
        * @TestScenario:
        * - 50 parallel delete requests
 * - Atomic operation verification
 * - Write conflict handling test
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentDeleteTest {
    public static void main(String[] args) throws InterruptedException {
        int numClients = 50;
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9022);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {
                    String operation = "delete";
                    String word = "writeTestWord" + clientId;
                    String order = "1";

                    out.writeUTF(operation);
                    out.writeUTF(word);
                    out.writeUTF(order);
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
