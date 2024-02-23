package com.mycompany.javafxapplication1;
import javafx.scene.control.PasswordField;
import java.security.spec.InvalidKeySpecException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Stage;



public class SecondaryController {
    @FXML
    private PasswordField passPasswordField;

    @FXML
    private TextField newPasswordField;

    String thePassword;





    @FXML
    private TextField deleteUserField;

    @FXML
    private TextField userTextField;

    @FXML
    private TableView dataTableView;

    @FXML
    private Button secondaryButton;

    @FXML
    private Button refreshBtn;

    @FXML
    private TextField customTextField;

    public void setThePassword(String pass){
        thePassword = pass;
    }

    @FXML
    private void RefreshBtnHandler(ActionEvent event){
        Stage primaryStage = (Stage) customTextField.getScene().getWindow();

        String[] credentials = {userTextField.getText(), passPasswordField.getText()};

        customTextField.setText((String)primaryStage.getUserData());
    }

    private void RefreshHandler(String password){
        Stage primaryStage = (Stage) customTextField.getScene().getWindow();

        String[] credentials = {userTextField.getText(), password};
        initialise(credentials);

        customTextField.setText((String)primaryStage.getUserData());
    }

    @FXML
    private void switchToPrimary(){
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
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
            e.printStackTrace();
        }
    }

    public void initialise(String[] credentials) {
        userTextField.setText(credentials[0]);
        DB myObj = new DB();
        ObservableList<User> data;
        try {
            data = myObj.getDataFromTable();
            TableColumn user = new TableColumn("User");
            user.setCellValueFactory(
                    new PropertyValueFactory<>("user"));

            TableColumn pass = new TableColumn("Pass");
            pass.setCellValueFactory(
                    new PropertyValueFactory<>("pass"));
            dataTableView.setItems(data);
            dataTableView.getColumns().addAll(user, pass);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(SecondaryController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    @FXML
    private void updateBtnHandler(ActionEvent event) throws InvalidKeySpecException, ClassNotFoundException {

        //updating the passowrd
        DB myObj = new DB();
        String pass = newPasswordField.getText();

        myObj.updatePassword(userTextField.getText(), pass);
        RefreshHandler(pass);

    }

    @FXML
    private void deleteUserBtnHandler(ActionEvent event) {
        DB myObj = new DB();

        myObj.deleteUser(deleteUserField.getText());
        RefreshHandler(thePassword);
        deleteUserField.clear();


    }


    public void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);

        Optional<ButtonType> result = alert.showAndWait();
    }

}