package controllers;


import database.Database;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Line;
import javafx.util.Callback;
import mods.ModPackage;
import mods.PackageVersion;
import org.json.JSONObject;
import service.InputFilter;
import service.JsonReader;
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
    AnchorPane fullModAnchor = new AnchorPane();

    private Task onlineTask;

    private File gameDirectory;
    private Map<Integer, List<Node>> storedOnlineMods = new HashMap<>();
    private Map<String, Integer> gottenModPositions = new HashMap<>();
    ObservableList<String> searchedItems = FXCollections.observableArrayList();

    private ModFullPageController modFullPageController;


    private List<ModPackage> installedModPackages = new ArrayList<>();

    private int currentPageNumber = 0;
    private boolean showingOnlineMods = false;
    private boolean showingInstalledMods = true;

    List<ModPackage> modPackages;
    private int packagesSize;
    private boolean settingSearchPage = false;

    List<PackageVersion> installedVersions = new ArrayList<>();
    private int installedVersionsSize;

    private Database db = new Database();
    private Connection conn = db.connect();

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
                if((!storedOnlineMods.isEmpty()) && (storedOnlineMods.containsKey(pageNum) && (!showingInstalledMods))){
                    packageBox.getChildren().clear();
                    packageBox.getChildren().addAll(storedOnlineMods.get(pageNum));
                }
                else {
                    try {
                        PackageItemController packageItemController;
                        AnchorPane itemAnchorPane;
                        List<Node> shownMods = new ArrayList<>();
                        packageBox.getChildren().clear();
                        int recentInstallCount = 0;
                        for (int i = startNum; i < finalEndNum; i++) {

                            FXMLLoader fxmlLoader = new FXMLLoader();
                            fxmlLoader.setLocation(getClass().getResource("/view/packageItem.fxml"));

                            itemAnchorPane = fxmlLoader.load();

                            packageItemController = fxmlLoader.getController();

                            try {
                                if (showingOnlineMods) {
                                    packageItemController.setData(modPackages.get(i), installedModPackages, gottenModPositions, modPackages);
                                } else {
                                    packageItemController.setData(installedModPackages.get(i), installedModPackages, gottenModPositions, modPackages);
                                }
                            } catch (SQLException throwables) {
                                throwables.printStackTrace();
                            }

                            packageBox.getChildren().add(itemAnchorPane);

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

                            Task packageTask = packageItemController.getPackageItemTask();
                            PackageItemController finalPackageItemController = packageItemController;
                            packageTask.progressProperty().addListener(new ChangeListener<Number>() {
                                @Override
                                public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                                    if((double)t1 == 1.0){
                                        try {
                                            setRecentlyInstalled(finalPackageItemController.getRecentlyInstalledMods());
                                        }catch(SQLException s){
                                            s.printStackTrace();
                                        }
                                    }
                                }
                            });
                            if(!showingInstalledMods) {
                                shownMods.add(itemAnchorPane);
                            }
                        }
                        if(!showingInstalledMods) {
                            storedOnlineMods.put(pageNum, shownMods);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        return packageBox;
    }

    private void setRecentlyInstalled(List<PackageVersion> packageVersions) throws SQLException {
        if(packageVersions != null) {
            for (PackageVersion packageVersion : packageVersions) {
                if(!installedVersions.contains(packageVersion)) {
                    ModPackage recentlyInstalled = findModPackage(packageVersion);
                     int installedInt = db.modIsInstalled(recentlyInstalled.getName(), recentlyInstalled.getOwner(),
                             recentlyInstalled.getVersions().get(0).getVersion_number(), conn);

                    if(installedInt == 1){
                        recentlyInstalled.setInstalled(true);
                        recentlyInstalled.setNeedsUpdate(true);
                    }
                    else if(installedInt == 0){
                        recentlyInstalled.setInstalled(true);
                        recentlyInstalled.setNeedsUpdate(false);
                    }

                    recentlyInstalled.setInstalledPackageVersion(packageVersion);

                    this.installedModPackages.add(recentlyInstalled);
                    installedVersionsSize += 1;
                }
            }
        }
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
                    if(modPackage.isInstalled()){
                        installedModPackages.add(modPackage);
                    }
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
        Platform.runLater(new Runnable() {
            @Override
            public void run() {

                installedModsLabel.setDisable(true);
                allModsOnlineLabel.setDisable(false);

                packageBox.getChildren().clear();
                showingOnlineMods = false;
                showingInstalledMods = true;
                searchComboBox.setVisible(false);
                int maxPages = (int) Math.ceil((double) installedVersionsSize / (double) packagesPerPage);
                modPagination.setMaxPageIndicatorCount(maxPages);

                modPagination.setCurrentPageIndex(0);

                if (!sceneAnchorPane.getChildren().contains(modPagination)){
                    showModListPage();
                }
            }
        });
    }

    public void playButtonOnMouseClicked(){
        try {
            JSONObject configObject = JsonReader.readJsonFromFile("Config/Config.json");
            File gameDir = new File(configObject.getString("directory"));
            File gameExe = new File(gameDir + "/Risk Of Rain 2.exe");


            final ProcessBuilder pb = new ProcessBuilder(gameExe.getAbsolutePath());
            pb.directory(gameDir);
            final Process p = pb.start();

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

    public void showFullModPage(ModPackage modPackage) throws ParseException {

        modFullPageController.setData(modPackage);
        modListAnchorpane.setVisible(false);
        fullModAnchor.setVisible(true);
        sceneAnchorPane.getChildren().remove(modPagination);
        if(!sceneAnchorPane.getChildren().contains(fullModAnchor)) {
            sceneAnchorPane.getChildren().add(fullModAnchor);
        }
        AnchorPane.setBottomAnchor(fullModAnchor, 0.0);
        AnchorPane.setTopAnchor(fullModAnchor, 60.0);
        AnchorPane.setRightAnchor(fullModAnchor, 0.0);
        AnchorPane.setLeftAnchor(fullModAnchor, 0.0);
    }

    public void showModListPage(){
        sceneAnchorPane.getChildren().remove(fullModAnchor);
        sceneAnchorPane.getChildren().add(modPagination);

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
        ModPackage foundModPackage = modPackages.get(modPosition);
        return foundModPackage;
    }

}
