package service;

import org.json.JSONArray;
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

    public void writeJsonArrayToFile(String filePath, JSONArray jsonArray) throws FileNotFoundException {
        File jsonFile = new File(filePath);

        PrintWriter writer = new PrintWriter(jsonFile);
        writer.print(jsonArray.toString());
        writer.close();
    }
}
