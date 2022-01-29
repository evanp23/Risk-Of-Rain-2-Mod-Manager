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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import javafx.util.Callback;
import mods.ModPackage;
import mods.PackageVersion;
import org.controlsfx.control.SearchableComboBox;
import org.json.JSONObject;
import service.*;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;

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
    private String t1Saved = null;

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

        initializeModFullPage();


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
                    if(!packageBox.getChildren().contains(finalModPackage.getStoredController().getAnchorPane())){
                        packageBox.getChildren().add(finalModPackage.getStoredPackageItemNode());
                    }
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
                    if(!searchedItems.contains(modPackage.getFull_name())) {
                        Platform.runLater(() -> searchedItems.add(modPackage.getFull_name()));
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
            setUpSearch();
            searchComboBox.setVisible(false);
            searchComboBox.setEditable(true);
            return null;
            }
        };
        onlineTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                setUpSearch();
            }
        });
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
            URI uri = new URI("steam://run/632360");
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
                Stage stage = (Stage)playButton.getScene().getWindow();
                stage.setIconified(true);
            }
        } catch (IOException | URISyntaxException e) {
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
        confirmWarnDialogController.setParent(sceneAnchorPane);
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

    private void confirmDownload(ModPackage modPackage) throws SQLException, IOException {
        initializeConfirmationWarnDialog();
        ModDownloader modDownloader = new ModDownloader();
        ComboBox versionBox = modPackage.getStoredController().getVersionBox();
        System.out.println(versionBox.getSelectionModel().getSelectedItem());
        List<ModPackage> allToInstall = new ArrayList<>();
        modDownloader.getDownloadUrls(modPackage, (String) versionBox.getSelectionModel().getSelectedItem(), gottenModPositions, modPackages, allToInstall);


        confirmWarnDialogController.showDownloadConfirmation(modPackage, allToInstall);
        setWarnConfirmUI(true);

        confirmWarnDialogController.selectionProperty().addListener((obs, old, t1)->{
            if(modPackage.isFlaggedForInstall()) {
                if ((int) t1 == -1) {
                    setWarnConfirmUI(false);
                    for (ModPackage removeInstalledV : allToInstall) {
                        if (removeInstalledV.isFlaggedForInstall()) {
                            removeInstalledV.setInstalledPackageVersion(null);
                        }
                    }
                } else if ((int) t1 == 1) {
                    setWarnConfirmUI(false);
                    modPackage.getStoredController().startDownloadTask(allToInstall);
                }
                confirmWarnDialogController.selectionProperty().set(-2);
            }
        });
    }

    private void confirmUninstall(ModPackage modPackage){
        initializeConfirmationWarnDialog();
        ModDownloader modDownloader = new ModDownloader();
        List<ModPackage> modsToRemove = new ArrayList<>();

        ModPackage bepInEx = modPackages.get(gottenModPositions.get("bbepis-BepInExPack"));
        ModPackage r2api = modPackages.get(gottenModPositions.get("tristanmcpherson-R2API"));
        System.out.println(r2api.getFull_name());


        if(modPackage.equals(bepInEx) && installedVersionsSize > 1){
            System.out.println("equals");

            installedModPackages.remove(bepInEx);
            installedModPackages.add(bepInEx);


            System.out.println(installedModPackages);
            modsToRemove.addAll(installedModPackages);
            System.out.println("size: " + installedModPackages.size() + ":" + modsToRemove.size());
            for(ModPackage modPackage1 : modsToRemove){
                modPackage1.flagForUninstall(true);
            }
        }
        else {
            for (ModPackage installedMod : installedModPackages) {
                if (installedMod.dependsOn(modPackage)) {
                    modsToRemove.add(installedMod);
                    installedMod.flagForUninstall(true);
                }
            }
            modsToRemove.add(modPackage);
            modPackage.flagForUninstall(true);
        }

        confirmWarnDialogController.showUninstallConfirmation(modPackage, modsToRemove);

        setWarnConfirmUI(true);

        Task removeTask = initializeUninstallTask(modsToRemove, modDownloader);

        List<ModPackage> finalModsToRemove = modsToRemove;
        confirmWarnDialogController.selectionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(modPackage.isFlaggedForUninstall()) {
                    int selection = (int) t1;
                    //CANCEL
                    if (selection == -1) {
                        setWarnConfirmUI(false);
                        for(ModPackage modPackage1 : finalModsToRemove){
                            modPackage1.flagForUninstall(false);
                        }
                    }
                    //USER CONFIRMED REMOVE MOD
                    else if (selection == 1) {
                        new Thread(removeTask).start();
                        removeTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                            @Override
                            public void handle(WorkerStateEvent workerStateEvent) {
                                for (ModPackage removeMod : finalModsToRemove) {
                                    db.removeMod(removeMod, conn);
                                    removeMod.flagForUninstall(false);
                                }
                                setWarnConfirmUI(false);
                            }
                        });
                    }
                    confirmWarnDialogController.selectionProperty().set(-2);
                }
            }
        });

    }

    private void confirmUpdate(ModPackage modPackage, boolean allMods) throws SQLException, IOException {
        ModDownloader modDownloader = new ModDownloader();
        List<ModPackage> allModsToUpdate = new ArrayList<>();
        if(allMods){
            for(ModPackage installed : installedModPackages){
                if(installed.needsUpdate()){
                    allModsToUpdate.add(installed);
                    installed.flagForUpdate(true);
                }
            }
        }
        modPackage.flagForUpdate(true);

        confirmWarnDialogController.showUpdateConfirmation(modPackage, allModsToUpdate);
        setWarnConfirmUI(true);

        confirmWarnDialogController.selectionProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if(modPackage.isFlaggedForUpdate()) {
                    int selection = (int) t1;

                    //CANCEL
                    if (selection == -1) {

                        for(ModPackage removeFlag : allModsToUpdate){
                            removeFlag.flagForUpdate(false);
                        }
                        setWarnConfirmUI(false);
                    }
                    //USER CONFIRMED UPDATE MOD
                    else if (selection == 1) {
                        List<ModPackage> installPackages = new ArrayList<>();
                        setWarnConfirmUI(false);
                        try {
                            modPackage.setInstalled(false);
                            modPackage.getStoredController().setState(false);
                            modDownloader.getDownloadUrls(modPackage, "", gottenModPositions, modPackages, installPackages);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        modPackage.getStoredController().startDownloadTask(installPackages);
//                        for(ModPackage removeFlag : allModsToUpdate){
//                            removeFlag.flagForUpdate(false);
//                        }
                        setWarnConfirmUI(false);
                    }
                    confirmWarnDialogController.selectionProperty().set(-2);
                }
            }
        });

//        //User confirmed update one mod
//        confirmWarnDialogController.getConfirmationButton().setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                for(PackageItemController controller : controllers){
//                    if(controller != null){
//                        try {
//                            controller.setState(true);
//                        } catch (SQLException throwables) {
//                            throwables.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        controller.startDownloadTask(true);
//                    }
//                }
//            }
//        });
//
//        //User canceled mod update
//        confirmWarnDialogController.getCancelButton().setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                setWarnConfirmUI(false);
//            }
//        });
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
        setDownloadButtonListener(packageToDraw, drawnPackageController.getDownloadButton());

        return packageItemAnchorPane;
    }

    private void setDownloadButtonListener(ModPackage packageToDraw, Button downloadButton) {
        downloadButton.setOnMouseClicked((event -> {
            try {
                confirmDownload(packageToDraw);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
    }

    private void setInstalledPropertyListener(ModPackage modPackage){
        modPackage.installedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                System.out.println(modPackage.getName() + ": installedProperty: " + newValue + ": updateFlagged: " + modPackage.isFlaggedForUpdate() );
                PackageItemController storedController = modPackage.getStoredController();
                if (newValue && !modPackage.isFlaggedForUpdate()) {
                    if(showingInstalledMods && !packageBox.getChildren().contains(storedController.getAnchorPane())){
                        Platform.runLater(()-> {packageBox.getChildren().add(storedController.getAnchorPane());});
                    }
                    installedModPackages.add(modPackage);
                    installedVersionsSize = installedModPackages.size();
                    setUninstallListener(modPackage, storedController.getUninstallButton());
                    setUpdateButtonListener(modPackage, storedController.getUpdateButton());
                    db.addMod(modPackage.getInstalledPackageVersion(), conn);
                } else if(!modPackage.isFlaggedForUpdate()){
                    if(showingInstalledMods && packageBox.getChildren().contains(storedController.getAnchorPane())){
                        Platform.runLater(()-> {packageBox.getChildren().remove(storedController.getAnchorPane());});
                    }
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
                    confirmUpdate(modPackage, false);
                } catch (SQLException sqlException) {
                    sqlException.printStackTrace();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
    }

    private Task initializeUninstallTask(List<ModPackage> modsToRemove, ModDownloader modDownloader){
        Task removeTask = new Task() {
            @Override
            protected Object call() throws Exception {
                for(ModPackage uninstall : modsToRemove){
                    try {
                        modDownloader.removeModFiles(uninstall.getFull_name(), modDownloader.getBepInDir());
                        uninstall.setInstalled(false);
                        uninstall.setInstalledPackageVersion(null);
                        uninstall.flagForUninstall(false);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        };
        return removeTask;
    }

    private void setUpSearch(){
        searchComboBox.setItems(searchedItems);
        AutoCompleteComboBoxListener<String> listener = new AutoCompleteComboBoxListener<>(searchComboBox);
        searchComboBox.getSelectionModel().selectedItemProperty().addListener(new ChangeListener() {
            @Override
            public void changed(ObservableValue observableValue, Object o, Object t1) {
                System.out.println(o + ": " + t1);
                System.out.println("saved: " + t1Saved);
                System.out.println("********************");
                if(!(t1 == null || t1.equals("") || t1.equals(o))){
                    System.out.println("searchd: " + t1);
                    fullModAnchor.setMinHeight(sceneAnchorPane.getHeight());
                    fullModAnchor.setMinWidth(sceneAnchorPane.getWidth());
                    sceneAnchorPane.getChildren().remove(modPagination);
                    installedModsLabel.setDisable(true);
                    try {
                        showFullModPage(modPackages.get(gottenModPositions.get(t1)));
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    searchComboBox.setVisible(false);
                    backButton.setVisible(true);
                }
            }
        });
    }

    private int pageOf(ModPackage modPackage){
        int modPosition = gottenModPositions.get(modPackage.getFull_name());
        System.out.println((int) Math.ceil((double)modPosition / (double) packagesPerPage));
        return (int) ((double)modPosition / (double) packagesPerPage);
    }

}
