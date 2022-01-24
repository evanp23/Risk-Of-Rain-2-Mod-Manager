package controllers;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import mods.ModPackage;


import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ConfirmWarnDialogController implements Initializable {

    @FXML
    private AnchorPane confirmationAnchor;

    @FXML
    private Label confirmationLabel;

    @FXML
    private ScrollPane dependentScrollPane;

    @FXML
    private Button confirmationButton;

    @FXML
    private VBox dependentsBox;

    @FXML
    private Label modNameLabel;

    @FXML
    private Button cancelButton;

    @FXML
    private HBox buttonHBox;

    private Button updateAllButton;
    private double emptyHeight = 180.0;
    private double fullHeight = 280;
    private double width = 440;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        updateAllButton = new Button();
        updateAllButton.setText("Update All");
        updateAllButton.setMinWidth(100);
        cancelButton.setMinWidth(100);
        confirmationButton.setMinWidth(100);
        confirmationAnchor.setMinHeight(fullHeight);
        confirmationAnchor.setMaxHeight(fullHeight);
        confirmationAnchor.setMinWidth(width);
        confirmationAnchor.setMaxWidth(width);
    }

    public void showUninstallConfirmation(ModPackage packageToRemove, List<ModPackage> dependents) {
        dependentsBox.getChildren().clear();
        if(buttonHBox.getChildren().contains(updateAllButton)){
            buttonHBox.getChildren().remove(updateAllButton);
        }
        modNameLabel.setText(packageToRemove.getName());
        confirmationButton.setText("Uninstall");

        double parentHeight = confirmationAnchor.getParent().getLayoutBounds().getHeight();
        double parentWidth = confirmationAnchor.getParent().getLayoutBounds().getWidth();

        if (!dependents.isEmpty()) {
            confirmationLabel.setText("Removing this mod will also remove");
            if(!confirmationAnchor.getChildren().contains(dependentScrollPane)){
                confirmationAnchor.getChildren().add(dependentScrollPane);
            }
            for (ModPackage dependent : dependents) {
                dependentsBox.getChildren().add(new Label(dependent.getName()));
            }
            dependentScrollPane.setVisible(true);
            confirmationAnchor.setMinHeight(fullHeight);
            confirmationAnchor.setMaxHeight(fullHeight);
            confirmationAnchor.setMinWidth(width);
            confirmationAnchor.setMaxWidth(width);
            confirmationAnchor.setLayoutX((parentWidth / 2) - width / 2);
            confirmationAnchor.setLayoutY((parentHeight / 2) - fullHeight / 2);
        } else {
            confirmationAnchor.getChildren().remove(dependentScrollPane);
            confirmationAnchor.setMinHeight(emptyHeight);
            confirmationAnchor.setMaxHeight(emptyHeight);
            confirmationAnchor.setMinWidth(width);
            confirmationAnchor.setMaxWidth(width);
            confirmationLabel.setText("Uninstall mod?");
            confirmationAnchor.setLayoutX((parentWidth / 2) - width / 2);
            confirmationAnchor.setLayoutY((parentHeight / 2) - emptyHeight / 2);
        }


        System.out.println("dd " + parentHeight + ":" + width + ": " + confirmationAnchor.getHeight() + ":" + confirmationAnchor.getWidth());


    }

    public void showUpdateConfirmation(ModPackage packageToUpdate, List<ModPackage> dependenciesNeedingUpdate){
        if(buttonHBox.getChildren().contains(updateAllButton)){
            buttonHBox.getChildren().remove(updateAllButton);
        }
        double parentHeight = confirmationAnchor.getParent().getLayoutBounds().getHeight();
        double parentWidth = confirmationAnchor.getParent().getLayoutBounds().getWidth();
        dependentsBox.getChildren().clear();
        modNameLabel.setText(packageToUpdate.getName());
        confirmationButton.setText("Update");
        if(dependenciesNeedingUpdate.size() != 0) {
            if(!confirmationAnchor.getChildren().contains(dependentScrollPane)){
                confirmationAnchor.getChildren().add(dependentScrollPane);
            }
            for (ModPackage dependency : dependenciesNeedingUpdate) {
                dependentsBox.getChildren().add(new Label(dependency.getName()));
            }
            dependentScrollPane.setVisible(true);
            confirmationLabel.setText("Some dependencies also need updates: ");
            buttonHBox.getChildren().add(updateAllButton);
            confirmationButton.setText("Just update\nthis mod");
            confirmationAnchor.setMinHeight(fullHeight);
            confirmationAnchor.setMaxHeight(fullHeight);
            confirmationAnchor.setMinWidth(width);
            confirmationAnchor.setMaxWidth(width);
            confirmationAnchor.setLayoutX((parentWidth / 2) - width / 2);
            confirmationAnchor.setLayoutY((parentHeight / 2) - fullHeight / 2);
        }
        else{
            confirmationAnchor.getChildren().remove(dependentScrollPane);
            confirmationAnchor.setMinHeight(emptyHeight);
            confirmationAnchor.setMaxHeight(emptyHeight);
            confirmationAnchor.setMinWidth(width);
            confirmationAnchor.setMaxWidth(width);
            confirmationLabel.setText("Update to " + packageToUpdate.getVersions().get(0).getVersion_number() + "?");
            confirmationAnchor.setLayoutX((parentWidth / 2) - width / 2);
            confirmationAnchor.setLayoutY((parentHeight / 2) - emptyHeight / 2);
        }


    }

    public Button getConfirmationButton(){
        return this.confirmationButton;
    }

    public Button getCancelButton(){
        return this.cancelButton;
    }

    public Button getUpdateAllButton(){
        return this.updateAllButton;
    }
}
