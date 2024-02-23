package com.mycompany.javafxapplication1;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


public class userProfileController {
    @FXML
    private PasswordField passPasswordField;

    @FXML
    private TextField newPasswordField;


    @FXML
    private TextField userTextField;


    @FXML
    private Button secondaryButton;


    @FXML
    private void switchToUserWindow() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {


            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("userWindow.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            UserWindowController controller = loader.getController();
                controller.initialise(userTextField.getText());
            secondaryStage.setTitle("Your Disk");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();

        log("opened main Disk window");
        obj.addToAuditTrail("opened main Disk window");
    }


    @FXML
    private void deleteBtnHandler(ActionEvent event) throws InvalidKeySpecException, SQLException, ClassNotFoundException {
        DB myObj = new DB();
        if (dialogueTwoButtons("Confirmation window", "Press 'OK' to confirm the operation") == true) {
            myObj.deletePassword(userTextField.getText());
            myObj.username = userTextField.getText();

            log("deleted password");
            myObj.addToAuditTrail("deleted password");
        }

    }
    @FXML
    private void updateBtnHandler(ActionEvent event) throws InvalidKeySpecException, SQLException, ClassNotFoundException {

        //updating the passowrd
        DB myObj = new DB();
        String pass = newPasswordField.getText();

        if (dialogueTwoButtons("Confirmation window", "Press 'OK' to confirm the operation") == true){
            myObj.updatePassword(userTextField.getText(), pass);
            myObj.username = userTextField.getText();

            log("updated password");
            myObj.addToAuditTrail("updated password");
        }

    }


    public void dialogueSuccess(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Success Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);

        Optional<ButtonType> result = alert.showAndWait();
    }

    public void dialogueError(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);

        Optional<ButtonType> result = alert.showAndWait();
    }

    public boolean dialogueTwoButtons(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);


        alert.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Handle the OK button click
            return true;

        } else {
            // Handle the Cancel button click or if the dialog is closed without clicking a button
            return false;
        }
    }
    
    public void initialise(String userName){
        userTextField.setText(userName);
    }
    private void log(String event){

        try {
            Files.createDirectories(Paths.get("/home/ntu-user/App/"));
            File file = new File("/home/ntu-user/App/logs.txt");
            if (!file.exists()){
                file.createNewFile();
            }
            FileWriter fr = new FileWriter(file, true);

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formatDateTime = now.format(formatter);

            fr.write(formatDateTime+": "+ event + "\n");
            fr.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
    }
}