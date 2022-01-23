package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import mods.ModPackage;


import java.net.URL;
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

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void showUninstallConfirmation(ModPackage packageToRemove, List<ModPackage> dependents) {
        dependentsBox.getChildren().clear();
        modNameLabel.setText(packageToRemove.getName());

        if (!dependents.isEmpty()) {
            for (ModPackage dependent : dependents) {
                dependentsBox.getChildren().add(new Label(dependent.getName()));
            }
        } else {
            dependentsBox.getChildren().add(new Label("No Depending Mods."));
        }

    }

    public Button getConfirmationButton(){
        return this.confirmationButton;
    }

    public Button getCancelButton(){
        return this.cancelButton;
    }
}
