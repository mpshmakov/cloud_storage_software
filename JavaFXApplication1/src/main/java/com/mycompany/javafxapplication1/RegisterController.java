package com.mycompany.javafxapplication1;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author ntu-user
 */
public class RegisterController {

    /**
     * Initializes the controller class.
     */
    @FXML
    private Button registerBtn;

    @FXML
    private Button backLoginBtn;

    @FXML
    private PasswordField passPasswordField;

    @FXML
    private PasswordField rePassPasswordField;

    @FXML
    private TextField userTextField;

    @FXML
    private Text fileText;

    @FXML
    private Button selectBtn;



    private void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);
        Optional<ButtonType> result = alert.showAndWait();
    }

    @FXML
    private void registerBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            DB myObj = new DB();
            if (passPasswordField.getText().equals(rePassPasswordField.getText())) {
                if (!(myObj.checkIfUserExists(userTextField.getText()))) { // should have just created one boolean which would be 0 if one of these conditions fails. would be cleaner
                    myObj.addDataToDB(userTextField.getText(), passPasswordField.getText());
                    myObj.username = userTextField.getText();

                    log("registered" + userTextField.getText());
                    myObj.addToAuditTrail("registered");
                    dialogue("Adding information to the database", "Successful!");

                    String[] credentials = {userTextField.getText(), passPasswordField.getText()};
                    loader.setLocation(getClass().getResource("userWindow.fxml"));

                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    UserWindowController controller = loader.getController();
                    controller.initialise(credentials[0]);
                    secondaryStage.setScene(scene);

                    secondaryStage.setTitle("Your Disk");

                    String msg = "some data sent from Register Controller";
                    secondaryStage.setUserData(msg);

                }
                else {
                    dialogue("Adding information to the database", "This username is already taken. Try another one.");
                    loader.setLocation(getClass().getResource("register.fxml"));
                    Parent root = loader.load();
                    Scene scene = new Scene(root, 640, 480);
                    secondaryStage.setScene(scene);
                    secondaryStage.setTitle("Register a new User");
                }
            } else {
                dialogue("Adding information to the database", "The passwords do not match. Try again.");
                loader.setLocation(getClass().getResource("register.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root, 640, 480);
                secondaryStage.setScene(scene);
                secondaryStage.setTitle("Register a new User");
            }
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    @FXML
    private void backLoginBtnHandler(ActionEvent event) {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) backLoginBtn.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("primary.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
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