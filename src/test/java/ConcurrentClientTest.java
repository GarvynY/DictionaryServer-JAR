/**
        * @Author: Garvyn-Yuan
 * @FileName: ConcurrentClientTest.java
 * @Description: Stress test simulating 50 concurrent query clients.
 * @Date: Created on [Date]
        * @ModifiedBy: [Modifier Name]
        * @Version: V1.0
        * @TestCoverage:
        * - Local query concurrency validation
 * - Connection pool stress testing
 * - Server response consistency check
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class ConcurrentClientTest {
    public static void main(String[] args) throws InterruptedException {
        int numClients = 50; // 50 concurrent clients
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9022);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    out.writeUTF("local");
                    out.writeUTF("testWord" + clientId);
                    out.writeUTF("");
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
