import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ConcurrentDeleteTest {
    public static void main(String[] args) throws InterruptedException {
        int numClients = 50; // 50个并发删除请求
        ExecutorService executor = Executors.newFixedThreadPool(numClients);

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try (Socket socket = new Socket("127.0.0.1", 9022);
                     DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                     DataInputStream in = new DataInputStream(socket.getInputStream())) {

                    // 模拟删除请求。假设每个客户端删除不同单词的第一个含义，
                    // 你可以根据实际情况修改单词名称和删除参数
                    String operation = "delete";
                    String word = "writeTestWord" + clientId;
                    // 删除请求中的第二个参数，这里表示删除含义的顺序，默认删除第1个含义
                    String order = "1";

                    out.writeUTF(operation);
                    out.writeUTF(word);
                    out.writeUTF(order);
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
