package controllers;


import javafx.fxml.Initializable;

public abstract class Controller implements Initializable {

//    @FXML
//    private VBox packageBox;
//
//    @FXML
//    private Label allModsOnlineLabel;
//
//    @FXML
//    private Label previousPageLabel;
//
//    @FXML
//    private Label nextPageLabel;
//
//    @FXML
//    private ScrollPane packageScrollPane;
//
//    @FXML
//    private HBox modInfoHbox;
//
//    private int pageNumber;
//    private boolean showingOnlineMods = false;
//
//    List<ModPackage> modPackages = new ArrayList<>();
//
//
//    @Override
//    public void initialize(URL url, ResourceBundle resourceBundle) {
//        if(pageNumber == 0){
//            previousPageLabel.setVisible(false);
//        }
//        allModsOnlineLabel.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                allModsOnlineLabel.getScene().setCursor(Cursor.HAND);
//            }
//        });
//
//        allModsOnlineLabel.setOnMouseExited(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                allModsOnlineLabel.getScene().setCursor(Cursor.DEFAULT);
//            }
//        });
//        allModsOnlineLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                if(showingOnlineMods){
//
//                }
//                else{
//                    showMods(0);
//                }
//
//            }
//        });
//
//        nextPageLabel.setOnMouseEntered(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                nextPageLabel.getScene().setCursor(Cursor.HAND);
//            }
//        });
//
//        nextPageLabel.setOnMouseExited(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                allModsOnlineLabel.getScene().setCursor(Cursor.DEFAULT);
//            }
//        });
//        nextPageLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
//            @Override
//            public void handle(MouseEvent mouseEvent) {
//                pageNumber++;
//                showMods(pageNumber);
//            }
//        });
//
//        Platform.runLater(new Runnable() {
//            @Override
//            public void run() {
//                PackageGetter getter = new PackageGetter();
//                try {
//                    modPackages = getter.loadPackages();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//
//
//    }
//
//    public void showMods(int pageNum){
//
//
//
//        int startNum = pageNum * 20;
//        int endNum = (pageNum + 1) * 20;
//        int packagesSize = modPackages.size();
//
//        if(packagesSize - endNum <= 20){
//            endNum = packagesSize - 1;
//            nextPageLabel.setVisible(false);
//        }
//
//        int packageBoxCount = 0;
//        showingOnlineMods = true;
//
//        packageScrollPane.setVvalue(0.0);
//
//        packageBox.getChildren().clear();
//        PackageItem packageItem = null;
//        AnchorPane itemAnchorPane = null;
//        try {
//            for(int i = startNum; i <= endNum; i++){
//                FXMLLoader fxmlLoader = new FXMLLoader();
//                fxmlLoader.setLocation(getClass().getResource("/packageItem.fxml"));
//
//                itemAnchorPane = fxmlLoader.load();
//
//                packageItem = fxmlLoader.getController();
//                packageItem.showUninstallConfirmation(modPackages.get(i));
//
//
//
//                packageBox.getChildren().add(itemAnchorPane);
//                packageBox.setMinWidth(packageScrollPane.getWidth());
//
//                packageScrollPane.widthProperty().addListener(new ChangeListener<Number>() {
//                    @Override
//                    public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
//                        packageBox.setMinWidth(packageScrollPane.getWidth());
//                    }
//                });
////                //add new item after clicked item
////                AnchorPane finalItemAnchorPane = itemAnchorPane;
////                itemAnchorPane.setOnMouseClicked(new EventHandler<MouseEvent>() {
////                    @Override
////                    public void handle(MouseEvent mouseEvent) {
////                        int index = packageBox.getChildren().indexOf(finalItemAnchorPane) + 1;
////
////                        //TODO: add new element with mod details
////
////                    }
////                });
//
//            }
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//    }


}
