package controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import org.json.JSONObject;
import service.JsonReader;
import service.JsonWriter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ChooseGameDirectoryController implements Initializable {
    @FXML
    private Button browseDirectoryButton;
    @FXML
    private TextField gameDirectoryTextField;
    @FXML
    private AnchorPane chooseDirAnchorPane;
    @FXML
    private Button submitDirectory;
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        browseDirectoryButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                final DirectoryChooser gameDirChooser = new DirectoryChooser();
                Stage stg = (Stage) chooseDirAnchorPane.getScene().getWindow();
                File rorFilePath = gameDirChooser.showDialog(stg);
                if(rorFilePath != null) {
                    JSONObject jsonConfigObj = null;

                    try {
                        jsonConfigObj = JsonReader.readJsonFromFile("Config/Config.json");
                        jsonConfigObj.put("directory", rorFilePath.getAbsolutePath());
                        JsonWriter jsonWriter = new JsonWriter();
                        jsonWriter.writeJsonToFile("Config/Config.json", jsonConfigObj);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    submitDirectory.setDisable(false);
                    gameDirectoryTextField.setText(rorFilePath.getAbsolutePath());
                    submitDirectory.setOnMouseClicked(new EventHandler<MouseEvent>() {
                        @Override
                        public void handle(MouseEvent mouseEvent) {
                            try {
                                FXMLLoader fxmlLoader = new FXMLLoader();
                                fxmlLoader.setLocation(getClass().getResource("/view/LoadingMods.fxml"));
                                Parent root = fxmlLoader.load();
                                Stage stage = (Stage)((Node)mouseEvent.getSource()).getScene().getWindow();
                                Scene scene = new Scene(root);
                                stage.setScene(scene);
                                stage.show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });
    }
}
