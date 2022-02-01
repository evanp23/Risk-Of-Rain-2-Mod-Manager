package controllers;


import database.Database;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Array;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

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
    private ImageView backButton;
    @FXML
    private TextField searchTextField;


    private final int packagesPerPage = 125;
    private AnchorPane fullModAnchor = new AnchorPane();
    private Task onlineTask;
    private Map<String, Integer> gottenModPositions = new HashMap<>();
    private ModFullPageController modFullPageController;
    private ConfirmWarnDialogController confirmWarnDialogController;
    private AnchorPane warnConfirmAnchor = new AnchorPane();
    private List<ModPackage> installedModPackages = new ArrayList<>();
    private boolean showingOnlineMods = false;
    private boolean showingInstalledMods = true;
    private ObservableList<ModPackage> modPackages = FXCollections.observableArrayList();
    private FilteredList<ModPackage> filteredModPackages;
    private int packagesSize;
    private int installedVersionsSize;
    private Database db = new Database();
    private Connection conn = db.connect();
    private File gameDirectory;
    private IntegerProperty packageBoxFill;
    private boolean searching;
    private File bepInDirectory;
    private double desiredVval;
    private ModPackage packageToExpand;
    private String launchParameter = null;
    private ObservableList<Node> filteredNodes = FXCollections.observableArrayList();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        try {
            getConfig();
        } catch (IOException e) {
            e.printStackTrace();
        }
        packageBoxFill = new SimpleIntegerProperty();

        this.onlineTask = initializeOnlineTask();
        new Thread(onlineTask).start();

        onlineTask.setOnSucceeded(event -> {
            System.out.println("succeeded");
            setUpSearch();

            AnchorPane.setTopAnchor(modPagination, 0.0);
            AnchorPane.setBottomAnchor(modPagination, 0.0);
            AnchorPane.setLeftAnchor(modPagination, 0.0);
            AnchorPane.setRightAnchor(modPagination, 0.0);

            filteredModPackages.setPredicate(installedPackage->installedPackage.isInstalled());
            int totalPages = (int) (Math.ceil(modPackages.size() * 1.0 / packagesPerPage));
            modPagination.setPageCount(totalPages);
            modPagination.setCurrentPageIndex(0);
            showMods(0);
            modPagination.setPageFactory(new Callback<Integer, Node>() {
                @Override
                public Node call(Integer integer) {
                    Platform.runLater(()->{
                        packageScrollPane.setContent(showMods(integer));
                    });
                    return packageScrollPane;
                }
            });
            if(launchParameter != null){
                List<String> initialParams = Arrays.asList(launchParameter.split("//"));
                String arg = initialParams.get(1);
                List<String> allArgs = Arrays.asList(arg.split("/"));
                ModPackage modPackage = modPackages.get(gottenModPositions.get(allArgs.get(3) + "-" + allArgs.get(4)));
                try {
                    showFullModPage(modPackage);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        });

        searchTextField.textProperty().addListener((obs, oldVal, newVal)->{
            if(showingInstalledMods && !newVal.isBlank()){
                filteredModPackages.setPredicate(searchedPackage->
                        newVal == null || newVal.isEmpty() || searchedPackage.getFull_name().toLowerCase().contains(newVal.toLowerCase())
                                && searchedPackage.isInstalled());
            }
            else if(showingInstalledMods){
                filteredModPackages.setPredicate(searchedPackage-> searchedPackage.isInstalled());
            }
            else if(showingOnlineMods && !newVal.isBlank()){
                filteredModPackages.setPredicate(searchedPackage->
                        newVal == null || newVal.isEmpty() || searchedPackage.getFull_name().toLowerCase().contains(newVal.toLowerCase()));
            }
            else{
                filteredModPackages.setPredicate(searchedPackage -> modPackages.contains(searchedPackage));
            }
            showMods(modPagination.getCurrentPageIndex());
        });

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




        backButton.setImage(new Image("/images/back.png"));
        backButton.setVisible(false);

        initializeModFullPage();


    }

    public VBox showMods(int pageNum){

        int fromIndex = pageNum * packagesPerPage;
        int toIndex = Math.min(fromIndex + packagesPerPage, modPackages.size());

        int minIndex = Math.min(toIndex, filteredModPackages.size());
        SortedList<ModPackage> sortedMods = new SortedList<>(
                FXCollections.observableArrayList(filteredModPackages.subList(Math.min(fromIndex, minIndex), minIndex)));

        packageBox.getChildren().clear();

        for(ModPackage modPackage : sortedMods){
            modPackage.getStoredController().getImage();
            AnchorPane storedNode = (AnchorPane) modPackage.getStoredController().getAnchorPane();
            if (!packageBox.getChildren().contains(storedNode)) {
                packageBox.getChildren().add(storedNode);
            }

            Line itemSeparator = new Line();
            itemSeparator.setStartX(0);
            itemSeparator.setEndX(packageScrollPane.getWidth() - 15);
            storedNode.getChildren().add(itemSeparator);
            AnchorPane.setBottomAnchor(itemSeparator, 0.0);

            packageBox.widthProperty().addListener((obs, oldVal, newVal)->{
                itemSeparator.setEndX(packageBox.getWidth());
            });
        }

        packageBox.setMinWidth(packageScrollPane.getWidth() - 15);

        packageScrollPane.widthProperty().addListener((obs, oldVal, newVal)->{
            packageBox.setMinWidth(packageScrollPane.getWidth() - 15);
        });

//        ObservableList<Node> sortedNodes = FXCollections.observableArrayList();
//
//        sortedMods.addListener(new ListChangeListener<ModPackage>() {
//            @Override
//            public void onChanged(Change<? extends ModPackage> c) {
//                System.out.println("sortedListChange");
//                while(c.next()){
//                    int start = c.getFrom();
//                    int end = c.getTo();
//                    if(c.wasAdded()){
//                        for(int i = start; i < end; i++){
//                            packageBox.getChildren().add(sortedMods.get(i).getStoredPackageItemNode());
//                        }
//                    }
//                    if(c.wasRemoved()){
//                        for(int i = start; i < end; i++){
//                            packageBox.getChildren().remove(sortedMods.get(i).getStoredPackageItemNode());
//                        }
//                    }
//                }
//            }
//        });

        //Bindings.bindContentBidirectional(filteredNodes, packageBox.getChildren());



//        int startNum = pageNum * packagesPerPage;
//        int endNum = (pageNum + 1) * packagesPerPage;
//        int finalSize;
//
//        if(showingInstalledMods){
//            finalSize = installedVersionsSize;
//        }
//        else{
//            finalSize = packagesSize;
//        }
//
//        if(endNum > finalSize){
//            endNum = finalSize;
//        }
//
//        packageBox.getChildren().clear();
//
//        int finalEndNum = endNum;
//        packageBox.getChildren().clear();
//        for (int i = startNum; i < finalEndNum; i++) {
//            ModPackage finalModPackage;
//            if (showingOnlineMods) {
//                finalModPackage = modPackages.get(i);
//            } else {
//                finalModPackage = installedModPackages.get(i);
//            }
//            AnchorPane itemAnchorPane = (AnchorPane) finalModPackage.getStoredController().getAnchorPane();
//            if(!packageBox.getChildren().contains(itemAnchorPane)){
//                packageBox.getChildren().add(itemAnchorPane);
//            }
//            finalModPackage.getStoredController().getImage();
//
//
//
//
//        }

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
//                    setInstalledPropertyListener(modPackage);
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
            filteredModPackages.setPredicate(modPackage -> modPackages.contains(modPackage));
            packageBox.getChildren().clear();
            installedModsLabel.setDisable(false);
            allModsOnlineLabel.setDisable(true);
            showingOnlineMods = true;
            showingInstalledMods = false;
            int maxPages = (int)Math.ceil((double)packagesSize / (double)packagesPerPage);
            modPagination.setMaxPageIndicatorCount(maxPages);
            modPagination.setCurrentPageIndex(0);
        }
    }

    public void installedLabelOnMouseClicked(){
        System.out.println("INSTALLED LABEL CLICKED");
        filteredModPackages.setPredicate(installedPackage->installedPackage.isInstalled());
        installedModsLabel.setDisable(true);
        allModsOnlineLabel.setDisable(false);
        packageBox.getChildren().clear();
        showingOnlineMods = false;
        showingInstalledMods = true;
        int maxPages = (int) Math.ceil((double) installedVersionsSize / (double) packagesPerPage);
        if(maxPages == 0){
            maxPages = 1;
        }
        modPagination.setMaxPageIndicatorCount(maxPages);
        modPagination.setCurrentPageIndex(0);
//        if (!sceneAnchorPane.getChildren().contains(modPagination)){
//            showModListPage();
//        }
    }

    public void playButtonOnMouseClicked(){
        try {
            URI uri = new URI("steam://run/632360//--doorstop-enable%20false");
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
        backButton.setVisible(true);
        fullModAnchor.setVisible(true);
        searchTextField.setVisible(false);
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
        searchTextField.setVisible(true);
        installedModsLabel.setDisable(false);
        backButton.setVisible(false);
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
            searchTextField.setDisable(true);

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
            searchTextField.setDisable(false);
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

//    private void setInstalledPropertyListener(ModPackage modPackage){
//        modPackage.installedProperty().addListener(new ChangeListener<Boolean>() {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//                System.out.println(modPackage.getName() + ": installedProperty: " + newValue + ": updateFlagged: " + modPackage.isFlaggedForUpdate() );
//                PackageItemController storedController = modPackage.getStoredController();
//                if (newValue && !modPackage.isFlaggedForUpdate()) {
//                    if(showingInstalledMods && !packageBox.getChildren().contains(storedController.getAnchorPane())){
//                        Platform.runLater(()-> {packageBox.getChildren().add(storedController.getAnchorPane());});
//                    }
//                    installedModPackages.add(modPackage);
//                    installedVersionsSize = installedModPackages.size();
//                    setUninstallListener(modPackage, storedController.getUninstallButton());
//                    setUpdateButtonListener(modPackage, storedController.getUpdateButton());
//                    db.addMod(modPackage, conn);
//                } else if(!modPackage.isFlaggedForUpdate()){
//                    if(showingInstalledMods && packageBox.getChildren().contains(storedController.getAnchorPane())){
//                        Platform.runLater(()-> {packageBox.getChildren().remove(storedController.getAnchorPane());});
//                    }
//                    installedModPackages.remove(modPackage);
//                    installedVersionsSize = installedModPackages.size();
//                    db.removeMod(modPackage, conn);
//                }
//                System.out.println(installedVersionsSize);
//                Platform.runLater(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            storedController.setState(newValue);
//                        } catch (SQLException sqlException) {
//                            sqlException.printStackTrace();
//                        } catch (IOException ioException) {
//                            ioException.printStackTrace();
//                        }
//                    }
//                });
//
//            }
//        });
//
//    }

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
        filteredModPackages = new FilteredList<>(modPackages, p -> true);
    }

    private int pageOf(ModPackage modPackage){
        int modPosition = gottenModPositions.get(modPackage.getFull_name());
        System.out.println((int) Math.ceil((double)modPosition / (double) packagesPerPage));
        return (int) ((double)modPosition / (double) packagesPerPage);
    }

    private double calculateModPositionInBox(ModPackage modPackage, int pageNum){
        double modAbsolutePosition = gottenModPositions.get(modPackage.getFull_name());
        System.out.println(modAbsolutePosition);
        double modRelativePosition = modAbsolutePosition - (packagesPerPage * pageNum);
        System.out.println(modRelativePosition);
        return modRelativePosition / packagesPerPage;
    }

    public void setLaunchParameter(String launchParameter){
        this.launchParameter = launchParameter;
        System.out.println(launchParameter);
    }

}
