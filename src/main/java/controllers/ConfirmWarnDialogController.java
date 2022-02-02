package controllers;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
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

    private double emptyHeight = 180.0;
    private double fullHeight = 280;
    private double width = 440;
    private IntegerProperty buttonSelection = new SimpleIntegerProperty(); //-2 = resetting property -1 = cancel, 0 = middle option, 1 = confirm/confirm all/OK
    private AnchorPane parentAnchor;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        cancelButton.setMinWidth(100);
        confirmationButton.setMinWidth(100);
        confirmationAnchor.setMinHeight(fullHeight);
        confirmationAnchor.setMaxHeight(fullHeight);
        confirmationAnchor.setMinWidth(width);
        confirmationAnchor.setMaxWidth(width);
    }

    public void setParent(AnchorPane sceneAnchorPane){
        this.parentAnchor = sceneAnchorPane;
    }

    private void setBehaviorInParent(boolean expanded){
        double parentHeight = parentAnchor.getHeight();
        double parentWidth = parentAnchor.getWidth();

        if(expanded) {
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
            confirmationAnchor.setLayoutX((parentWidth / 2) - width / 2);
            confirmationAnchor.setLayoutY((parentHeight / 2) - emptyHeight / 2);
        }
    }

    public void showUninstallConfirmation(ModPackage packageToRemove, List<ModPackage> dependents) {
        dependents.remove(packageToRemove);
        dependentsBox.getChildren().clear();
        modNameLabel.setText(packageToRemove.getName());
        confirmationButton.setText("Uninstall");

        if (dependents.size() >= 1) {
            confirmationLabel.setText("Removing this mod will also remove");
            if(!confirmationAnchor.getChildren().contains(dependentScrollPane)){
                confirmationAnchor.getChildren().add(dependentScrollPane);
            }
            for (ModPackage dependent : dependents) {
                dependentsBox.getChildren().add(new Label(dependent.getName()));
            }
            dependentScrollPane.setVisible(true);
            setBehaviorInParent(true);
        } else {
            confirmationLabel.setText("Uninstall mod?");
            setBehaviorInParent(false);
        }
        dependents.add(packageToRemove);
    }

    public void showUpdateConfirmation(ModPackage packageToUpdate, List<ModPackage> allModsNeedingUpdate){
        allModsNeedingUpdate.remove(packageToUpdate);
        dependentsBox.getChildren().clear();
        confirmationButton.setText("Confirm");
        if(allModsNeedingUpdate.size() != 0) {
            modNameLabel.setText("All Mods");
            confirmationLabel.setText("Update all mods?");
            if(!confirmationAnchor.getChildren().contains(dependentScrollPane)){
                confirmationAnchor.getChildren().add(dependentScrollPane);
            }
            for (ModPackage dependency : allModsNeedingUpdate) {
                dependentsBox.getChildren().add(new Label(dependency.getName()));
            }
            dependentScrollPane.setVisible(true);
            setBehaviorInParent(true);
        }
        else{
            dependentScrollPane.setVisible(false);
            modNameLabel.setText(packageToUpdate.getName());
            confirmationLabel.setText("Update to " + packageToUpdate.getVersions().get(0).getVersion_number() + "?");
            setBehaviorInParent(false);
        }

        allModsNeedingUpdate.add(packageToUpdate);
    }

    public void showDownloadConfirmation(ModPackage packageToInstall, List<ModPackage> dependenciesToInstall){
        dependenciesToInstall.remove(packageToInstall);
        modNameLabel.setText(packageToInstall.getName());
        if(!dependenciesToInstall.isEmpty()){
            confirmationLabel.setText("Required dependencies: ");
            for(ModPackage dependency : dependenciesToInstall){
                dependentsBox.getChildren().add(new Label(dependency.getName()));
            }
            dependenciesToInstall.add(packageToInstall);
            setBehaviorInParent(true);
        }
        else{
            setBehaviorInParent(false);
            confirmationLabel.setText("Download mod?");
        }
        confirmationButton.setText("Confirm");
        dependenciesToInstall.add(packageToInstall);
    }

    public void onCancelClicked(){
        buttonSelection.set(-1);
    }

    public void onConfirmOKClicked(){
        buttonSelection.set(1);
    }

    public IntegerProperty selectionProperty(){
        return this.buttonSelection;
    }
}
