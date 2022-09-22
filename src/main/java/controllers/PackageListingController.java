package controllers;


import database.Database;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.stage.Stage;
import mods.ModPackage;
import mods.PackageVersion;
import service.*;


import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private ImageView backButton;
    @FXML
    private TextField searchTextField;

    private final int packagesPerPage = 125;
    private AnchorPane fullModAnchor = new AnchorPane();
    private Task<Integer> onlineTask;
    private Map<String, Integer> gottenModPositions = new HashMap<>();
    private ModFullPageController modFullPageController;
    private ConfirmWarnDialogController confirmWarnDialogController;
    private AnchorPane warnConfirmAnchor = new AnchorPane();
    private boolean showingOnlineMods = false;
    private boolean showingInstalledMods = true;
    private ObservableList<ModPackage> modPackages;
    private FilteredList<ModPackage> filteredModPackages;
    private String launchParameter = null;
    private int lastPageNum = -1;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        this.onlineTask = initializeOnlineTask();
        new Thread(onlineTask).start();

        onlineTask.setOnSucceeded(event -> {
            filteredModPackages = new FilteredList<>(modPackages, modPackage ->
                    (showingInstalledMods));

            AnchorPane.setTopAnchor(modPagination, 0.0);
            AnchorPane.setBottomAnchor(modPagination, 0.0);
            AnchorPane.setLeftAnchor(modPagination, 0.0);
            AnchorPane.setRightAnchor(modPagination, 0.0);

            filteredModPackages.setPredicate(ModPackage::isInstalled);

            int totalPages = (int) (Math.ceil(filteredModPackages.size() * 1.0 / packagesPerPage));
            modPagination.setMaxPageIndicatorCount(totalPages);
            modPagination.setPageFactory((pageNum)->{
                Platform.runLater(()->{
                    packageScrollPane.setContent(showMods(pageNum));
                });
                return packageScrollPane;
            });

            modPagination.setCurrentPageIndex(0);

            searchTextField.textProperty().addListener((obs, oldVal, newVal)->{
                if(showingInstalledMods && !newVal.isBlank()){
                    filteredModPackages.setPredicate(searchedPackage->
                            newVal.isEmpty() || searchedPackage.getFull_name().toLowerCase().contains(newVal.toLowerCase()) && searchedPackage.isInstalled());
                    modPagination.setCurrentPageIndex(0);
                    showMods(0);
                }
                else if(showingInstalledMods){
                    filteredModPackages.setPredicate(ModPackage::isInstalled);
                    modPagination.setCurrentPageIndex(0);
                    showMods(0);
                }
                else if(showingOnlineMods && !newVal.isBlank()){
                    if(lastPageNum == -1){
                        lastPageNum = modPagination.getCurrentPageIndex();
                    }
                    filteredModPackages.setPredicate(searchedPackage->
                            newVal.isEmpty() || searchedPackage.getFull_name().toLowerCase().contains(newVal.toLowerCase()));
                    modPagination.setCurrentPageIndex(0);
                    showMods(0);
                }
                else if(showingOnlineMods){
                    filteredModPackages.setPredicate(searchedPackage -> modPackages.contains(searchedPackage));
                    modPagination.setCurrentPageIndex(lastPageNum);
                    showMods(lastPageNum);
                    lastPageNum = -1;
                }

            });

            if(launchParameter == null) return;

            List<String> initialParams = Arrays.asList(launchParameter.split("//"));
            String arg = initialParams.get(1);
            List<String> allArgs = Arrays.asList(arg.split("/"));
            String namespace = allArgs.get(3);
            String modName = allArgs.get(4);
            String version = allArgs.get(5);
            String fullName = namespace + "-" + modName;
            ModPackage modPackage = modPackages.get(gottenModPositions.get(fullName));

            if(!modPackage.isInstalled()){
                try {
                    modPackage.getStoredController().setVersionBoxSelection(version);
                    onlineLabelMouseClicked();
                    searchTextField.setText(modPackage.getFull_name());
                    confirmDownload(modPackage);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                PackageVersion installedVersion = modPackage.getInstalledPackageVersion();
                PackageVersion givenVersion = modPackage.getVersionsMap().get(version);
                searchTextField.setText(fullName);

                if(!modPackage.needsUpdate() || !givenVersion.isNewerThan(installedVersion)){
                    warnUser("You already have a newer version installed!");
                    return;
                }
                try {
                    confirmUpdate(modPackage, false, null);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        installedModsLabel.setStyle("--fx-background-color=red;");
        installedModsLabel.setDisable(true);
        allModsOnlineLabel.setDisable(false);
        allModsOnlineLabel.setStyle("--fx-background-color=white;");

        Image playButtonImage = new Image("images/play.png");

        playButton.setImage(playButtonImage);

        Platform.runLater(this::initializeConfirmationWarnDialog);

        sceneAnchorPane.widthProperty().addListener((obs, oldVal, t1)->{
            listingLine.setEndX(sceneAnchorPane.getWidth());
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
        packageScrollPane.setVvalue(0.0);

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

        return packageBox;
    }

    private Task<Integer> initializeOnlineTask(){
        Task<Integer> onlineTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                PackageGetter getter = new PackageGetter();
                int count = 0;
                try {
                    List<ModPackage> unObsList = getter.loadPackages(gottenModPositions);
                    modPackages = FXCollections.observableArrayList(modPackage ->
                            new Observable[] {modPackage.installedProperty()});
                    
                    for(ModPackage modPackage : unObsList){
                        count++;
                        drawPackageItem(modPackage);
                        modPackages.add(modPackage);
                        updateProgress(count, unObsList.size());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return count;
            }
        };
        return onlineTask;
    }


    public Task<Integer> getOnlineTask(){
        return this.onlineTask;
    }



    // private void getConfig() throws IOException {
    //     JSONObject configObj = JsonReader.readJsonFromFile("Config/Config.json");
    //     String gameDir = configObj.getString("directory");
    //     // this.bepInDirectory = new File(gameDirectory.getAbsolutePath() + "/BepInEx");
    // }

    public void onMouseEnteredHand(){
        allModsOnlineLabel.getScene().setCursor(Cursor.HAND);
    }

    public void onMouseExitedHand(){
        allModsOnlineLabel.getScene().setCursor(Cursor.DEFAULT);
    }

    public void onlineLabelMouseClicked(){
        if(showingOnlineMods) return;

        filteredModPackages.setPredicate(modPackage -> modPackages.contains(modPackage));
        packageBox.getChildren().clear();
        installedModsLabel.setDisable(false);
        allModsOnlineLabel.setDisable(true);
        showingOnlineMods = true;
        showingInstalledMods = false;
        int totalPages = (int) (Math.ceil(filteredModPackages.size() * 1.0 / packagesPerPage));
        modPagination.setMaxPageIndicatorCount(totalPages);
        modPagination.setCurrentPageIndex(0);
    }

    public void installedLabelOnMouseClicked(){
        if(showingInstalledMods) return;

        filteredModPackages.setPredicate(ModPackage::isInstalled);
        installedModsLabel.setDisable(true);
        allModsOnlineLabel.setDisable(false);
        showingOnlineMods = false;
        showingInstalledMods = true;
        int totalPages = (int) (Math.ceil(filteredModPackages.size() * 1.0 / packagesPerPage));
        if(totalPages == 0){
            totalPages = 1;
        }
        modPagination.setMaxPageIndicatorCount(totalPages);
        modPagination.setCurrentPageIndex(0);
    }

    public void playButtonOnMouseClicked(){
        try {
            if(!Desktop.isDesktopSupported()) return;
            URI uri = new URI("steam://run/632360//--doorstop-enable%20false");

            Desktop.getDesktop().browse(uri);
            Stage stage = (Stage)playButton.getScene().getWindow();
            stage.setIconified(true);

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

    private void confirmDownload(ModPackage modPackage) throws SQLException, IOException {
        initializeConfirmationWarnDialog();
        ModDownloader modDownloader = new ModDownloader();
        ComboBox<String> versionBox = modPackage.getStoredController().getVersionBox();
        List<ModPackage> allToInstall = new ArrayList<>();
        modDownloader.getDownloadUrls(modPackage, (String) versionBox.getSelectionModel().getSelectedItem(), gottenModPositions, modPackages, allToInstall);


        confirmWarnDialogController.showDownloadConfirmation(modPackage, allToInstall);
        setWarnConfirmUI(true);

        confirmWarnDialogController.selectionProperty().addListener((obs, old, t1)->{
            if(!modPackage.isFlaggedForInstall()) return;

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
                modPackage.getStoredController().getModDownloaderTask().setOnSucceeded((event)->{
                    if(showingInstalledMods){
                        installedLabelOnMouseClicked();
                    }
                });
            }
            confirmWarnDialogController.selectionProperty().set(-2);

        });
    }

    private void confirmUninstall(ModPackage modPackage){
        initializeConfirmationWarnDialog();
        ModDownloader modDownloader = new ModDownloader();
        List<ModPackage> modsToRemove = new ArrayList<>();

        ModPackage bepInEx = modPackages.get(gottenModPositions.get("bbepis-BepInExPack"));


        FilteredList<ModPackage> installedMods = modPackages.filtered(ModPackage::isInstalled);
        if(modPackage.equals(bepInEx) && installedMods.size() > 1){
            for(ModPackage installedMod : installedMods){
                if(!installedMod.equals(bepInEx)){
                    modsToRemove.add(installedMod);
                    installedMod.flagForUninstall(true);
                }
            }
            modsToRemove.add(modPackage);
            modPackage.flagForUninstall(true);
        }
        else {
            FilteredList<ModPackage> dependents = installedMods.filtered(installedMod-> installedMod.dependsOn(modPackage));
            for (ModPackage installedMod : dependents) {
                modsToRemove.add(installedMod);
                installedMod.flagForUninstall(true);
            }
            modsToRemove.add(modPackage);
            modPackage.flagForUninstall(true);
        }

        confirmWarnDialogController.showUninstallConfirmation(modPackage, modsToRemove);

        setWarnConfirmUI(true);

        Task<Integer> removeTask = initializeUninstallTask(modsToRemove, modDownloader);

        confirmWarnDialogController.selectionProperty().addListener((obs, oldVal, t1)->{
            if(!modPackage.isFlaggedForUninstall()) return;

            int selection = (int) t1;
            //CANCEL
            if (selection == -1) {
                setWarnConfirmUI(false);
                for(ModPackage modPackage1 : modsToRemove){
                    modPackage1.flagForUninstall(false);
                }
            }
            //USER CONFIRMED REMOVE MOD
            else if (selection == 1) {
                System.out.println("remove task");
                new Thread(removeTask).start();
                removeTask.setOnSucceeded((event)->{
                    for (ModPackage removeMod : modsToRemove) {
                        removeMod.flagForUninstall(false);
                    }
                    setWarnConfirmUI(false);
                });
            }
            confirmWarnDialogController.selectionProperty().set(-2);
        });

    }

    private void confirmUpdate(ModPackage modPackage, boolean allMods, String version) throws SQLException, IOException {
        ModDownloader modDownloader = new ModDownloader();
        List<ModPackage> allModsToUpdate = new ArrayList<>();

        //TODO: list new dependencies which will be installed with update

        FilteredList<ModPackage> installedMods = modPackages.filtered(ModPackage::isInstalled);
        FilteredList<ModPackage> needingUpdate = installedMods.filtered(ModPackage::needsUpdate);
        if(allMods){
            for(ModPackage installed : needingUpdate){
                allModsToUpdate.add(installed);
                installed.flagForUpdate(true);
            }
        }
        modPackage.flagForUpdate(true);

        confirmWarnDialogController.showUpdateConfirmation(modPackage, allModsToUpdate);
        setWarnConfirmUI(true);

        String finalVersion = version;
        confirmWarnDialogController.selectionProperty().addListener((obs, oldVal, t1)->{
            if(!modPackage.isFlaggedForUpdate()) return;
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
                    String verToDl;
                    if(finalVersion == null){
                        verToDl = "";
                    }
                    else{
                        verToDl = finalVersion;
                    }
                    modDownloader.getDownloadUrls(modPackage, verToDl, gottenModPositions, modPackages, installPackages);
                } catch (SQLException | IOException throwables) {
                    throwables.printStackTrace();
                }
                modPackage.getStoredController().startDownloadTask(installPackages);
                setWarnConfirmUI(false);
            }
            confirmWarnDialogController.selectionProperty().set(-2);
        });
    }

    public void warnUser(String warningMessage){
        initializeConfirmationWarnDialog();
        confirmWarnDialogController.showWarning(warningMessage);
        setWarnConfirmUI(true);

        confirmWarnDialogController.selectionProperty().addListener((obs, oldVal, newVal)->{
            if((int) newVal != 1) return;
            setWarnConfirmUI(false);
        });
    }

    private void setWarnConfirmUI(boolean setShowing){
        if(!setShowing){
            warnConfirmAnchor.setVisible(false);
            modPagination.setDisable(false);
            labelBox.setDisable(false);
            searchTextField.setDisable(false);
            return;
        }

        DropShadow ds = new DropShadow(20, Color.GRAY);
        warnConfirmAnchor.setEffect(ds);
        warnConfirmAnchor.setVisible(true);
        warnConfirmAnchor.setDisable(false);
        labelBox.setDisable(true);
        modPagination.setDisable(true);
        searchTextField.setDisable(true);

        sceneAnchorPane.widthProperty().addListener((obs, oldVal, newVal)->{
            double val = (double) newVal;
            warnConfirmAnchor.setLayoutX((val / 2) - warnConfirmAnchor.getWidth() / 2);
        });

        sceneAnchorPane.heightProperty().addListener((obs, oldVal, newVal)->{
            warnConfirmAnchor.setLayoutY(((double)newVal / 2) - (warnConfirmAnchor.getHeight() / 2));
        });
    }

    private void drawPackageItem(ModPackage packageToDraw) throws IOException {
        System.out.println("DRAWING: " + packageToDraw.getName());
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/view/packageItem.fxml"));

        AnchorPane packageItemAnchorPane = loader.load();
        PackageItemController drawnPackageController = loader.getController();

        FilteredList<ModPackage> installedMods = modPackages.filtered(ModPackage::isInstalled);

        Platform.runLater(()->{
            try {
                drawnPackageController.setData(packageToDraw, installedMods, gottenModPositions, modPackages);
                drawnPackageController.setState(packageToDraw.isInstalled());
            } catch (IOException | SQLException ioException) {
                ioException.printStackTrace();
            }
        });

        packageToDraw.setDrawn(true);
        packageToDraw.setStoredController(drawnPackageController);


        setUninstallListener(packageToDraw, drawnPackageController.getUninstallButton());
        setInstalledPropertyListener(packageToDraw);
        setUpdateButtonListener(packageToDraw, drawnPackageController.getUpdateButton());
        setDownloadButtonListener(packageToDraw, drawnPackageController.getDownloadButton());
    }

    private void setDownloadButtonListener(ModPackage packageToDraw, Button downloadButton) {
        downloadButton.setOnMouseClicked((event -> {
            try {
                confirmDownload(packageToDraw);
            } catch (SQLException | IOException throwables) {
                throwables.printStackTrace();
            }
        }));
    }

    private void setUninstallListener(ModPackage modPackage, Button uninstallButton){
        uninstallButton.setOnMouseClicked((event)->{
            confirmUninstall(modPackage);
        });
    }

    private void setUpdateButtonListener(ModPackage modPackage, Button updateButton){
        updateButton.setOnMouseClicked((event)->{
            try {
                confirmUpdate(modPackage, false, null);
            } catch (SQLException | IOException sqlException) {
                sqlException.printStackTrace();
            }
        });
    }

    private Task<Integer> initializeUninstallTask(List<ModPackage> modsToRemove, ModDownloader modDownloader){
        Task<Integer> removeTask = new Task<Integer>() {
            @Override
            protected Integer call() throws Exception {
                int count = 0;
                for(ModPackage uninstall : modsToRemove){
                    try {
                        modDownloader.removeModFiles(uninstall.getFull_name(), modDownloader.getBepInDir());
                        uninstall.setInstalled(false);
                        uninstall.setInstalledPackageVersion(null);
                        uninstall.flagForUninstall(false);
                        count++;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return count;
            }
        };
        return removeTask;
    }

    public void setLaunchParameter(String launchParameter){
        this.launchParameter = launchParameter;
    }

    private void setInstalledPropertyListener(ModPackage modPackage){
        modPackage.installedProperty().addListener((obs, oldVal, newVal)->{
            PackageItemController storedController = modPackage.getStoredController();
            if (newVal && !modPackage.isFlaggedForUpdate()) {
                Database.addMod(modPackage);
            } else if(!modPackage.isFlaggedForUpdate()){
                Database.removeMod(modPackage);
            }
            Platform.runLater(()->{
                try {
                    storedController.setState(newVal);
                } catch (SQLException | IOException sqlException) {
                    sqlException.printStackTrace();
                }
                if(!showingInstalledMods || modPackage.isFlaggedForUpdate()) return;
                modPagination.setCurrentPageIndex(0);
                showMods(0);
            });
        });
    }
}
