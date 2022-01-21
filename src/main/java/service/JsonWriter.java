package service;

import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class JsonWriter {

    public void writeJsonToFile(String filePath, JSONObject jsonObject) throws FileNotFoundException {
        File jsonFile = new File(filePath);

        PrintWriter writer = new PrintWriter(jsonFile);
        writer.print(jsonObject.toString(4));
        writer.close();

    }
}
