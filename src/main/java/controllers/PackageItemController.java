package controllers;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import mods.ModPackage;
import mods.PackageVersion;
import service.ModDownloader;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;

public class PackageItemController implements Initializable {
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
    private ComboBox<String> versionBox;
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
    private ModPackage thisModPackage;
    private List<PackageVersion> recentlyInstalledMods;
    private Task<Integer> modDownloaderTask;
    private boolean showingInfoAnchor = false;
    private boolean imageIsLoaded = false;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        downloadProgress.setMinWidth(90);
        modInfoHbox.getChildren().remove(downloadProgress);

        itemAnchorPane.setMinHeight(57);
        modInfoAnchor.setMinHeight(142);

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

        uninstallButton.setOnMouseEntered((event)->{
            onMouseEntered();
        });
        uninstallButton.setOnMouseExited((event -> {
            onMouseExited();
        }));
        updateButton.setOnMouseEntered((event -> {
            onMouseEntered();
        }));
        updateButton.setOnMouseExited((event -> {
            onMouseExited();
        }));
    }

    public Task<Integer> getPackageItemTask(){
        return this.modDownloaderTask;
    }

    public List<PackageVersion> getRecentlyInstalledMods(){
        return this.recentlyInstalledMods;
    }

    public void setData(ModPackage modPackage, List<ModPackage> installedModPackages, Map<String, Integer> allModsMap, List<ModPackage> allOnlineMods) throws IOException, SQLException {
        this.thisModPackage = modPackage;

        Label descLabel = new Label();
        descLabel.setMaxWidth(200);
        descLabel.setWrapText(true);
        descLabel.setEllipsisString("...");

        if(modPackage.getName() != null) {
            modOwnerLabel.setText("by " + modPackage.getOwner());

            if (modPackage.isIs_deprecated()) {
                modNameLabel.setText(modPackage.getName() + " - DEPRECATED");
                modNameLabel.setStyle("-fx-text-fill: red");
            } else {
                modNameLabel.setText(modPackage.getName());
            }

            descLabel.setText(thisModPackage.getVersions().get(0).getDescription());
            modInfoAnchor.getChildren().add(descLabel);
        }
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

            detailsChevron.setRotate(90);
            modInfoAnchor.setVisible(true);
            itemAnchorPane.setMinHeight(199);
            showingInfoAnchor = true;
        }
    }

    public void setInstalledUI(){
        updateButton.setText("Update");
        showNewButton(uninstallButton);
        populateVersionBox( true);
    }

    public void setUninstalledUI(){
        if(thisModPackage.isFlaggedForUpdate()){
            showNewButton(downloadProgress);
        }
        else{
            showNewButton(downloadButton);
        }
        populateVersionBox(false);
    }

    public void setState(boolean installed) throws SQLException, IOException {
        if(installed){
            setInstalledUI();
        }
        else{
            setUninstalledUI();
        }
    }

    public void startDownloadTask(List<ModPackage> modsToInstall){
        this.modDownloaderTask = initializeDownloadTask(modsToInstall);
        showNewButton(downloadProgress);
        downloadProgress.progressProperty().bind(modDownloaderTask.progressProperty());
        new Thread(modDownloaderTask).start();
    }

    public Task<Integer> getModDownloaderTask(){
        return this.modDownloaderTask;
    }

    public void getImage(){
        if(imageIsLoaded) return;

        Task<Integer> getImageTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                Image iconImage = new Image(thisModPackage.getVersions().get(0).getIcon());

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        packageImage.setImage(iconImage);
                    }
                });

                return 1;
            }
        };
        new Thread(getImageTask).start();
        imageIsLoaded = true;
    }

    public void populateVersionBox(boolean installed){
        versionBox.getItems().clear();
        List<String> versionNums = new ArrayList<>();

        if(installed) {
            String installedVersion = thisModPackage.getInstalledPackageVersion().getVersion_number();
            if(thisModPackage.needsUpdate()) {
                String latestVersion = thisModPackage.getVersions().get(0).getVersion_number();
                versionNums.add(installedVersion);
                versionNums.add(latestVersion + " (new)");
                versionBox.setDisable(false);

                versionBox.setStyle("-fx-text-fill: green;");

                versionBox.valueProperty().addListener(new ChangeListener<String>() {
                    @Override
                    public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                        if(newValue==null) return;

                        if (newValue.equals(latestVersion + " (new)")) {
                            showNewButton(updateButton);
                        } else if (newValue.equals(installedVersion)) {
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
            for (PackageVersion packageVersion : thisModPackage.getVersions()) {
                versionNums.add(packageVersion.getVersion_number());
                versionBox.setDisable(false);
            }
        }
        versionBox.getItems().addAll(versionNums);
        versionBox.getSelectionModel().select(0);
    }

    public void showNewButton(Node newButton){
        modInfoHbox.getChildren().remove(2);
        modInfoHbox.getChildren().add(newButton);
    }

    private Task<Integer> initializeDownloadTask(List<ModPackage> modsToInstall){
        Task<Integer> downloadTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                ModDownloader modDownloader = new ModDownloader();

                modDownloader.progressProperty().addListener((obs, oldVal, newVal)->{
                    updateProgress((double)newVal, 1.0);
                });

                modDownloader.downloadMod(modsToInstall);
                return null;
            }
        };
        return downloadTask;
    }

    public Button getUninstallButton(){
        return this.uninstallButton;
    }
    public Button getUpdateButton(){return this.updateButton;}

    public Button getDownloadButton(){
        return this.downloadButton;
    }

    public Node getAnchorPane(){
        return this.itemAnchorPane;
    }

    public void onMouseEntered(){
        itemAnchorPane.getScene().setCursor(Cursor.HAND);
    }

    public void onMouseExited(){
        itemAnchorPane.getScene().setCursor(Cursor.DEFAULT);
    }

    public ComboBox<String> getVersionBox(){
        return this.versionBox;
    }

    public void setVersionBoxSelection(String selection){
        versionBox.getSelectionModel().select(selection);
    }

}
