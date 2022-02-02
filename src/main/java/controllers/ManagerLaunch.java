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
import java.util.List;

public class ManagerLaunch extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        JsonReader jsonReader = new JsonReader();
        JSONObject configObject = jsonReader.readJsonFromFile("Config/Config.json");
        String gameDir = configObject.getString("directory");

        Database db = new Database();
        Connection conn = db.connect();
        db.createTable(conn);
        conn.close();

        Parameters params = getParameters();
        List<String> launchParams = params.getRaw();
        String protocolParameter = null;
        if(launchParams.size() != 0){
            protocolParameter = launchParams.get(0);
        }

        FXMLLoader loader = new FXMLLoader();
        Parent root = null;

        if(!(gameDir.equals(""))) {
            loader.setLocation(getClass().getResource("/view/LoadingMods.fxml"));
            root = loader.load();
            LoadingModsController controller = loader.getController();
            controller.setLaunchParameter(protocolParameter);
            primaryStage.setTitle("Risk Of Rain 2 Mod Manager");
        }
        else{
            loader.setLocation(getClass().getResource("/view/ChooseGameDirectory.fxml"));
            root = loader.load();
            ChooseGameDirectoryController controller = loader.getController();
            controller.setLaunchParameter(protocolParameter);
            primaryStage.setTitle("Choose Directory");
        }
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
}
