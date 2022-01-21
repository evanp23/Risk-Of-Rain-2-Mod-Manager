package controllers;

import database.Database;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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

    private Database db = new Database();
    private Connection conn = db.connect();
    private Map<String, Integer> gottenModPositions;
    List<ModPackage> modPackages;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {


        ModDownloader modDownloader = new ModDownloader(conn, db);


        instLabel.setAlignment(Pos.CENTER);

        modInfoAnchor.setVisible(false);
        showingInfoAnchor = false;
        itemAnchorPane.setMinHeight(57);

        Task downloadTask = new Task() {
            @Override
            protected Object call() throws Exception {

                String selectedVersion = (String) versionBox.getSelectionModel().getSelectedItem();
                modDownloader.downloadMod(thisModPackage, selectedVersion, gottenModPositions, modPackages);

                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {

                        instLabel.setText("Installed");
                        instLabel.setMinWidth(90);
                        modInfoHbox.getChildren().remove(3);

                    }
                });
                recentlyInstalledMods = modDownloader.getRecentlyInstalledMods();
                setRecentlyInstalled(recentlyInstalledMods);
                updateProgress(1, 1);


                //TODO: SET THREAD PROGRESS TO FULL ONLY HERE
                return null;
            }
        };

        this.modDownloaderTask = downloadTask;

        downloadButton.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                downloadButton.getScene().setCursor(Cursor.HAND);
            }
        });

        downloadButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                modInfoHbox.getChildren().add(3, downloadProgress);
                modInfoHbox.getChildren().remove(downloadButton);

                instLabel.setText("Installing");
                modInfoHbox.getChildren().add(2, instLabel);


                new Thread(modDownloaderTask).start();
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Button getDownloadButton(){
        return this.downloadButton;
    }

    public void setData(ModPackage modPackage, List<ModPackage> installedModPackages, Map<String, Integer> allModsMap, List<ModPackage> allOnlineMods) throws IOException, SQLException {
        this.gottenModPositions = allModsMap;
        this.modPackages = allOnlineMods;

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

            modInfoHbox.getChildren().remove(3);

            List<String> versionNums = new ArrayList<>();

            for (PackageVersion packageVersion : modPackage.getVersions()) {
                versionNums.add(packageVersion.getVersion_number());
            }


            versionBox.getItems().addAll(versionNums);
            versionBox.getSelectionModel().selectFirst();

            descLabel.setText(thisModPackage.getVersions().get(0).getDescription());
            modInfoAnchor.getChildren().add(descLabel);

            if(modPackage.isInstalled()) {
                setInstalledUI(modPackage, installedModPackages);
            }

            downloadButton.setMinWidth(90);

            Task task = new Task() {
                @Override
                protected Object call() throws Exception {
                    Image iconImage = new Image(modPackage.getVersions().get(0).getIcon());

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
        itemAnchorPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                startAnimation();
            }
        });
        downloadButton.setFocusTraversable(false);
    }

    public String returnData(){
        String str = "Name: " + modNameLabel.getText() + ", Owner: " + modOwnerLabel.getText();
        return str;
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

    private void setInstalledUI(ModPackage modPackage, List<ModPackage> installedPackages){
        List<String> filesToRemove = new ArrayList<>();
        versionBox.getSelectionModel().select(modPackage.getInstalledPackageVersion().getVersion_number());
        versionBox.setDisable(true);
        if(modPackage.needsUpdate()){
            downloadButton.setStyle("-fx-background-color: #2B801A;");
            downloadButton.setText(modPackage.getVersions().get(0).getVersion_number());
        }
        else{
            downloadButton.setText("Uninstall");
            downloadButton.setStyle("-fx-background-color: #c43323;");

            downloadButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
                @Override
                public void handle(MouseEvent mouseEvent) {
                    ModDownloader modDownloader = new ModDownloader(conn, db);

                    for(ModPackage installedMod : installedPackages){
                        if(installedMod.dependsOn(modPackage)){
                            System.out.println(installedMod.getName() + " depends on " + modPackage.getName());
                            filesToRemove.add(installedMod.getFull_name());
                        }
                    }


                }
            });

        }
    }
}
