package com.mycompany.javafxapplication1;
import javafx.scene.control.TextArea;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
//import org.freedesktop.gtk.GtkModule;

public class terminalController {

    @FXML
    private Button registerBtn;
    
    @FXML
    private TextArea textArea;
    
    @FXML
    private TextField userTextField;
    
    @FXML
    private void lsButton(){
        userTextField.appendText("ls");
    }
    
     @FXML
    private void mvButton(){
        userTextField.appendText("mv");
    }
    
     @FXML
    private void treeButton(){
        userTextField.appendText("tree");
    }
    
     @FXML
    private void whoamiButton(){
        userTextField.appendText("whoami");
    }
    
     @FXML
    private void cpButton(){
        userTextField.appendText("cp");
    }
    
     @FXML
    private void psButton(){
        userTextField.appendText("ps");
    }
    
     @FXML
    private void nanoButton(){
        userTextField.appendText("nano");
    }
    
     @FXML
    private void mkdirButton(){
        userTextField.appendText("mkdir");
    }

    @FXML
    private void textFieldExecute(){  // vulnurability: you can execute all kinds of commands through nano window. 
        
        ArrayList<String> allowedCommands = new ArrayList();
        allowedCommands.add("ls");
        allowedCommands.add("mkdir");
        allowedCommands.add("tree");
        allowedCommands.add("ps");
        allowedCommands.add("mv");
        allowedCommands.add("whoami");
        allowedCommands.add("cp");
        allowedCommands.add("nano");
        
        ArrayList<String> bashOperators = new ArrayList();
        bashOperators.add("&&"); bashOperators.add("||"); bashOperators.add("|");
                
        
        //get and display text in textarea
        String text = userTextField.getText();
        userTextField.clear();
        textArea.appendText("> "+text + "\n");
        
        //execute text in terminal
        boolean nanoException = false;
        if (text.split(" ")[0].equals("nano")){
            text = text.replace("nano", "terminator -e nano");
            
            nanoException = true;
        }
        
        ArrayList<String> tmp = new ArrayList();
        for (String word : text.split(" ")) {
            tmp.add(word);
        }
        String command = tmp.get(0);
        

        boolean state =false;

        if ((nanoException|| allowedCommands.contains(tmp.get(0)))) {
            state = true;
            for (int i = 0; i < tmp.size(); i++) {
                
                if ((bashOperators.contains(tmp.get(i)) )) {
                    if ((i + 1) < tmp.size()) {
                        i++;
                    }
                    if ((allowedCommands.contains(tmp.get(i)))) {
                         // "bash" "-c" "ls && tree"
                        state = true;
                       
                        tmp.set(i, tmp.get(i-2)+" "+tmp.get(i-1)+" "+tmp.get(i));
                        tmp.set(i-1, "-c");
                        tmp.set(i-2, "bash");
                       

                    } else if (tmp.get(i).equals("terminator")) {
                        if ((tmp.get(i + 1).equals("-e")) && (tmp.get(i + 2).equals("nano"))) {
                            
                            state = true;
                            if ((i + 3 < tmp.size())&& !(bashOperators.contains(tmp.get(i+3))) && !(allowedCommands.contains(tmp.get(i+3)))){
                                tmp.set(i+2, "nano "+tmp.get(i+3));
                                tmp.remove(i+3);
                            }
                            
                        }
                    } else {
                        command = tmp.get(i);
                        state = false;
                        break;
                    }
                }
                else if (tmp.get(i).equals("terminator")) {
                        if ((tmp.get(i + 1).equals("-e")) && (tmp.get(i + 2).equals("nano"))) {
                            
                            state = true;
                            if ((i + 3 < tmp.size())&& !(bashOperators.contains(tmp.get(i+3))) && !(allowedCommands.contains(tmp.get(i+3)))){
                                tmp.set(i+2, "nano "+tmp.get(i+3));
                                tmp.remove(i+3);
                            }
                            
                        }
                }
                
                else if ((tmp.get(i).charAt(0) == '$')&&(tmp.get(i+1).charAt(1) == '(')){ // you can write a command in &(somethign) and execute it. it is hard to check so it is restricted
                    command = tmp.get(i);
                    state = false;
                    break;
                }

            }
        }
        
        String res = null;
        if (state == true) {

            try {
                //GtkModule.load("appmenu-gtk-module");

                ProcessBuilder processBuilder = new ProcessBuilder(tmp);
                if (!nanoException)
                processBuilder.redirectErrorStream(true);

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
                    System.out.println("Process terminated successfully.");
                    DB obj = new DB();
                    obj.username = username;

                    log("executed command" + command);
                    obj.addToAuditTrail("executed command" + command);
                } else {
                    System.err.println("Error occurred. Exit code: " + exitCode);
                    System.err.println("Error output:\n" + output.toString());
                    textArea.appendText("Error occurred. Exit code: " + exitCode + "\n");
                    textArea.appendText("Error output:\n");
                }
            } catch (IOException | InterruptedException e) {
                log("Error: " + e.getMessage());
                e.printStackTrace();
            } catch (SQLException e) {
                log("Error: " + e.getMessage());
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                log("Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        } else {
            log("Error: Either the command " + command + " is not in the list of commands or you tried to do something illegal");
            textArea.appendText("Error: Either the command " + command + " is not in the list of commands or you tried to do something illegal\n");
        }
        //output result in textarea
        if (res!=null)
        textArea.appendText(res + "\n");
        
    }

    @FXML
    private void switchToPrimary() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) registerBtn.getScene().getWindow();
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
    private String username = null;
    public void initialise(String userName){
        username = userName;
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


    
   

    
  
