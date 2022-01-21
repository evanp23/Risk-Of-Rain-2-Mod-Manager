package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import mods.ModPackage;


import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

public class ModFullPageController implements Initializable {
    @FXML
    private Label modNameLabel;

    @FXML
    private Label modAuthorLabel;

    @FXML
    private ImageView modImage;

    @FXML
    private VBox modDescriptionVBox;

    private Label descriptionLabel;
    private Label latestVersionLabel;
    private Label lastUpdateLabel;
    private Label publishedLabel;
    private List<Label> labelList = new ArrayList<>();


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        descriptionLabel = new Label("Description\n");
        descriptionLabel.setAlignment(Pos.CENTER);
        labelList.add(descriptionLabel);

        latestVersionLabel = new Label("Latest Version\n");
        latestVersionLabel.setAlignment(Pos.CENTER);
        labelList.add(latestVersionLabel);

        publishedLabel = new Label("Date Published\n");
        publishedLabel.setAlignment(Pos.CENTER);
        labelList.add(publishedLabel);

        lastUpdateLabel = new Label("Date Updated\n");
        lastUpdateLabel.setAlignment(Pos.CENTER);
        labelList.add(lastUpdateLabel);

        DropShadow ds = new DropShadow(20, Color.GRAY);
        modImage.setEffect(ds);

    }

    public void setData(ModPackage modPackage) throws ParseException {
        List<Label> textList = new ArrayList<>();
        modNameLabel.setText(modPackage.getName());
        modAuthorLabel.setText(modPackage.getOwner());

        Image packageImage = new Image(modPackage.getVersions().get(0).getIcon());
        modImage.setImage(packageImage);

        List<String> finalDates = createDates(modPackage);

        Label description = new Label(modPackage.getVersions().get(0).getDescription());
        textList.add(description);
        Label version = new Label(modPackage.getVersions().get(0).getVersion_number());
        textList.add(version);
        Label created = new Label(finalDates.get(0) + " - " + finalDates.get(1));
        textList.add(created);
        Label updated = new Label(finalDates.get(2) + " - " + finalDates.get(3));
        textList.add(updated);



        modDescriptionVBox.getChildren().clear();


        for(int i = 0; i < textList.size(); i++){
            VBox insideBox = new VBox();

            labelList.get(i).setStyle("-fx-font-weight: bold");
            textList.get(i).setWrapText(true);
            insideBox.getChildren().add(labelList.get(i));
            insideBox.getChildren().add(textList.get(i));
            modDescriptionVBox.getChildren().add(insideBox);
        }



    }

    private List<String> createDates(ModPackage modPackage) throws ParseException {
        List<String> finalDates = new ArrayList<>();

        String unformattedDateCreated = modPackage.getDate_created();
        String unformattedDateUpdated = modPackage.getDate_updated();

        Scanner createdScanner = new Scanner(unformattedDateCreated);
        Scanner updatedScanner = new Scanner(unformattedDateUpdated);

        createdScanner.useDelimiter("T|Z");
        updatedScanner.useDelimiter("T|Z");

        String formattedDateCreated = createdScanner.next();
        String formattedTimeCreated = createdScanner.next();
        String formattedDateUpdated = updatedScanner.next();
        String formattedTimeUpdated = updatedScanner.next();

        DateFormat parserDate = new SimpleDateFormat("yyyy-mm-dd");
        DateFormat parserTime = new SimpleDateFormat("hh:mm:sssss");

        DateFormat dateFormatter = new SimpleDateFormat("MMM dd, yyyy");
        DateFormat timeFormatter = new SimpleDateFormat("hh:mm:ss");

        Date convertedDateCreated = parserDate.parse(formattedDateCreated);
        Date convertedTimeCreated = parserTime.parse(formattedTimeCreated);
        Date convertedDateUpdated = parserDate.parse(formattedDateUpdated);
        Date convertedTimeUpdated = parserTime.parse(formattedTimeUpdated);


        String finalDateCreated = dateFormatter.format(convertedDateCreated);
        String finalTimeCreated = timeFormatter.format(convertedTimeCreated);
        String finalDateUpdated = dateFormatter.format(convertedDateUpdated);
        String finalTimeUpdated = timeFormatter.format(convertedTimeUpdated);

        finalDates.add(finalDateCreated);
        finalDates.add(finalTimeCreated);
        finalDates.add(finalDateUpdated);
        finalDates.add(finalTimeUpdated);

        return finalDates;
    }
}
