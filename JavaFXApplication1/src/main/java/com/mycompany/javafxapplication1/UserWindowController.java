package com.mycompany.javafxapplication1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;


import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class UserWindowController {
    
    @FXML
    private Label filename;
    
    @FXML
    private Button saveFileBtn;

    @FXML
    private Button downloadBtn;
    
    @FXML
    private TextField userTextField;

    @FXML
    private TableView filesTableView;
    
    @FXML
    private TextField createTextField;
    
    @FXML
    private TextArea userTextArea;

    @FXML
    private Button secondaryButton;
    
    @FXML
    private Button refreshBtn;
    
    @FXML
    private Button uploadBtn;
    
    @FXML
    private Button deleteBtn;

    @FXML
    private Button shareBtn;

    @FXML
    private TextField shareRevokeTextField;

    @FXML
    private Button revokeBtn;

    @FXML
    private CheckBox readCheckBox;

    @FXML
    private CheckBox editCheckBox;


    @FXML
    private Button editFileBtn;

    private String directoryPath;
    
    
    @FXML
    private void recoverFilesBtn() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("recoverMyFiles.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            recoverWindowController controller = loader.getController();
            controller.initialise(userTextField.getText(), numOfContainers);

            secondaryStage.setTitle("Recover Files");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();
        log("switched to recover files window");
        obj.addToAuditTrail("switched to recover files window");
    }

    @FXML
    private void uploadFilebutton() throws JSchException, SftpException, IOException, SQLException, ClassNotFoundException {
        Stage primaryStage = (Stage) uploadBtn.getScene().getWindow();
        primaryStage.setTitle("Select a File");

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        File selectedFile = fileChooser.showOpenDialog(primaryStage);

        
        if(selectedFile!=null){
            DB obj = new DB();
            obj.username = userTextField.getText();
            obj.localAppPath = directoryPath;
            obj.userAclTableName  = "User"+userTextField.getText()+"ACL";


            new Thread(() -> {
                try {

                    boolean success = obj.uploadFile(selectedFile.toString());
                    obj.username = userTextField.getText();
                    log("uploaded chunks for "+selectedFile.getName());
                    obj.addToAuditTrail("uploaded chunks for "+selectedFile.getName());
                    if (success) {
                        Platform.runLater(() -> {
                            updateTableView();
                            dialogueSuccess("Success", "File uploaded successfully");
                        });
                    } else{
                        Platform.runLater(() -> {
                            dialogueError("Error", "File with this name already exists");
                        });
                    }
                } catch (JSchException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e);
                } catch (SftpException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e);
                } catch (SQLException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e);
                } catch (ClassNotFoundException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    throw new RuntimeException(e);
                }
            }).start();
        }
    }

    @FXML
    private void downloadBtnHandler() throws JSchException, SftpException, IOException, SQLException, ClassNotFoundException {
        Files.createDirectories(Paths.get(directoryPath));
        DB obj = new DB();
        obj.localAppPath = directoryPath;
        obj.username = userTextField.getText();
        obj.downloadFile(currentFile, currentOwner, "downloads");
        log("downloaded "+currentFile);
        obj.addToAuditTrail("downloaded "+currentFile);
        dialogueSuccess("Success", "File downloaded successfully");

    }


    @FXML
    private void editFileBtn() throws JSchException, SftpException, IOException, SQLException, ClassNotFoundException {
        Files.createDirectories(Paths.get(directoryPath));
        DB obj = new DB();
        obj.localAppPath = directoryPath;
        obj.username = userTextField.getText();
        String res = null;

        obj.downloadFile(currentFile, currentOwner, "tmp");

        File selectedFile = new File(directoryPath+currentFile);

        if (selectedFile != null) {
        try {
            filesTableView.setVisible(false);
            userTextArea.setVisible(true);
            refreshBtn.setVisible(false);
            saveFileBtn.visibleProperty().set(true);
            filename.setText(selectedFile.getName());
            
            //String command = "cat" + selectedFile;
            ProcessBuilder processBuilder = new ProcessBuilder("cat", selectedFile.getAbsolutePath());

            Process process = processBuilder.start();

            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                int exitCode = process.waitFor();

                StringBuilder output = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }

                 res = output.toString();

                if (exitCode == 0) {
                    log("Process terminated successfully.");
                    System.out.println("Process terminated successfully.");
                } else {
                    log("Error occurred. Exit code: " + exitCode);
                    System.err.println("Error occurred. Exit code: " + exitCode);
                    log("Error output:\n" + output.toString());
                    System.err.println("Error output:\n" + output.toString());
                }
            } catch (IOException | InterruptedException e) {
                log(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();
            }
        }
        log("downloaded "+currentFile);
        obj.addToAuditTrail("downloaded "+currentFile);
        //output result in textarea
        if (res!=null)
            userTextArea.appendText(res);

    }

    

    @FXML
    private void saveFileButton() throws JSchException, SftpException, IOException, SQLException, ClassNotFoundException {

        DB obj = new DB();
        obj.localAppPath = directoryPath;
        obj.username = userTextField.getText();

        FileWriter writer = null;


        try {
            writer = new FileWriter(directoryPath+currentFile);
            writer.write(userTextArea.getText());

        } catch (IOException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(UserWindowController.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                writer.close();
                //generateFileMetadata(directoryPath, theFileName);
                filename.setText("");
                userTextArea.clear();
                userTextArea.setVisible(false);
                saveFileBtn.setVisible(false);
                refreshBtn.setVisible(true);
                filesTableView.setVisible(true);

            } catch (IOException ex) {
                Logger.getLogger(UserWindowController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        obj.updateLastModifiedDateAndUploadFile(currentFile, currentOwner);
        log("edited "+currentFile);
        obj.addToAuditTrail("edited "+currentFile);
        log("uploaded "+currentFile);
        obj.addToAuditTrail("uploaded "+currentFile);

        dialogueSuccess("Success", "File edited successfully");

        File file = new File(directoryPath+currentFile);
        file.delete();
    }
    
    @FXML
    private void deleteFilebutton() throws JSchException, IOException, SQLException, ClassNotFoundException {
        //TODO: if fileOwner = userTextField.getText() then delete, else remove userTextField.getText() from the list of users who can access the file
        DB obj = new DB();
        obj.userAclTableName = "User"+userTextField.getText()+"ACL";
        obj.username = userTextField.getText();
        if (currentOwner.equals(userTextField.getText())){
            obj.moveFileToRecoveryAndDeleteFromFilesTable(currentFile);

            log("moved "+currentFile+" to recovery, deleted from Files and inserted into recoveryFiles");
            obj.addToAuditTrail("moved "+currentFile+" to recovery, deleted from Files and inserted into recoveryFiles");
            //obj.deleteFileFromMyAcl(currentFile, currentOwner);
        } else {
            obj.deleteFileFromMyAcl(currentFile, currentOwner);

            log("deleted "+currentFile+" from User"+userTextField.getText()+"ACL");
            obj.addToAuditTrail("deleted "+currentFile+" from User"+userTextField.getText()+"ACL");
        }
        dialogueSuccess("Success", "File deleted successfully");
        updateTableView();
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
    private void createFilebutton() throws JSchException, SftpException, IOException {
         String fileName = createTextField.getText();

            createTextField.clear();
            
        try {

            Files.createDirectories(Paths.get(directoryPath));

            Path filePath = Paths.get(directoryPath, fileName);
            if (!(Files.exists(filePath)) && filename != null){
                Files.createFile(filePath);
                //generateFileMetadata(directoryPath, fileName);
                DB obj = new DB();
                obj.userAclTableName  = "User"+userTextField.getText()+"ACL";
                obj.localAppPath = directoryPath;
                obj.username = userTextField.getText();
                obj.uploadFile(directoryPath+fileName);

                log("created "+fileName);
                log("uploaded "+fileName);
                obj.addToAuditTrail("created " + fileName);
                obj.addToAuditTrail("uploaded " + fileName);
                dialogueSuccess("Success", "File created successfully.");
            
            System.out.println("File created successfully.");
            } else {
                log("The file already exists or has no name");
                dialogueError("The file already exists or has no name","Please try again!");
                System.out.println("File with the same name already exists");
            }


            
        } catch (IOException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(UserWindowController.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
        updateTableView();

    }

    public void generateFileMetadata(String stogragePath, String fileName) throws IOException {
        Path file = Path.of(stogragePath+fileName);
        BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
        String dir = stogragePath+"metadata/";
        Files.createDirectories(Paths.get(dir));
        FileWriter myWriter = new FileWriter(dir + fileName+"_metadata.txt");
        myWriter.write("creationTime: " + attr.creationTime() + "\n");
        myWriter.write("lastAccessTime: " + attr.lastAccessTime() + "\n");
        myWriter.write("lastModifiedTime: " + attr.lastModifiedTime() + "\n");
        myWriter.write("isDirectory: " + attr.isDirectory() + "\n");
        myWriter.write("isOther: " + attr.isOther() + "\n");
        myWriter.write("isRegularFile: " + attr.isRegularFile() + "\n");
        myWriter.write("isSymbolicLink: " + attr.isSymbolicLink() + "\n");
        myWriter.write("size: " + attr.size() + "\n");
        myWriter.close();

    }
    
    //@FXML
//    private void RefreshBtnHandler(ActionEvent event){
//        Stage primaryStage = (Stage) customTextField.getScene().getWindow();
//        customTextField.setText((String)primaryStage.getUserData());
//    }

    //TODO: add recievedfiles controller. files and owners of files which were shared with current user will be shown there. implement acl from there
    @FXML
    private void switchToTerminal() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {


            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("terminal.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            terminalController controller = loader.getController();
            controller.initialise(userTextField.getText());

            secondaryStage.setTitle("Terminal");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();
        log("opened terminal");
        obj.addToAuditTrail("opened terminal");
    }

    @FXML
    private void switchToRemoteTerminal() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {


            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("remoteTerminal.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            remoteTerminalController controller = loader.getController();
            controller.initialise(userTextField.getText(), numOfContainers);

            secondaryStage.setTitle("Remote terminal");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();

        log("opened remote terminal");
        obj.addToAuditTrail("opened remote terminal");
    }
    
    @FXML
    private void switchToProfile() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) secondaryButton.getScene().getWindow();
        try {


            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("userProfile.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root, 640, 480);
            secondaryStage.setScene(scene);
            userProfileController controller = loader.getController();
                controller.initialise(userTextField.getText());
            secondaryStage.setTitle("Login");
            secondaryStage.show();
            primaryStage.close();

        } catch (Exception e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();

        log("opened user profile");
        obj.addToAuditTrail("opened user profile");
    }

    @FXML
    private void switchToPrimary() throws SQLException, ClassNotFoundException {
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
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        DB obj = new DB();
        obj.username = userTextField.getText();

        log("logged out");
        obj.addToAuditTrail("logged out");
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
    private int currentWrite ;
    private int currentRead ;
    private int numOfContainers = 3;

    @FXML
    private void filesTableViewHandler() throws SQLException, ClassNotFoundException {
        UserAcl selectedRow = (UserAcl) filesTableView.getSelectionModel().getSelectedItem();
        DB obj = new DB();
        obj.username = userTextField.getText();

        if (selectedRow != null) {
            String fileName = selectedRow.getFilename();
            System.out.println(fileName);

            log("selected "+fileName);
            obj.addToAuditTrail("selected "+fileName);
            currentFile = fileName;
            currentOwner = selectedRow.getOwner();
            currentWrite = selectedRow.getWritable();
            currentRead = selectedRow.getReadable();

            filename.setText("");

            if (numOfContainers == obj.getNumOfContainersForFile(currentFile, currentOwner) ) {
                deleteBtn.setDisable(false);
                if (currentWrite == 1)
                    editFileBtn.setDisable(false);
                else
                    editFileBtn.setDisable(true);

                if (currentRead == 1)
                    downloadBtn.setDisable(false);
                else
                    downloadBtn.setDisable(true);
            } else {
                deleteBtn.setDisable(true);
                    editFileBtn.setDisable(true);
                    downloadBtn.setDisable(true);
                    filename.setText("Active number of containers: " + numOfContainers+ ". This file was saved on "+obj.getNumOfContainersForFile(currentFile, currentOwner)+" containers.");
            }
            if (currentOwner.equals(userTextField.getText())) {
                shareBtn.setDisable(false);
                revokeBtn.setDisable(false);
                editCheckBox.setDisable(false);
                readCheckBox.setDisable(false);
                shareRevokeTextField.setDisable(false);
            } else {
                shareBtn.setDisable(true);
                revokeBtn.setDisable(true);
                editCheckBox.setDisable(true);
                readCheckBox.setDisable(true);
                shareRevokeTextField.setDisable(true);
            }
        }
        else{
            deleteBtn.setDisable(true);
            editFileBtn.setDisable(true);
            shareBtn.setDisable(true);
            revokeBtn.setDisable(true);
            editCheckBox.setDisable(true);
            readCheckBox.setDisable(true);
            shareRevokeTextField.setDisable(true);
            downloadBtn.setDisable(true);
        }
    }

    @FXML
    private void refreshBtnHandler() throws SQLException, ClassNotFoundException {
        updateTableView();
        DB obj = new DB();
        obj.username = userTextField.getText();

        log("refreshed table view");
        obj.addToAuditTrail("refreshed table view");
    }
    private void updateTableView(){

        DB obj = new DB();
        obj.userAclTableName  = "User"+userTextField.getText()+"ACL";
        ObservableList<UserAcl> data;

        try {
            data = obj.getDataFromACLTable();

            TableColumn<String, String> fileNameColumn = new TableColumn<>("Filename");
            fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("filename"));

            TableColumn<String , String> ownerColumn = new TableColumn<>("Owner");
            ownerColumn.setCellValueFactory(new PropertyValueFactory<>("owner"));

            TableColumn<Integer , Integer> readableColumn = new TableColumn<>("Readable");
            readableColumn.setCellValueFactory(new PropertyValueFactory<>("readable"));

            TableColumn<Integer, Integer> writableColumn = new TableColumn<>("Writable");
            writableColumn.setCellValueFactory(new PropertyValueFactory<>("writable"));



            filesTableView.setItems(data);
            filesTableView.getColumns().setAll(fileNameColumn, ownerColumn, readableColumn, writableColumn);
        } catch (ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(UserWindowController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    @FXML
    private void shareBtnHandler() throws SQLException, ClassNotFoundException {
        String usern = shareRevokeTextField.getText();
        DB obj = new DB();
        obj.addToOtherACL(usern, currentFile, currentOwner, readCheckBox.isSelected(), editCheckBox.isSelected() );

        obj.username = userTextField.getText();

        log("shared "+currentFile+" with "+shareRevokeTextField.getText());
        obj.addToAuditTrail("shared "+currentFile+" with "+shareRevokeTextField.getText());
    }

    @FXML
    private void revokeBtnHandler() throws SQLException, ClassNotFoundException {
        String usern = shareRevokeTextField.getText();
        DB obj = new DB();
        obj.revokeSomeoneFromFile(currentFile, usern, currentOwner);
        obj.username = userTextField.getText();

        log("revoked "+currentFile+" from "+shareRevokeTextField.getText());
        obj.addToAuditTrail("revoked "+currentFile+" from "+shareRevokeTextField.getText());

    }

    public void initialise(String credentials) throws ClassNotFoundException, IOException {
        
        userTextField.setText(credentials);
        directoryPath = "/home/ntu-user/App_"+userTextField.getText()+"/";
        DB obj = new DB();
        obj.localAppPath = directoryPath;
        obj.username = userTextField.getText();
        obj.userAclTableName  = "User"+userTextField.getText()+"ACL";
        obj.createAclTable();
        Files.createDirectories(Paths.get(directoryPath));
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
