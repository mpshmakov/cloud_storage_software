package com.mycompany.javafxapplication1;

import com.jcraft.jsch.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
//import org.freedesktop.gtk.GtkModule;

public class remoteTerminalController {

    @FXML
    private Button goBackBtn;

    @FXML
    private Button container1Btn;

    @FXML
    private Button container2Btn;

    @FXML
    private Button container3Btn;

    @FXML
    private Button container4Btn;
    
    @FXML
    private TextArea textArea;
    
    @FXML
    private TextField userTextField;

    private String username;

    @FXML
    private Label label;

    private int currentContainer = 1;
    
    private String labelText = "current container";

    private String[] containersTextAreaText = {" ", " ", " ", " "};


    @FXML
    private void textFieldExecute() throws JSchException, IOException, SQLException, ClassNotFoundException {
        //executeExecCommands();
        enableShellCommands();
        updateTextAreaText();
        userTextField.clear();
    }

    @FXML
    private void setContainerTo1() throws SQLException, ClassNotFoundException {
        currentContainer = 1;
        label.setText(labelText+" - "+currentContainer);
        updateTextAreaText();
        DB obj = new DB();
        obj.username = username;

        log("clicked on container "+1);
        obj.addToAuditTrail("clicked on container "+1);
    }

    @FXML
    private void setContainerTo2() throws SQLException, ClassNotFoundException {
        currentContainer = 2;
        label.setText(labelText+" - "+currentContainer);
        updateTextAreaText();
        DB obj = new DB();
        obj.username = username;

        log("clicked on container "+2);
        obj.addToAuditTrail("clicked on container "+2);
    }

    @FXML
    private void setContainerTo3() throws SQLException, ClassNotFoundException {
        currentContainer = 3;
        label.setText(labelText+" - "+currentContainer);
        updateTextAreaText();
        DB obj = new DB();
        obj.username = username;

        log("clicked on container "+3);
        obj.addToAuditTrail("clicked on container "+3);
    }

    @FXML
    private void setContainerTo4() throws SQLException, ClassNotFoundException {
        currentContainer = 4;
        label.setText(labelText+" - "+currentContainer);
        updateTextAreaText();
        DB obj = new DB();
        obj.username = username;
        obj.addToAuditTrail("clicked on container "+4);
    }

    @FXML
    private void switchToUserWindow() throws SQLException, ClassNotFoundException {
        Stage secondaryStage = new Stage();
        Stage primaryStage = (Stage) goBackBtn.getScene().getWindow();
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

    private void updateTextAreaText(){
        //accumulate all outputs in String container1out
        //change textarea text every time the function is called to another container's text
        textArea.setText(containersTextAreaText[currentContainer-1]);

    }

    private String[] remoteHost = {"172.19.0.3", "172.19.0.6", "172.19.0.4", "172.19.0.2"};
    private String root = "root";
    private String password = "ntu-user";

    private ChannelExec setupExecJsch(int containerNum) throws JSchException {
        JSch jsch = new JSch();
        //jsch.setKnownHosts("/Users/john/.ssh/known_hosts");
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session jschSession = jsch.getSession(root, remoteHost[containerNum]);
        jschSession.setConfig(config);
        jschSession.setPassword(password);
        jschSession.connect();
        System.out.println("connected exec to " + jschSession.getHost());
        log("connected exec to " + jschSession.getHost());
        containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat("connected exec to " + jschSession.getHost()+"\n");
        return (ChannelExec) jschSession.openChannel("exec");
    }

    private void executeExecCommands() throws JSchException, IOException {
        ChannelExec channelExec = setupExecJsch(currentContainer-1);
        channelExec.setCommand(userTextField.getText()); //sending command
        containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat("> "+userTextField.getText()+"\n");
        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);

        InputStream in=channelExec.getInputStream(); //get output
        channelExec.connect();
        byte[] tmp=new byte[1024];
        while(true){
            while(in.available()>0){
                int i=in.read(tmp, 0, 1024);
                if(i<0)break;
                System.out.print(new String(tmp, 0, i));
                containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat(new String(tmp, 0, i));
                //textArea.appendText(new String(tmp, 0, i));

            }
            containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat("\n");
            if(channelExec.isClosed()){
                System.out.println("exit-status: "+channelExec.getExitStatus());
                //containersTextAreaText[currentContainer-1].concat("exit-status: "+channelExec.getExitStatus()+"\n");
                //textArea.appendText("exit-status: "+channelExec.getExitStatus());
                break;
            }
            try{Thread.sleep(1000);}
            catch(Exception e){e.printStackTrace();}
        }
        channelExec.disconnect();
        channelExec.getSession().disconnect();
        containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat("closed exec connection\n");
        System.out.println("closed exec connection");
        log("closed exec connection");
    }
    private ChannelShell setupShellJsch(int containerNum) throws JSchException {
        JSch jsch = new JSch();
        //jsch.setKnownHosts("/Users/john/.ssh/known_hosts");
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session jschSession = jsch.getSession(root, remoteHost[containerNum]);
        jschSession.setConfig(config);
        jschSession.setPassword(password);
        jschSession.connect();
        log("connected shell to " + jschSession.getHost());
        System.out.println("connected shell to " + jschSession.getHost());
        //containersTextAreaText[currentContainer-1] = containersTextAreaText[currentContainer-1].concat("connected exec to " + jschSession.getHost()+"\n");
        return (ChannelShell) jschSession.openChannel("shell");
    }

    private void enableShellCommands() throws JSchException, IOException, SQLException, ClassNotFoundException {
        ChannelShell channelShell = setupShellJsch(currentContainer-1);

        channelShell.setInputStream(System.in);
        //channelShell.setErrStream(System.err);

        InputStream in=channelShell.getInputStream(); //get output
        channelShell.connect();
        DB obj = new DB();
        obj.username = username;

        log("connected to container "+ currentContainer);
        obj.addToAuditTrail("connected to container "+ currentContainer);
        byte[] tmp=new byte[1024];
        while(true){
            while(in.available()>0){
                int i=in.read(tmp, 0, 1024);
                if(i<0)
                    break;
                System.out.print(new String(tmp, 0, i));
            }

            if(channelShell.isClosed()){
                System.out.println("exit-status: "+channelShell.getExitStatus());
                //containersTextAreaText[currentContainer-1].concat("exit-status: "+channelExec.getExitStatus()+"\n");
                //textArea.appendText("exit-status: "+channelExec.getExitStatus());
                break;
            }
            try{Thread.sleep(1000);}
            catch(Exception e){
                log(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();}
        }
        in.close();
        channelShell.disconnect();
        channelShell.getSession().disconnect();

        log("disconnected from container "+ currentContainer);
        obj.addToAuditTrail("disconnected from container "+ currentContainer);
        log("closed shell connection");
        System.out.println("closed exec connection");
    }
    private int numOfContainers = 0;
    public void initialise(String userName, int containers){
        username = userName;
        numOfContainers = containers;
        if (numOfContainers >=1){
            container1Btn.setDisable(false);
        }
        if (numOfContainers >=2){
            container2Btn.setDisable(false);
        }
        if (numOfContainers >=3){
            container3Btn.setDisable(false);
        }
        if (numOfContainers ==4){
            container4Btn.setDisable(false);
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


    
   

    
  
