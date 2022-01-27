package controllers;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;


import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LoadingModsController implements Initializable {
    @FXML
    private AnchorPane loadingAnchorPane;
    @FXML
    private ProgressBar loadingProgress;
    @FXML
    private Label manyModsLabel;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/view/PackageListing.fxml"));

        Parent root = null;
        try {
            root = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        PackageListingController packageListingController = fxmlLoader.getController();
        Task onlineTask = packageListingController.getOnlineTask();
        loadingProgress.progressProperty().bind(onlineTask.progressProperty());

        int installedSize = packageListingController.getInstalledSize();
        if(installedSize > 30){
            manyModsLabel.setText(String.format("You have %d mods installed.\nThis could take a while.", installedSize));
        }

        Parent finalRoot = root;

        onlineTask.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if((double)t1 == 1.0){
                    Stage stage = (Stage)loadingAnchorPane.getScene().getWindow();

                    double width = 1200;
                    double height = 700;

                    Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
                    stage.setX((screenBounds.getWidth() - width) / 2);
                    stage.setY((screenBounds.getHeight() - height) / 2);
                    stage.setTitle("Risk Of Rain 2 Mod Manager");
                    Scene scene = new Scene(finalRoot, width, height);

                    stage.setScene(scene);
                    stage.show();
                }
            }
        });
    }
}
