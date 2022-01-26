package controllers;


import database.Database;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.util.Callback;
import mods.ModPackage;
import mods.PackageVersion;
import org.json.JSONObject;
import service.InputFilter;
import service.JsonReader;
import service.ModDownloader;
import service.PackageGetter;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;

public class PackageListingController implements Initializable {
    @FXML
    private VBox packageBox;
    @FXML
    private Label allModsOnlineLabel;
    @FXML
    private ScrollPane packageScrollPane;
    @FXML
    private HBox labelBox;
    @FXML
    private AnchorPane sceneAnchorPane;
    @FXML
    private Label installedModsLabel;
    @FXML
    private ImageView playButton;
    @FXML
    private Pagination modPagination;
    @FXML
    private AnchorPane modListAnchorpane;
    @FXML
    private Line listingLine;
    @FXML
    private ComboBox searchComboBox;
    @FXML
    private ImageView backButton;

    private final int packagesPerPage = 125;
    private AnchorPane fullModAnchor = new AnchorPane();
    private Task onlineTask;
    private Map<Integer, List<ModPackage>> storedOnlineMods = new HashMap<>();
    private Map<Integer, List<Node>> storedOnlineNodes = new HashMap<>();
    private Map<String, Integer> gottenModPositions = new HashMap<>();
    private ObservableList<String> searchedItems = FXCollections.observableArrayList();
    private ModFullPageController modFullPageController;
    private ConfirmWarnDialogController confirmWarnDialogController;
    private AnchorPane warnConfirmAnchor = new AnchorPane();
    private List<ModPackage> installedModPackages = new ArrayList<>();
    private boolean showingOnlineMods = false;
    private boolean showingInstalledMods = true;
    private List<ModPackage> modPackages;
    private int packagesSize;
    private boolean settingSearchPage = false;
    private List<PackageVersion> installedVersions = new ArrayList<>();
    private int installedVersionsSize;
    private Database db = new Database();
    private Connection conn = db.connect();
    private File bepInDirectory;
    private File gameDirectory;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            getConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.onlineTask = initializeOnlineTask();
        new Thread(onlineTask).start();

        installedModsLabel.setStyle("--fx-background-color=red;");
        installedModsLabel.setDisable(true);
        allModsOnlineLabel.setDisable(false);
        allModsOnlineLabel.setStyle("--fx-background-color=white;");

        Image playButtonImage = new Image("images/play.png");

        playButton.setImage(playButtonImage);
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initializeConfirmationWarnDialog();
            }
        });
        sceneAnchorPane.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                listingLine.setEndX(sceneAnchorPane.getWidth());
            }
        });
        modPagination.setPageFactory(new Callback<Integer, Node>() {
            @Override
            public Node call(Integer integer) {
                packageScrollPane.setContent(showMods(integer));
                return packageScrollPane;
            }
        });

        AnchorPane.setTopAnchor(modPagination, 0.0);
        AnchorPane.setBottomAnchor(modPagination, 0.0);
        AnchorPane.setLeftAnchor(modPagination, 0.0);
        AnchorPane.setRightAnchor(modPagination, 0.0);

        backButton.setImage(new Image("/images/back.png"));
        backButton.setVisible(false);
        searchComboBox.setVisible(false);
        searchComboBox.setEditable(true);
        initializeModFullPage();

        onlineTask.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if((double)t1 == 1.0){
                    int finalSize;
                    finalSize = packagesSize;
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            for (ModPackage modPackage : modPackages) {
                                searchedItems.add(modPackage.getName());
                            }
                        }
                    });
                    FilteredList<String> filteredModNames = new FilteredList<String>(searchedItems);
                    searchComboBox.getEditor().textProperty().addListener(new InputFilter(searchComboBox, filteredModNames, false, finalSize));
                    searchComboBox.setItems(filteredModNames);

                    searchComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
                        @Override
                        public void changed(ObservableValue observableValue, Object o, Object t1) {
                            if(!(t1 == null || t1.equals("") || t1.equals(o) || (t1 == null && o == null))) {
                                fullModAnchor.setMinHeight(sceneAnchorPane.getHeight());
                                fullModAnchor.setMinWidth(sceneAnchorPane.getWidth());
                                sceneAnchorPane.getChildren().remove(modPagination);
                                installedModsLabel.setDisable(true);
                                try {
                                    showFullModPage(modPackages.get(searchedItems.indexOf(t1)));
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                searchComboBox.setVisible(false);
                                backButton.setVisible(true);
                            }
                        }
                    });
                }
            }
        });
    }

    public VBox showMods(int pageNum){
        int startNum = pageNum * packagesPerPage;
        int endNum = (pageNum + 1) * packagesPerPage;
        int finalSize;

        if(showingInstalledMods){
            finalSize = installedVersionsSize;
        }
        else{
            finalSize = packagesSize;
        }

        if(endNum > finalSize){
            endNum = finalSize;
        }

        if(!settingSearchPage){
            packageScrollPane.setVvalue(0.0);
        }
        else{
            settingSearchPage = false;
        }
        packageBox.getChildren().clear();

        int finalEndNum = endNum;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                packageBox.getChildren().clear();
                for (int i = startNum; i < finalEndNum; i++) {
                    ModPackage finalModPackage;
                    if (showingOnlineMods) {
                        finalModPackage = modPackages.get(i);
                    } else {
                        finalModPackage = installedModPackages.get(i);
                    }
                    packageBox.getChildren().add(finalModPackage.getStoredPackageItemNode());
                    finalModPackage.getStoredController().getImage();

                    AnchorPane itemAnchorPane = (AnchorPane) finalModPackage.getStoredPackageItemNode();

                    Line itemSeparator = new Line();
                    itemSeparator.setStartX(0);
                    itemSeparator.setEndX(packageScrollPane.getWidth() - 15);
                    itemAnchorPane.getChildren().add(itemSeparator);
                    AnchorPane.setBottomAnchor(itemSeparator, 0.0);

                    packageScrollPane.widthProperty().addListener(new ChangeListener<Number>() {
                        @Override
                        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                            itemSeparator.setEndX(packageScrollPane.getWidth() - 15);
                        }
                    });
                }
            }
        });
        return packageBox;
    }

    private Task initializeOnlineTask(){
        Task onlineTask = new Task() {
            @Override
            protected Object call() throws Exception {
            PackageGetter getter = new PackageGetter();
            try {
                modPackages = getter.loadPackages(gottenModPositions);
                int count = 0;
                for(ModPackage modPackage : modPackages){
                    count++;
                    if(modPackage.isInstalled()) {
                        installedModPackages.add(modPackage);
                    }
                    setInstalledPropertyListener(modPackage);
                    drawPackageItem(modPackage);
                    updateProgress(count, modPackages.size());
                }
                installedVersionsSize = installedModPackages.size();
                packagesSize = modPackages.size();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
            }
        };
        return onlineTask;
    }


    public Task getOnlineTask(){
        return this.onlineTask;
    }

    public int getInstalledSize(){
        return installedVersionsSize;
    }


    private void getConfig() throws IOException {
        JSONObject configObj = JsonReader.readJsonFromFile("Config/Config.json");
        String gameDir = configObj.getString("directory");
        this.gameDirectory = new File(gameDir);
        this.bepInDirectory = new File(gameDirectory.getAbsolutePath() + "/BepInEx");
    }

    public void onMouseEnteredHand(){
        allModsOnlineLabel.getScene().setCursor(Cursor.HAND);
    }

    public void onMouseExitedHand(){
        allModsOnlineLabel.getScene().setCursor(Cursor.DEFAULT);
    }

    public void onlineLabelMouseClicked(){
        if(!showingOnlineMods || showingInstalledMods){
            searchComboBox.setDisable(false);
            searchComboBox.setVisible(true);
            packageBox.getChildren().clear();
            installedModsLabel.setDisable(false);
            allModsOnlineLabel.setDisable(true);
            showingOnlineMods = true;
            showingInstalledMods = false;
            int maxPages = (int)Math.ceil((double)packagesSize / (double)packagesPerPage);
            modPagination.setMaxPageIndicatorCount(maxPages);
            modPagination.setCurrentPageIndex(0);
            if (!sceneAnchorPane.getChildren().contains(modPagination)){
                showModListPage();
            }
        }

    }

    public void installedLabelOnMouseClicked(){
        installedModsLabel.setDisable(true);
        allModsOnlineLabel.setDisable(false);
        packageBox.getChildren().clear();
        showingOnlineMods = false;
        showingInstalledMods = true;
        searchComboBox.setVisible(false);
        int maxPages = (int) Math.ceil((double) installedVersionsSize / (double) packagesPerPage);
        if(maxPages == 0){
            maxPages = 1;
        }
        modPagination.setMaxPageIndicatorCount(maxPages);
        modPagination.setCurrentPageIndex(0);
        if (!sceneAnchorPane.getChildren().contains(modPagination)){
            showModListPage();
        }
    }

    public void playButtonOnMouseClicked(){
        try {
            JSONObject configObject = JsonReader.readJsonFromFile("Config/Config.json");
            File gameDir = new File(configObject.getString("directory"));
            File gameExe = new File(gameDir + "/Risk Of Rain 2.exe");

            ProcessBuilder pb;
            pb = new ProcessBuilder(gameExe.getAbsolutePath());
            pb.directory(gameDir);
            pb.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeModFullPage(){

        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/view/ModFullPage.fxml"));
        try {
            fullModAnchor = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        modFullPageController = fxmlLoader.getController();
        fullModAnchor.setMinWidth(modListAnchorpane.getWidth());
    }

    public void initializeConfirmationWarnDialog(){
        FXMLLoader fxmlLoader = new FXMLLoader();
        fxmlLoader.setLocation(getClass().getResource("/view/confirmWarnDialog.fxml"));
        try {
            warnConfirmAnchor = fxmlLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
        confirmWarnDialogController = fxmlLoader.getController();
        sceneAnchorPane.getChildren().add(warnConfirmAnchor);
        warnConfirmAnchor.setVisible(false);
    }

    public void showFullModPage(ModPackage modPackage) throws ParseException {

        modFullPageController.setData(modPackage);
        modListAnchorpane.setVisible(false);
        fullModAnchor.setVisible(true);
        if(!sceneAnchorPane.getChildren().contains(fullModAnchor)) {
            sceneAnchorPane.getChildren().add(fullModAnchor);
        }
        AnchorPane.setBottomAnchor(fullModAnchor, 0.0);
        AnchorPane.setTopAnchor(fullModAnchor, 55.0);
        AnchorPane.setRightAnchor(fullModAnchor, 0.0);
        AnchorPane.setLeftAnchor(fullModAnchor, 0.0);
    }

    public void showModListPage(){
        sceneAnchorPane.getChildren().remove(fullModAnchor);
        sceneAnchorPane.getChildren().remove(warnConfirmAnchor);
        sceneAnchorPane.getChildren().add(modPagination);
        sceneAnchorPane.getChildren().add(warnConfirmAnchor);
        fullModAnchor.setVisible(false);
        AnchorPane.setTopAnchor(modPagination, 55.0);
        AnchorPane.setBottomAnchor(modPagination, 0.0);
        AnchorPane.setLeftAnchor(modPagination, 0.0);
        AnchorPane.setRightAnchor(modPagination, 0.0);

    }

    public void onBackButtonClicked(){
        searchComboBox.getEditor().clear();
        installedModsLabel.setDisable(false);
        backButton.setVisible(false);
        searchComboBox.setVisible(true);
        showModListPage();
    }

    private ModPackage findModPackage(PackageVersion packageVersion){
        String packageName = packageVersion.getName();
        String packageAuthor = packageVersion.getNamespace();
        int modPosition = gottenModPositions.get(packageAuthor + "-" + packageName);
        return modPackages.get(modPosition);
    }

    private void confirmUninstall(ModPackage modPackage){
        initializeConfirmationWarnDialog();
        ModDownloader modDownloader = new ModDownloader();
        List<String> filesToRemove = new ArrayList<>();
        List<ModPackage> modsToRemove = new ArrayList<>();

        for(ModPackage installedMod : installedModPackages){
            if(installedMod.dependsOn(modPackage)){
                filesToRemove.add(installedMod.getFull_name());
                modsToRemove.add(installedMod);
            }
        }
        confirmWarnDialogController.showUninstallConfirmation(modPackage, modsToRemove);

        modsToRemove.add(modPackage);
        filesToRemove.add(modPackage.getFull_name());

        setWarnConfirmUI(true);

        Task removeTask = new Task() {
            @Override
            protected Object call() throws Exception {
            for(ModPackage uninstall : modsToRemove){
                try {
                    modDownloader.removeModFiles(uninstall.getFull_name(), modDownloader.getBepInDir());
                    uninstall.setInstalled(false);
                    uninstall.setInstalledPackageVersion(null);
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                            Node storedNode = uninstall.getStoredPackageItemNode();
                            PackageItemController storedController = uninstall.getStoredController();
                            if(showingInstalledMods) {
                                if(packageBox.getChildren().contains(storedNode)){
                                    packageBox.getChildren().remove(storedNode);
                                }
                            }
                            try {
                                storedController.setState(false);
                            } catch (SQLException sqlException) {
                                sqlException.printStackTrace();
                            } catch (IOException ioException) {
                                ioException.printStackTrace();
                            }
                        }
                    });
                    installedModPackages.remove(uninstall);
                    installedVersionsSize--;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
            }
        };

        confirmWarnDialogController.selectionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                int selection = (int) t1;

                //CANCEL
                if(selection == -1){
                    setWarnConfirmUI(false);

                }
                //USER CONFIRMED REMOVE MOD
                else if(selection == 1){
                    new Thread(removeTask).start();
                    removeTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                        @Override
                        public void handle(WorkerStateEvent workerStateEvent) {
                            for(ModPackage removeMod : modsToRemove){
                                db.removeMod(removeMod, conn);
                            }
                            setWarnConfirmUI(false);
                        }
                    });
                }
                confirmWarnDialogController.selectionProperty().set(-2);
            }
        });

    }

    private void confirmUpdate(ModPackage modPackage) throws SQLException, IOException {
        ModDownloader modDownloader = new ModDownloader();
        PackageVersion installedVersion = modPackage.getInstalledPackageVersion();
        List<String> filesToRemove = new ArrayList<>();
        final List<ModPackage> dependentsNeedingUpdate = new ArrayList<>();
        final List<PackageItemController> controllers = new ArrayList<>();

        for(ModPackage installedPackage : installedModPackages){
            if(installedPackage.dependsOn(modPackage) && installedPackage.needsUpdate()){
                PackageItemController controller = installedPackage.getStoredController();
                dependentsNeedingUpdate.add(installedPackage);
                controllers.add(controller);
            }
        }

        confirmWarnDialogController.showUpdateConfirmation(modPackage, dependentsNeedingUpdate);
        setWarnConfirmUI(true);

        //User confirmed update one mod
        confirmWarnDialogController.getConfirmationButton().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                for(PackageItemController controller : controllers){
                    if(controller != null){
                        try {
                            controller.setState(true);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        controller.startDownloadTask(true);
                    }
                }
            }
        });

        //User canceled mod update
        confirmWarnDialogController.getCancelButton().setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                setWarnConfirmUI(false);
            }
        });
    }

    private void setWarnConfirmUI(boolean setShowing){
        if(setShowing){
            DropShadow ds = new DropShadow(20, Color.GRAY);
            warnConfirmAnchor.setEffect(ds);
            warnConfirmAnchor.setVisible(true);
            warnConfirmAnchor.setDisable(false);
            labelBox.setDisable(true);
            modPagination.setDisable(true);
            searchComboBox.setDisable(true);

            sceneAnchorPane.widthProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    warnConfirmAnchor.setLayoutX((sceneAnchorPane.getWidth() / 2) - warnConfirmAnchor.getWidth() / 2);
                }
            });

            sceneAnchorPane.heightProperty().addListener(new ChangeListener<Number>() {
                @Override
                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                    warnConfirmAnchor.setLayoutY((sceneAnchorPane.getHeight() / 2) - (warnConfirmAnchor.getHeight() / 2));
                }
            });
        }
        else {
            warnConfirmAnchor.setVisible(false);
            modPagination.setDisable(false);
            labelBox.setDisable(false);
            searchComboBox.setDisable(false);
        }

    }

    private Node drawPackageItem(ModPackage packageToDraw) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/view/packageItem.fxml"));

        AnchorPane packageItemAnchorPane = loader.load();
        PackageItemController drawnPackageController = loader.getController();

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                try {
                    drawnPackageController.setData(packageToDraw, installedModPackages, gottenModPositions, modPackages);
                    drawnPackageController.setState(packageToDraw.isInstalled());
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                }
            }
        });

        packageToDraw.setDrawn(true);
        packageToDraw.setStoredController(drawnPackageController);
        packageToDraw.setStoredPackageItemNode(packageItemAnchorPane);

        if (packageToDraw.isInstalled()){
            setUninstallListener(packageToDraw, drawnPackageController.getUninstallButton());
            setUpdateButtonListener(packageToDraw, drawnPackageController.getUpdateButton());
        }

        return packageItemAnchorPane;
    }

    private void setInstalledPropertyListener(ModPackage modPackage){
        modPackage.installedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                System.out.println(modPackage.getName() + ":" + newValue);
                PackageItemController storedController = modPackage.getStoredController();
                if (newValue) {
                    installedModPackages.add(modPackage);
                    installedVersionsSize = installedModPackages.size();
                    setUninstallListener(modPackage, storedController.getUninstallButton());
                    setUpdateButtonListener(modPackage, storedController.getUpdateButton());
                    db.addMod(modPackage.getInstalledPackageVersion(), conn);
                } else {
                    installedModPackages.remove(modPackage);
                    installedVersionsSize = installedModPackages.size();
                    db.removeMod(modPackage, conn);
                }
                System.out.println(installedVersionsSize);
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            storedController.setState(newValue);
                        } catch (SQLException sqlException) {
                            sqlException.printStackTrace();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });

            }
        });

    }

    private void setUninstallListener(ModPackage modPackage, Button uninstallButton){
        uninstallButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                confirmUninstall(modPackage);
            }
        });
    }

    private void setUpdateButtonListener(ModPackage modPackage, Button updateButton){
        updateButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    confirmUpdate(modPackage);
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

}
