package controllers;


import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import mods.ModPackage;
import mods.PackageVersion;
import service.ModDownloader;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

public class PackageItemController implements Initializable {
    private String name;
    private String author;
    private ModPackage thisModPackage;
    private List<PackageVersion> recentlyInstalledMods;
    private Task modDownloaderTask;
    private boolean showingInfoAnchor = false;

    @FXML
    private Label modOwnerLabel;

    @FXML
    private Label modNameLabel;

    @FXML
    private ImageView packageImage;

    @FXML
    private HBox modInfoHbox;

    @FXML
    private AnchorPane itemAnchorPane;

    @FXML
    private Button downloadButton;

    @FXML
    private ComboBox versionBox;

    @FXML
    private ProgressBar downloadProgress;

    @FXML
    private Label instLabel = new Label();

    @FXML
    private AnchorPane modInfoAnchor;

    @FXML
    private Label detailsChevron;

    private Button uninstallButton = new Button();
    private Button updateButton = new Button();

    private Map<String, Integer> gottenModPositions;
    private List<ModPackage> modPackages;

    private List<ModPackage> installedModPackages;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        downloadProgress.setMinWidth(90);
        modInfoHbox.getChildren().remove(downloadProgress);

        instLabel.setAlignment(Pos.CENTER);

        modInfoAnchor.setVisible(false);
        showingInfoAnchor = false;
        itemAnchorPane.setMinHeight(57);

        downloadButton.setMinWidth(90);
        downloadButton.setText("Download");

        updateButton.setMinWidth(90);
        updateButton.setStyle("-fx-background-color: #2e8c3b;");


        uninstallButton.setMinWidth(90);
        uninstallButton.setStyle("-fx-background-color: #c43323;");
        uninstallButton.setText("Uninstall");


        downloadButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                downloadButton.getScene().setCursor(Cursor.HAND);
            }
        });

        downloadButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                startDownloadTask(false);
            }
        });

        downloadButton.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                downloadButton.getScene().setCursor(Cursor.DEFAULT);
            }
        });


    }

    public Task getPackageItemTask(){
        return this.modDownloaderTask;
    }

    private void setRecentlyInstalled(List<PackageVersion> packageVersions){
        this.recentlyInstalledMods = packageVersions;
    }

    public List<PackageVersion> getRecentlyInstalledMods(){
        return this.recentlyInstalledMods;
    }

    public void setData(ModPackage modPackage, List<ModPackage> installedModPackages, Map<String, Integer> allModsMap, List<ModPackage> allOnlineMods, boolean installed) throws IOException, SQLException {
        this.gottenModPositions = allModsMap;
        this.modPackages = allOnlineMods;
        this.installedModPackages = installedModPackages;
        this.modDownloaderTask = initializeDownloadTask();

        Label descLabel = new Label();
        descLabel.setMaxWidth(200);
        descLabel.setWrapText(true);
        descLabel.setEllipsisString("...");

        if(modPackage.getName() != null) {
            this.thisModPackage = modPackage;
            modOwnerLabel.setText("by " + modPackage.getOwner());

            if (modPackage.isIs_deprecated()) {
                modNameLabel.setText(modPackage.getName() + " - DEPRECATED");
                modNameLabel.setStyle("-fx-text-fill: red");
            } else {
                modNameLabel.setText(modPackage.getName());
            }
            descLabel.setText(thisModPackage.getVersions().get(0).getDescription());
            modInfoAnchor.getChildren().add(descLabel);


            if(installed){
                setInstalledUI(thisModPackage);
            }
            else{
                setUninstalledUI(thisModPackage);
            }
        }

        getImage();

        itemAnchorPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                startAnimation();
            }
        });
        downloadButton.setFocusTraversable(false);
    }

    public void startAnimation(){
        if(showingInfoAnchor){
            modInfoAnchor.setVisible(false);
            detailsChevron.setRotate(0);
            itemAnchorPane.setMinHeight(itemAnchorPane.getHeight() - modInfoAnchor.getHeight());
            showingInfoAnchor = false;
        }
        else {
            System.out.println("Starting animation: " + modOwnerLabel.getText() + " : " + modNameLabel.getText());

            detailsChevron.setRotate(90);
            modInfoAnchor.setVisible(true);
            itemAnchorPane.setMinHeight(itemAnchorPane.getHeight() + modInfoAnchor.getHeight());
            showingInfoAnchor = true;
        }
    }

    public void setInstalledUI(ModPackage modPackage){
        updateButton.setText(modPackage.getInstalledPackageVersion().getVersion_number());
        showNewButton(uninstallButton);
        populateVersionBox(modPackage, true);
    }

    public void setUninstalledUI(ModPackage modPackage){
        showNewButton(downloadButton);
        populateVersionBox(modPackage, false);
    }

    public void refreshHBox(ModPackage modPackage, boolean installed) throws SQLException, IOException {
        this.setData(modPackage, installedModPackages, gottenModPositions, modPackages, installed);
    }

    public void startDownloadTask(boolean alreadyInstalled){
        showNewButton(downloadProgress);
        downloadProgress.progressProperty().bind(modDownloaderTask.progressProperty());
        new Thread(modDownloaderTask).start();
    }

    private void getImage(){
        Task task = new Task() {
            @Override
            protected Object call() throws Exception {
                Image iconImage = new Image(thisModPackage.getVersions().get(0).getIcon());

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        packageImage.setImage(iconImage);
                    }
                });
                return null;
            }
        };
        new Thread(task).start();
    }

    public void populateVersionBox(ModPackage modPackage, boolean installed){
        versionBox.getItems().clear();
        List<String> versionNums = new ArrayList<>();

        if(installed) {
            String installedVersion = modPackage.getInstalledPackageVersion().getVersion_number();
            if(modPackage.needsUpdate()) {

                String latestVersion = modPackage.getVersions().get(0).getVersion_number();
                versionNums.add(installedVersion);
                versionNums.add(latestVersion);

                versionBox.valueProperty().addListener(new ChangeListener() {
                    @Override
                    public void changed(ObservableValue observableValue, Object o, Object t1) {
                        if (t1.equals(latestVersion)) {
                            showNewButton(updateButton);
                        } else if (t1.equals(installedVersion)) {
                            showNewButton(uninstallButton);
                        }
                    }
                });
            }
            else{
                versionNums.add(installedVersion);
                versionBox.setDisable(true);
            }
        }
        else{
            for (PackageVersion packageVersion : modPackage.getVersions()) {
                versionNums.add(packageVersion.getVersion_number());
            }
        }

        versionBox.getItems().addAll(versionNums);
        versionBox.getSelectionModel().select(0);
    }

    public void showNewButton(Node newButton){
        modInfoHbox.getChildren().remove(2);
        modInfoHbox.getChildren().add(newButton);
    }

    private Task initializeDownloadTask(){
        Task downloadTask = new Task() {
            @Override
            protected Object call() throws Exception {

                System.out.println("download thread: " + thisModPackage.getName());
                ModDownloader modDownloader = new ModDownloader();

                String selectedVersion = (String) versionBox.getSelectionModel().getSelectedItem();
                versionBox.setDisable(true);



                modDownloader.progressProperty().addListener(new ChangeListener<Number>() {
                    @Override
                    public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                        if((double) t1==1.0){
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    showNewButton(uninstallButton);
                                }
                            });

                            recentlyInstalledMods = modDownloader.getRecentlyInstalledMods();
                            setRecentlyInstalled(recentlyInstalledMods);
                        }
                        updateProgress((double) t1, 1.0);
                    }
                });
                modDownloader.downloadMod(thisModPackage, selectedVersion, gottenModPositions, modPackages);


                return null;
            }
        };
        downloadProgress.progressProperty().bind(downloadTask.progressProperty());
        return downloadTask;
    }

    public Button getUninstallButton(){
        return this.uninstallButton;
    }
    public Button getUpdateButton(){return this.updateButton;}

}
