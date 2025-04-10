/**
 * @Author: Garvyn-Yuan
 * FIle Name: DictionaryServer
 * @Description: his is dictionary Server for assignment 1
 * @Date: File created in 21:48-2025/3/24
 * @Modified by:
 * Version: V1.1
 */
package distributed.app;
import distributed.models.LocalWords;

// reader and writer
import java.io.BufferedReader; // BufferedReader for reading std input
import java.io.InputStreamReader; // converting byte streams to character streams
import java.io.DataInputStream;
import java.io.DataOutputStream;

// sockets
import java.net.*;
import javax.net.ServerSocketFactory; // server socket object(especially for SSL/TSL) --> can create different socket conveniently

// Exceptions
import java.io.IOException;

//web
import org.json.JSONObject;
import java.net.http.HttpClient;

// Threads -> ThreadPoolExecutor
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
// cache && automatic tasks
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Interface
import java.util.List;

// lock
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class DictionaryServer {

    // Port for SERVER
    // private static final int SERVER_PORT = 9022;
    // ThreadPool Size
    private static final int MAX_THREADS = 10;
    // client threads counter
    private static int cThreadCounter = 0;
    // Thread Pool
    private static final ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
    // web client
    private static final HttpClient webClient = HttpClient.newHttpClient();

    // cache and sync
    private static final AtomicReference<List<String[]>> dicCache = new AtomicReference<>(LocalWords.readAllWords());
    // any client last req
    private static final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());
    // scheduler to remove cache
    private static final ScheduledExecutorService cacheScheduler = Executors.newSingleThreadScheduledExecutor();

    // delete , modify, add lock
    private static final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();


    // Server entrance
    public static void main(String[] args) throws UnknownHostException {
        // port and socket
        String ip = args[0];
        int SERVER_PORT = Integer.parseInt(args[1]);


        InetAddress bindAddr = InetAddress.getByName(ip);


        // socket factory ~
        ServerSocketFactory dicFactory = ServerSocketFactory.getDefault();

        // can speed up look-up speed -- client can share this cache -- but can not be shared between threads
        // List<String[]> dicCache = LocalWords.readAllWords();

        try (ServerSocket dicSocket = dicFactory.createServerSocket(SERVER_PORT, 50, bindAddr)) {
            System.out.println("Server starting.......");
            System.out.println("IP Address : " + ip);
            System.out.println("Port number : " + SERVER_PORT);
            System.out.println("-- On service --");

            // Start cache cleanup scheduler
            cacheScheduler.scheduleAtFixedRate(() -> {
                long now = System.currentTimeMillis();
                long lastAccess = lastAccessTime.get();
                long thirtyMinutes = 30 * 60 * 1000; // ms

                if (now - lastAccess > thirtyMinutes) {
                    dicCache.set(null);
                    System.out.println("Cache Cleared -- No requests for 30 minutes.");
                }
            }, 1, 5, TimeUnit.MINUTES); // check each 5 min


            // keep listening
            while (true) {
                Socket clientSocket = dicSocket.accept(); // blocked method, if success, Return a new socket for communication;
                cThreadCounter++;
                System.out.println("The client number is " + cThreadCounter);
                threadPool.execute(() -> {
                    try {
                        serverClient(clientSocket);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });// submit task
            }

        } catch (IOException e) {
            System.out.println("Exception caught when trying to listen on port " + SERVER_PORT + " or listening for a connection");
            System.out.println(e.getMessage());
        }finally {
            threadPool.shutdown(); // close threadPool anyway
        }

    }

    // Local word lookup
    private static List<String> lookUpLocal(String word) {
        // lock
        lock.readLock().lock();

        if (DictionaryServer.dicCache.get() == null || DictionaryServer.dicCache.get().isEmpty()) {
            DictionaryServer.dicCache.set(LocalWords.readAllWords());
        }
        LocalWords ld = new LocalWords();
        lock.readLock().unlock();
        return ld.findWord(word, DictionaryServer.dicCache.get());
    }

    // This function is to lookup word online in the dictionary
    private static String lookUpOnLine(String cWord) {
        try {
            // String language = detectLanguage(cWord);
            // Wikipedia API to look up words
            System.out.println("Looking up online");
            String apiUrl = "https://en.wikipedia.org/api/rest_v1/page/summary/" + cWord;
            // todo: Detect language

            // GET
            URI uri = new URI(apiUrl);
            URL url = uri.toURL(); // java 23 refuse new URL
            JSONObject json = getJsonObject(url);

            // extract meanings
            if (json.has("extract")) {
                return json.getString("extract");
            } else {
                return "Unknown word -- " + cWord + ", we're working on it !!";
            }

        } catch (Exception e) {
            return e.getMessage();
        }
    }

    // threads to serve client
    private static void serverClient(Socket clientSocket) throws IOException {
        System.out.println("New client connected, start to serve");
        try (Socket clientSoc = clientSocket;
             DataInputStream in = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {

            while (true) {
                // todo: deal with delete / update / add
                System.out.println("Start to accept data");
                String operation = in.readUTF();
                String word = in.readUTF();  // Receive word
                String meaning = in.readUTF();
                System.out.println("operation -- " + operation);
                System.out.println("word -- " + word);
                System.out.println("meaning -- " + meaning);

                // update access time
                lastAccessTime.set(System.currentTimeMillis());
                // language match and deal with req
                boolean isLangSupported = languageSupported(word);
                boolean containSpecialChar = specialCharacters(word);
                System.out.println(isLangSupported);
                System.out.println(containSpecialChar);
                if (isLangSupported && !containSpecialChar) {
                    if (operation.equals("local")) {
                        List<String> resList = DictionaryServer.lookUpLocal(word);
                        out.writeUTF(String.valueOf(resList)); // Send result back to client
                        out.flush(); // Ensure all data is sent
                    } else {
                        String result = switch (operation) {
                            case "remote" -> DictionaryServer.lookUpOnLine(word);
                            case "add" -> addLocalWord(word, meaning);
                            case "delete" -> deleteLocalWord(word, meaning);
                            case "update" -> updateLocalWord(word, meaning);
                            default -> "Not Supported Action ~";
                        };
                        out.writeUTF(result); // Send result back to client
                        out.flush(); // Ensure all data is sent
                    }
                }else{
                    out.writeUTF("Language unsupported or More than one word");
                    out.flush();
                }
            }
        } catch (IOException e) {
            System.out.println("Exception while handling client: " + e.getMessage());
        }
    }


    // API interact
    private static JSONObject getJsonObject(URL url) throws IOException {
        // down-casting and set params
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        // buffer reader to read input-stream (byte stream -> buffer)
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        // string builder to combine res
        StringBuilder response = new StringBuilder();
        String line;
        // build results
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        // close to free up resource
        reader.close();

        // JSON to string
        return new JSONObject(response.toString());
    }



    private static String addLocalWord(String word, String meaning) {
        lock.writeLock().lock();
        try {
            if (meaning.isEmpty()){
                return "Empty meaning is not allowed";
            }

            LocalWords lw = new LocalWords();
            if (isNewWord(word)){
                lw.addWord(word, meaning);  // Add the new word
                dicCache.set(LocalWords.readAllWords());  // Update the cache
                return "Word added successfully.";
            }else{
                lw.addWord(word, meaning);  // Add the new word
                dicCache.set(LocalWords.readAllWords());  // Update the cache
                return "Word already exists, new meaning added successfully.";
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static String updateLocalWord(String word, String meaning) {
        lock.writeLock().lock();
        try {
            if (!isNewWord(word)) {
                if (meaning.isEmpty()){
                    return "Empty meaning is not allowed";
                }
                int order = 1;
                try {
                    char fir = meaning.charAt(0);
                    if (Character.isDigit(fir)){
                        order = Character.getNumericValue(fir);
                    }
                    else{
                        throw new NumberFormatException();
                    }
                }catch (NumberFormatException e){
                    return "Please input the correct order of meaning at the beginning of meaning";
                }
                LocalWords lw = new LocalWords();
                String res = lw.updateWord(word, meaning.substring(1), order, dicCache.get());  // Update word's meaning
                dicCache.set(LocalWords.readAllWords());  // Update the cache
                return res;
            }else{
                return "Word does not exist";
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private static String deleteLocalWord(String word, String order) {
        /*
        * Delete meanings by order
        * */
        lock.writeLock().lock();
        try {
            int reqOrder = 1;
            if (!order.isEmpty()){
                reqOrder = Integer.parseInt(order);
            }
            if (!isNewWord(word)) {
                LocalWords lw = new LocalWords();
                String res = lw.deleteWord(word, reqOrder, dicCache.get());  // Delete the word
                dicCache.set(LocalWords.readAllWords());  // Update the cache
                return res;
            }else{
                return "Word does not exist";
            }
        }catch(NumberFormatException e){
            return "Invalid order number, please type a correct order number in the meaning box"
;        }
        finally {
            lock.writeLock().unlock();
        }
    }

    // check if this is a new word
    // todo: optimize the data structure
    private static boolean isNewWord(String word) {
        lock.readLock().lock();
        try {
            for (String[] entry : dicCache.get()) {
                if (entry[0].equalsIgnoreCase(word)) {
                    return false; // Word already exists
                }
            }
            return true; // New word
        } finally {
            lock.readLock().unlock();
        }
    }

    // check language supported status
    public static boolean languageSupported(String word) {
        return word.matches("^[a-zA-Z0-9" +
                "\\u4e00-\\u9fa5" +  // 汉字
                "\\u0400-\\u04FF" +  // 西里尔字母（俄语等）
                "\\u0600-\\u06FF" +  // 阿拉伯字母
                "\\u0E00-\\u0E7F" +  // 泰语
                "\\u0900-\\u097F" +  // 印地语
                "\\u0980-\\u09FF" +  // 孟加拉语
                "\\u3040-\\u30FF" +  // 日文（平假名、片假名）
                "\\uAC00-\\uD7AF" +  // 韩文
                "\\u0D00-\\u0D7F" +  // 泰米尔语
                "\\u1E00-\\u1EFF" +  // 拉丁字母扩展
                "]+$");  // 只允许合法字符
    }


    // check invalid input
    public static boolean specialCharacters(String word){
        return word.matches(".*[\\p{P}\\s]+.*");
    }

}


