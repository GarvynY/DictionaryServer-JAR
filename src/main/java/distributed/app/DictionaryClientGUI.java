package distributed.app;
/**
 * @Author: Garvyn-Yuan
 * @FileName: DictionaryClientGUI.java
 * @Description: GUI client for dictionary operations, supporting local/remote queries, history tracking, and dictionary management.
 * @Date: Created at 21:48 on 2025/3/24
        * @ModifiedBy: Garvyn
        * @Version: V1.2
        * @Features:
        * - Dual-mode query (Local/Remote)
 * - Interactive history panel with 10-record capacity
 * - Integrated dictionary CRUD operations
 * - Network exception handling
 */
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.LinkedList;
import java.util.Scanner;

public class DictionaryClientGUI extends JFrame {
    private final String SERVER_IP;
    private final int SERVER_PORT;
    private final LinkedList<String> dicHistory = new LinkedList<>(); // history
    private final DefaultListModel<String> historyModel = new DefaultListModel<>(); // GUI history list

    public static void main(String[] args) {
        String serverIp = args[0];
        int serverPort = Integer.parseInt(args[1]);
        System.out.println("IP address : " + serverIp);
        System.out.println("Port : " + serverPort);
        SwingUtilities.invokeLater(() -> new DictionaryClientGUI(serverIp, serverPort));
    }


    public DictionaryClientGUI(String SERVER_IP, int SERVER_PORT) {
        this.SERVER_IP = SERVER_IP;
        this.SERVER_PORT = SERVER_PORT;
        setTitle("Dictionary GUI");
        setSize(1000, 800);  // Increase the height to accommodate new buttons
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        // icon
        setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icon.png")));

        // Top panel with query input and buttons
        JPanel queryPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        JTextField wordField = new JTextField(15);
        JButton queryButton = new JButton("Query");
        queryButton.setFont(new Font("Arial", Font.BOLD, 14));

        // Query type (Local or Remote)
        JComboBox<String> queryTypeComboBox = new JComboBox<>(new String[] {"Local Query", "Remote Query"});
        queryPanel.add(new JLabel("Input word:"));
        queryPanel.add(wordField);
        queryPanel.add(queryButton);
        queryPanel.add(queryTypeComboBox);

        // Results panel
        JTextArea resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane resultScroll = new JScrollPane(resultArea);

        // Instruction panel
        // Instruction area (usage description)
        JTextArea instructionArea = new JTextArea();
        instructionArea.setEditable(false);
        instructionArea.setLineWrap(true);
        instructionArea.setWrapStyleWord(true);
        instructionArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        instructionArea.setText(
                "Instruction：\n" +
                        "1. Online query -- look up word online\n" +
                        "2. Local query -- look up word locally\n" +
                        "3. Delete word -- look up meanings of word and type the meaning order you want to delete\n" +
                        "4. Update meaning -- type the meaning order you want to change at the beginning of it"
        );
        JScrollPane instructionScroll = new JScrollPane(instructionArea);
        instructionScroll.setPreferredSize(new Dimension(300, 200));
        instructionScroll.setBorder(BorderFactory.createTitledBorder("Quick Start"));




        // History panel
        JList<String> historyList = new JList<>(historyModel);
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setPreferredSize(new Dimension(580, 100));
        historyScroll.setBorder(BorderFactory.createTitledBorder("History"));

        // Local dictionary management panel (Add, Update, Delete)
        JPanel localDictPanel = new JPanel();
        localDictPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 10));

        JTextField localWordField = new JTextField(10);
        JTextField localMeaningField = new JTextField(25);
        JButton addButton = new JButton("Add");
        JButton updateButton = new JButton("Update");
        JButton deleteButton = new JButton("Delete");

        localDictPanel.add(new JLabel("Word:"));
        localDictPanel.add(localWordField);
        localDictPanel.add(new JLabel("Meaning(order when delete):"));
        localDictPanel.add(localMeaningField);
        localDictPanel.add(addButton);
        localDictPanel.add(updateButton);
        localDictPanel.add(deleteButton);
        localDictPanel.add(instructionScroll, BorderLayout.EAST);


        // Events for buttons
        queryButton.addActionListener((ActionEvent e) -> {
            String word = wordField.getText().trim();
            String queryType = (String) queryTypeComboBox.getSelectedItem();
            if (!word.isEmpty()) {
                String meaning = queryWord(word, queryType);
                resultArea.setText(meaning);
                addHistory(word + " -> " + meaning);
            }
        });

        addButton.addActionListener((ActionEvent e) -> {
            String word = localWordField.getText().trim();
            String meaning = localMeaningField.getText().trim();
            if (!word.isEmpty() && !meaning.isEmpty()) {
                sendLocalDictRequest("add", word, meaning, resultArea);
            }
        });

        updateButton.addActionListener((ActionEvent e) -> {
            String word = localWordField.getText().trim();
            String meaning = localMeaningField.getText().trim();
            if (!word.isEmpty()) {
                sendLocalDictRequest("update", word, meaning, resultArea);
            }
        });

        deleteButton.addActionListener((ActionEvent e) -> {
            String word = localWordField.getText().trim();
            String meaning = localMeaningField.getText().trim();
            if (!word.isEmpty()) {
                sendLocalDictRequest("delete", word, meaning, resultArea);
            }
        });

        // Add components to the frame
        add(queryPanel, BorderLayout.NORTH);
        add(resultScroll, BorderLayout.CENTER);
        add(localDictPanel, BorderLayout.SOUTH);
        add(historyScroll, BorderLayout.WEST);

        setVisible(true);
    }

    private String queryWord(String word, String queryType) {
        if ("Local Query".equals(queryType)) {
            return queryLocalWord(word);
        } else {
            return queryRemoteWord(word);
        }
    }

    // Send request to the server for local dictionary operations
    private void sendLocalDictRequest(String operation, String word, String meaning, JTextArea resultArea) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Send operation type (add, update, delete) and word/meaning
            out.writeUTF(operation);
            out.writeUTF(word);
            if (meaning != null) {
                out.writeUTF(meaning);
            }
            out.flush();

            // Read the server response
            String response = in.readUTF();
            resultArea.setText(response);  // Show response in the text area
        } catch (IOException e) {
            resultArea.setText("Connection failed: " + e.getMessage());
        }
    }

    // Local word lookup (could be replaced with actual local dictionary)
    private String queryLocalWord(String word) {
        try(Socket socket = new Socket(SERVER_IP, SERVER_PORT);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())){

            String meaning = "";
            out.writeUTF("local");
            out.writeUTF(word);
            out.writeUTF(meaning);
            out.flush();
            System.out.println("Send local query : " + word);
            return in.readUTF();
        }catch (Exception e){
            return "Connection Failed" + e.getMessage();
        }
    }

    // Remote word lookup using server
    private String queryRemoteWord(String word) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {

            String meaning = "";
            out.writeUTF("remote");
            out.writeUTF(word);
            out.writeUTF(meaning);
            out.flush();
            System.out.println("Send remote query : " + word);
            return in.readUTF();  // result
        } catch (IOException e) {
            return "Connection failed: " + e.getMessage();
        }
    }

    private void addHistory(String record) {
        if (dicHistory.size() >= 10) {
            dicHistory.removeFirst();
        }
        dicHistory.add(record);
        historyModel.clear();
        for (String item : dicHistory) {
            historyModel.addElement(item);
        }
    }
}
