package controllers;


import database.Database;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.json.JSONObject;
import service.JsonReader;

import java.sql.Connection;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        JsonReader jsonReader = new JsonReader();
        JSONObject configObject = jsonReader.readJsonFromFile("Config/Config.json");
        String gameDir = configObject.getString("directory");

        Database db = new Database();
        Connection conn = db.connect();
        db.createTable(conn);

        conn.close();

        if(!(gameDir.equals(""))) {
            Parent root = FXMLLoader.load(getClass().getResource("/view/LoadingMods.fxml"));
            primaryStage.setTitle("Hello World");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();



        }
        else{
            Parent root = FXMLLoader.load(getClass().getResource("/view/ChooseGameDirectory.fxml"));
            primaryStage.setTitle("Hello World");
            primaryStage.setScene(new Scene(root));
            primaryStage.show();
        }


    }


    public static void main(String[] args) {
        launch(args);
    }
}
