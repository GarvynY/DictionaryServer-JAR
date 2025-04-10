package distributed.models;

/**
 * @Author: Garvyn-Yuan
 * FIle Name: LocalWords
 * @Description:
 * @Date: File created in 23:14-2025/4/2
 * @Modified by:
 * Version: V1.0
 */
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LocalWords {

    // In idea
    // private static final String CSV_FILE_PATH = "src/main/resources/words.csv";
    // in Jar
    private static final String CSV_FILE_PATH = "words.csv";

    // 静态代码块，类加载时执行，确保 CSV 文件存在
    static {
        File csvFile = new File(CSV_FILE_PATH);
        if (!csvFile.exists()) {
            try (InputStream is = LocalWords.class.getResourceAsStream("/words.csv")) {
                if (is != null) {
                    // 从 jar 包中复制到当前工作目录
                    Files.copy(is, csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied words.csv from jar to working directory.");
                } else {
                    // 如果 jar 包内没有该文件，则创建空文件
                    Files.createFile(csvFile.toPath());
                    System.out.println("Created empty words.csv file in working directory.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // construct func
    public LocalWords() {
        File csvFile = new File(CSV_FILE_PATH);
        if (!csvFile.exists()) {
            // 尝试从 jar 包内读取 words.csv 资源
            try (InputStream is = getClass().getResourceAsStream("/words.csv")) {
                if (is != null) {
                    Files.copy(is, csvFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Copied words.csv from jar to working directory.");
                } else {
                    // 如果 jar 包内也没有该资源，则创建一个空文件
                    Files.createFile(csvFile.toPath());
                    System.out.println("Created empty words.csv file in working directory.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // add function
    public void addWord(String word, String definition) {
        // use normal writer to construct a csv writer
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE_PATH, true))) {
            String[] entry = {word, definition};
            writer.writeNext(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // delete function
    public String deleteWord(String word, int order, List<String[]> allWords) {
        int matchCount = 1;

        Iterator<String[]> iterator = allWords.iterator();
        while (iterator.hasNext()) {
            String[] entry = iterator.next();
            if (entry[0].equalsIgnoreCase(word)) {
                if (matchCount == order) {
                    iterator.remove();
                    writeWordsToFile(allWords);
                    return "Word deleted successfully.";
                }else{
                    matchCount++;
                }
            }
        }
        return "Targeted meaning doesn't found";
    }


    // modify the definition of a word
    public String updateWord(String word, String newDefinition, int order, List<String[]> allWords) {
        int countOrder = 1;

        for (String[] entry : allWords) {
            if (entry[0].equalsIgnoreCase(word)) {
                if (countOrder == order){
                    entry[1] = newDefinition; // Update
                    writeWordsToFile(allWords); // Write Back
                    return "Word updated successfully.";
                }
                else {
                    countOrder++;
                }
            }
        }
        return "Targeted meaning or word doesn't found";
    }

    // Look UP by cache or fresh read
    public List<String> findWord(String word, List<String[]> allWords) {
        if (allWords == null || allWords.isEmpty()) {
            allWords = readAllWords(); // fallback
        }

        List<String> definitions = new ArrayList<>();
        for (String[] entry : allWords) {
            if (entry[0].equalsIgnoreCase(word)) {
                definitions.add(entry[1]); // Collect all matching definitions
            }
        }
        if (!definitions.isEmpty()) {
            return definitions;
        }else{
            definitions.add("No definitions, You can add one !");
            return definitions;
        }
    }

    // Overload
    public List<String> findWord(String word) {
        return findWord(word, readAllWords());
    }


    // write to file
    private void writeWordsToFile(List<String[]> allWords) {
        try (CSVWriter writer = new CSVWriter(new FileWriter(CSV_FILE_PATH))) {
            writer.writeAll(allWords); // write all data
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // read all data
    public static List<String[]> readAllWords() {
        List<String[]> allWords = new ArrayList<>(); // todo: ArrayList is not thread secured --> add lock when server use
        try (CSVReader reader = new CSVReader(new FileReader(CSV_FILE_PATH))) {
            String[] nextLine;
            while ((nextLine = reader.readNext()) != null) {
                allWords.add(nextLine); // read next line
            }
        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
        return allWords;
    }

}
