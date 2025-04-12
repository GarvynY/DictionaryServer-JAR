import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentWriteTest {
    public static void main(String[] args) throws InterruptedException {
        int numClients = 50; // 50个并发写请求
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9022);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    // 发送 "add" 请求，添加新的单词和含义
                    String operation = "add";
                    String word = "writeTestWord" + clientId;
                    String meaning = "Meaning for writeTestWord" + clientId;

                    out.writeUTF(operation);
                    out.writeUTF(word);
                    out.writeUTF(meaning);
                    out.flush();

                    // 读取服务器返回结果
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
