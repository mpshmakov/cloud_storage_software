package com.mycompany.javafxapplication1;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


public class recoverWindowController {
    
    @FXML
    private Label filename;
    
    @FXML
    private Button saveFileBtn;



    @FXML
    private TableView filesTableView;

    @FXML
    private TextArea userTextArea;

    @FXML
    private Button secondaryButton;

    @FXML
    private Button editFileBtn;

    @FXML
    private Button recoverBtn;

    private String directoryPath;
    
    
    @FXML
    private void recoverBtnHandler() throws JSchException, IOException, ClassNotFoundException, SQLException {
        DB obj = new DB();
        obj.userAclTableName = "User"+username+"ACL";
        obj.username = username;
        obj.recoverFiles(currentFile);
        dialogueSuccess("Success", "File recovered successfully");
        updateTableView();

        log("recovered "+currentFile);
        obj.addToAuditTrail("recovered "+currentFile);

    }
    

    private void dialogue(String headerMsg, String contentMsg) {
        Stage secondaryStage = new Stage();
        Group root = new Group();
        Scene scene = new Scene(root, 300, 300, Color.DARKGRAY);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerMsg);
        alert.setContentText(contentMsg);

        Optional<ButtonType> result = alert.showAndWait();
    }


    @FXML
    private void switchToPrimary() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        //DB myObj = new DB();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("userWindow.fxml"));
            Parent root = loader.load();
            UserWindowController controller = loader.getController();
            controller.initialise(username);
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            secondaryStage.setTitle("Your Disk");
            secondaryStage.show();
            primaryStage.close();
        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = username;

        log("opened main Disk window");
        obj.addToAuditTrail("opened main Disk window");
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

    private String currentFile = null;
    private String currentOwner = null;


    @FXML
    private void filesTableViewHandler() throws SQLException, ClassNotFoundException {
        recovery selectedRow = (recovery) filesTableView.getSelectionModel().getSelectedItem();
        DB obj = new DB();

        if (selectedRow != null) {
            String fileName = selectedRow.getFilename();
            System.out.println(fileName);

            obj.username = username;

            log("selected "+fileName);
            obj.addToAuditTrail("selected "+fileName);
            currentFile = fileName;
            currentOwner = selectedRow.getOwner();
            filename.setText("");
            if (numOfContainers == obj.getNumOfContainersForRecoveryFile(currentFile, currentOwner) ) {

                recoverBtn.setDisable(false);
            } else {
                filename.setText("Active number of containers: " + numOfContainers + ". This file was saved on " + obj.getNumOfContainersForRecoveryFile(currentFile, currentOwner) + " containers.");
                recoverBtn.setDisable(true);
            }

        } else{
            editFileBtn.setDisable(true);
            recoverBtn.setDisable(true);
        }
    }


    private void updateTableView(){

        DB obj = new DB();
        obj.username = username;
        ObservableList<recovery> data;

        try {
            data = obj.getDataFromRecoveryTable();

            TableColumn<String, String> fileNameColumn = new TableColumn<>("Filename");
            fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));

            TableColumn<String , String> ownerColumn = new TableColumn<>("Owner");
            ownerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));

            TableColumn<String, String> dateColumn = new TableColumn<>("Date Deleted");
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("deletedDate"));

            filesTableView.setItems(data);
            filesTableView.getColumns().setAll(fileNameColumn, ownerColumn, dateColumn);
        } catch (ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(recoverWindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String username = null;
    private int numOfContainers = 0;
    public void initialise(String credentials, int nofcontainers) throws ClassNotFoundException, IOException {
        
        username = credentials;
        directoryPath = "/home/ntu-user/App_"+username+"/";
        numOfContainers = nofcontainers;
        //DB obj = new DB();
//        obj.localAppPath = directoryPath;
//        obj.username = username;
        updateTableView();

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
