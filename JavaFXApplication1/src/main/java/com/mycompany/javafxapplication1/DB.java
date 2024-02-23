package com.mycompany.javafxapplication1;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;

import com.jcraft.jsch.*;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.ZipException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author ntu-user
 */
public class DB {
    private String dbFileName = "jdbc:sqlite:/home/ntu-user/App/comp20081.db";
    private int timeout = 30;
    private String dataBaseName = "COMP20081";

    public String username = null;
    public String userAclTableName;
    Connection connection = null;
    private Random random = new SecureRandom();
    private String characters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private int iterations = 10000;
    private int keylength = 256;
    private String saltValue;
    public String localAppPath = null;
    private String downloadPath = "/home/ntu-user/Downloads/";
    private String remoteDir = "/home/app/"; //storage containers' dir
    private int numOfContainers = 3; // max 8 (for convenience and my laptop's performance reasons)

    //TODO: count how many chunks for the file in the Files table is not null, and store in int chunksForThisFile; then use this variable to download!!! files
    //TODO: uploading should user numOfContainers, because it is the current number of containers in use and thus is more optimal

    /**
     * @brief constructor - generates the salt if it doesn't exist or load it from the file .salt
     */
    DB() {

        try {
            File fp = new File(".salt");
            if (!fp.exists()) {
                saltValue = this.getSaltvalue(30);
                FileWriter myWriter = new FileWriter(fp);
                myWriter.write(saltValue);
                myWriter.close();
            } else {
                Scanner myReader = new Scanner(fp);
                while (myReader.hasNextLine()) {
                    saltValue = myReader.nextLine();

                }
            }
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
    }

    /**
     * @brief create a new table
     */
    public void createTables() throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.execute("PRAGMA foreign_keys = ON");
            statement.executeUpdate("create table if not exists Users (name string primary key, password string, encryptionKey string)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS Files (filename STRING, checksum INTEGER, owner STRING REFERENCES Users(name), createdDate DATE, lastModified DATE, lastAccessTime DATE, isDir int, isFile int, isSymLink int, isOther int, filesize INTEGER, numOfContainers integer, firstChunk STRING, secondChunk STRING, thirdChunk STRING, fourthChunk STRING, primary key(filename, owner))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS recoveryFiles (filename STRING, checksum INTEGER, owner STRING REFERENCES Users(name), createdDate DATE, lastModified DATE, deletedDate DATE, lastAccessTime DATE, isDir int, isFile int, isSymLink int, isOther int, filesize INTEGER, numOfContainers integer, firstChunk STRING, secondChunk STRING, thirdChunk STRING, fourthChunk STRING, primary key(filename, owner))");
            statement.executeUpdate("create table if not exists auditTrail (user string REFERENCES Users(name), time DATE, event string)");

        } catch (SQLException ex) {
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                log(Arrays.toString(e.getStackTrace()));
                System.err.println(e.getMessage());
            }
        }
    }

    public void createAclTable() throws ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS " + userAclTableName + " (filename string, owner string, readable int, writable int, FOREIGN KEY(filename, owner) REFERENCES Files(filename, owner) ON DELETE CASCADE)");

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                log(Arrays.toString(e.getStackTrace()));
                System.err.println(e.getMessage());
            }
        }
    }
    public void addToAuditTrail(String event) throws SQLException, ClassNotFoundException {
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            //statement.execute("PRAGMA foreign_keys = ON");
            statement.executeUpdate("insert into auditTrail values ('"+username+"', datetime('now'), '"+event+"')");

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }


    /**
     * @param tableName of type String
     * @brief delete table
     */
    public void delTable(String tableName) throws ClassNotFoundException { // why is it even here?
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.executeUpdate("drop table if exists " + tableName);
        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                // connection close failed.
                log(Arrays.toString(e.getStackTrace()));
                System.err.println(e.getMessage());
            }
        }
    }

    /**
     * @param user     name of type String
     * @param password of type String
     * @brief add data to the database method
     */
    public void addDataToDB(String user, String password) throws InvalidKeySpecException, ClassNotFoundException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            String secPass = generateSecurePassword(password);
            String rand = String.valueOf(random.nextInt(100));
            String key = generateSecurePassword(user + rand);
            statement.executeUpdate("insert into Users values('" + user + "','" + secPass + "','"+key+"')");
            System.out.println("user " + user + ", password " + secPass + ", key "+key+ " added to the database");
            log("user " + user + ", password " + secPass + ", key "+key+ " added to the database");
            //addToAuditTrail("User "+user+" added to the database", connection);

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                log(Arrays.toString(ex.getStackTrace()));
                Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public boolean checkIfUserExists(String user) {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            int count = 1;
            ResultSet rs = statement.executeQuery("select count(*) as count from Users where name = '" + user + "'");
            if (rs.next()) {
                count = rs.getInt("count");
            }
            if (count == 0)
                return false;
            else
                return true;


        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException ex) {
                log(Arrays.toString(ex.getStackTrace()));
                Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return true;
    }


    public String getUserAclTableName() {
        return userAclTableName;
    }

    /**
     * @brief get data from the Database method
     * @retunr results as ResultSet
     */
    public ObservableList<User> getDataFromTable() throws ClassNotFoundException {
        ObservableList<User> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            ResultSet rs = statement.executeQuery("select * from Users");
            while (rs.next()) {
                // read the result set
                result.add(new User(rs.getString("name"), rs.getString("password")));
            }

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return result;
    }

    public ObservableList<UserAcl> getDataFromACLTable() throws ClassNotFoundException {
        ObservableList<UserAcl> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            ResultSet rs = statement.executeQuery("select * from " + userAclTableName);
            while (rs.next()) {
                // read the result set
                result.add(new UserAcl(rs.getString("filename"), rs.getString("owner"), rs.getInt("readable"), rs.getInt("writable")));
            }

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return result;
    }

    public ObservableList<recovery> getDataFromRecoveryTable() throws ClassNotFoundException { // for recover my files window
        ObservableList<recovery> result = FXCollections.observableArrayList();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            ResultSet rs = statement.executeQuery("select * from recoveryFiles where owner = '" +username +"'" );
            while (rs.next()) {
                // read the result set
                result.add(new recovery(rs.getString("filename"), rs.getString("owner"), rs.getDate("deletedDate")));
            }

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return result;
    }

    /**
     * @param user name as type String
     * @param pass plain password of type String
     * @return true if the credentials are valid, otherwise false
     * @brief decode password method
     */
    public boolean validateUser(String user, String pass) throws InvalidKeySpecException, ClassNotFoundException {
        Boolean flag = false;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select name, password from Users");
            String inPass = generateSecurePassword(pass);
            // Let's iterate through the java ResultSet
            while (rs.next()) {
                if (user.equals(rs.getString("name")) && rs.getString("password").equals(inPass)) {
                    flag = true;
                    break;
                }
            }
        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

        return flag;
    }

    private String getSaltvalue(int length) {
        StringBuilder finalval = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            finalval.append(characters.charAt(random.nextInt(characters.length())));
        }


        return new String(finalval);
    }

    /* Method to generate the hash value */
    private byte[] hash(char[] password, byte[] salt) throws InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keylength);
        Arrays.fill(password, Character.MIN_VALUE);
        try {
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new AssertionError("Error while hashing a password: " + e.getMessage(), e);
        } finally {
            spec.clearPassword();
        }
    }

    public String generateSecurePassword(String password) throws InvalidKeySpecException {
        String finalval = null;

        byte[] securePassword = hash(password.toCharArray(), saltValue.getBytes());

        finalval = Base64.getEncoder().encodeToString(securePassword);

        return finalval;
    }

    /**
     * @brief get table name
     * @return table name as String
     */


    /**
     * @param message of type String
     * @brief print a message on screen method
     */


    // server part
    //TODO: every new session make sure the ips are the same as here with docker inspect tmp_comp20081_network
    private String[] remoteHost = {"172.19.0.3", "172.19.0.6", "172.19.0.4", "172.19.0.2"};
    private String root = "root";
    private String password = "ntu-user";

    private ChannelSftp setupJsch(int containerNum) throws JSchException, SQLException, ClassNotFoundException {
        JSch jsch = new JSch();
        //jsch.setKnownHosts("/Users/john/.ssh/known_hosts");
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        Session jschSession = jsch.getSession(root, remoteHost[containerNum]);
        jschSession.setConfig(config);
        jschSession.setPassword(password);
        jschSession.connect();
        System.out.println("connected to " + jschSession.getHost());

        log("connected to " + jschSession.getHost());
        ////addToAuditTrail("connected to " + jschSession.getHost());
        return (ChannelSftp) jschSession.openChannel("sftp");
    }

    private ChannelExec setupExecJsch(int containerNum) throws JSchException, SQLException, ClassNotFoundException {
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
        //addToAuditTrail("connected exec to " + jschSession.getHost());

        return (ChannelExec) jschSession.openChannel("exec");
    }

    private void executeCommands(int j, String command) throws JSchException, IOException, SQLException, ClassNotFoundException {
        ChannelExec channelExec = setupExecJsch(j);
        channelExec.setCommand(command); //sending command

        channelExec.setInputStream(null);
        channelExec.setErrStream(System.err);

        InputStream in=channelExec.getInputStream(); //get output
        channelExec.connect();
        byte[] tmp=new byte[1024];
        while(true){
            while(in.available()>0){
                int i = in.read(tmp, 0, 1024);
                if(i < 0)break;
                System.out.print(new String(tmp, 0, i));
            }

            if(channelExec.isClosed()){
                System.out.println("exit-status: "+channelExec.getExitStatus());
                log("exit-status: "+channelExec.getExitStatus());
                break;
            }
            try{Thread.sleep(1000);}
            catch(Exception e){
                log(Arrays.toString(e.getStackTrace()));
                e.printStackTrace();}
        }
        channelExec.disconnect();
        channelExec.getSession().disconnect();

        System.out.println("closed exec connection");
        log("closed exec connection");

    }

    public boolean uploadFile(String localFile) throws JSchException, SftpException, ZipException, IOException, SQLException, ClassNotFoundException {

        boolean success = true;

        File file = new File(localFile);
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        boolean d = attr.isDirectory();
        int isDir = 0;
        if (d)
            isDir = 1;

        boolean f = attr.isRegularFile();
        int isFile = 0;
        if (f)
            isFile = 1;

        boolean sl = attr.isSymbolicLink();
        int isSymLink = 0;
        if (sl)
            isSymLink = 1;

        boolean o = attr.isOther();
        int isOther = 0;
        if(o)
            isOther = 1;

        long initSize = attr.size();

        String lFile = file.getName();
        byte[] bytes = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
        long crc32 = getCRC32Checksum(bytes);

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            int count = 0;
            ResultSet rs =  statement.executeQuery("select count(*) as count from Files where filename = '"+lFile+"' and owner = '"+username+"'");
            while (rs.next()){
                count = rs.getInt("count");
            }
            if (count <1) { // if file doesn't already exist

                File zippedFile = new File(zipAFile(localFile));

                int chunkSize = getSizeInBytes(zippedFile.length());

                List<File> chunks = splitBySize(zippedFile, chunkSize); //chunking


                String[] chunkName = new String[4]; // if an element isn't initialized with a chunk's name, it is null, which is good


                for (int i = 0; i < chunks.size(); i++) {
                    ChannelSftp channelSftp = setupJsch(i);
                    channelSftp.connect();

                    File chunk = chunks.get(i);
                    File renamedChunk = new File(localAppPath + username + "_" + lFile + "_Chunk_" + (i + 1) + ".zip");
                    if (chunk.renameTo(renamedChunk)) {
                        System.out.println("success renaming");
                        log("success renaming");
                    }
                    else {
                        System.out.println("fail renaming");
                        log("fail renaming");
                    }

                    chunkName[i] = renamedChunk.getName();
                    System.out.println(chunkName[i]);
                    

                    channelSftp.put(renamedChunk.getAbsolutePath(), remoteDir + renamedChunk.getName());
                    System.out.println("uploaded chunk " + (i + 1));
                    log("uploaded chunk " + (i + 1));
                    //addToAuditTrail("uploaded chunk " + (i + 1), connection);
                    channelSftp.exit();
                    channelSftp.getSession().disconnect();
                    System.out.println("closed sftp connection");
                    log("closed sftp connection");
                    //addToAuditTrail("closed sftp connection", connection);



                }

                statement.executeUpdate("insert into Files values('" + lFile + "','"+crc32+"','" + username + "', datetime('now'), datetime('now'), datetime('now'), '"+isDir+"', '"+isFile+"', '"+isSymLink+"', '"+isOther+"', '"+initSize+"', "+numOfContainers+",'" + chunkName[0] + "', '" + chunkName[1] + "', '" + chunkName[2] + "', '" + chunkName[3] + "')");

                addToMyACL(lFile);
                System.out.println("added file to table Files");
                log("added file to table Files");
                //addToAuditTrail("added file to table Files", connection);



            } else{
                System.out.println("file with this name already exists. change the name of the file.");
                log("file with this name already exists. change the name of the file.");
                success = false;
            }

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
        return success;
    }


    public void downloadFile(String remoteFile, String owner, String whereToDownload) throws JSchException, SftpException, ZipException, IOException, SQLException, ClassNotFoundException {
        //1. get chunks from the table into String[] chunkNames = new String[4];
        String[] chunkNames = new String[4];

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            ResultSet rs = statement.executeQuery("select firstChunk, secondChunk, thirdChunk, fourthChunk from Files where filename = '" + remoteFile + "' and owner = '" + owner + "'");

            while (rs.next()) {
                chunkNames[0] = rs.getString(1);
                chunkNames[1] = rs.getString(2);
                chunkNames[2] = rs.getString(3);
                chunkNames[3] = rs.getString(4);

            }

        //2. download files with a loop and channessftp.get  //3. store chunks in list<files> chunks
        List<File> chunks = new ArrayList<>();
        for (int i = 0; i < numOfContainers; i++) {
            ChannelSftp channelSftp = setupJsch(i);
            channelSftp.connect();

            channelSftp.get(remoteDir + chunkNames[i], localAppPath + chunkNames[i]);
            System.out.println("downloaded chunk " + (i + 1));
            log("downloaded chunk " + (i + 1));
            //addToAuditTrail("downloaded chunk " + (i + 1), connection);
            File chunk = new File(localAppPath + chunkNames[i]);

            chunks.add(chunk);

            channelSftp.exit();
            channelSftp.getSession().disconnect();
            System.out.println("closed connection");
            log("closed connection");
            //addToAuditTrail("closed connection", connection);
        }

        //3.
        File result = joinChunks(chunks);

        String finalPathForCrc32Check = null;

            if (whereToDownload.equals("downloads")) {
                unzipFile(result, "/home/ntu-user/Downloads/", owner);
                finalPathForCrc32Check = "/home/ntu-user/Downloads/" + remoteFile;
            }
            else if (whereToDownload.equals("tmp")) {
                unzipFile(result, localAppPath, owner);
                finalPathForCrc32Check = localAppPath + remoteFile;
            }

            byte[] bytes = Files.readAllBytes(Paths.get(finalPathForCrc32Check));
            long downloadedFileChecksum = getCRC32Checksum(bytes);
            System.out.println(downloadedFileChecksum);
            log("downloaded file checksum: " + downloadedFileChecksum);
            long crc32 = 0;

            ResultSet rs2 = statement.executeQuery("select checksum from Files where filename = '" + remoteFile + "' and owner = '" + owner + "'");
            while (rs2.next()){
                crc32 = rs2.getLong("checksum");

            }
            if (crc32 != downloadedFileChecksum){
                System.out.println("file has been corrupted. crc32 are not equal");
                log("file has been corrupted. crc32 are not equal");
                //addToAuditTrail("file has been corrupted. crc32 are not equal", connection);
            } else{
                System.out.println("file has NOT been corrupted");
                log("file has NOT been corrupted");
                //addToAuditTrail("file has NOT been corrupted", connection);
            }

        } catch (SQLException | ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void recoverFiles(String filename) throws JSchException, IOException, ClassNotFoundException {
        // 1. move from recovery folder to app (select where
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.execute("PRAGMA foreign_keys = ON");

            ResultSet rs = statement.executeQuery("select * from recoveryFiles where filename = '" + filename + "' and owner = '" + username + "'");
            while (rs.next()) {

                for (int i = 0; i < numOfContainers; i++){
                    if (rs.getString(columnChunkNames[i])!=null)
                        executeCommands(i, "mv /home/recovery/"+rs.getString(columnChunkNames[i])+" /home/app/"+rs.getString(columnChunkNames[i]));
                    else {

                        log("RuntimeException: chunk " + (i + 1) + " is null");
                        throw new RuntimeException("chunk " + (i + 1) + " is null");
                    }
                }
//                String test = rs.getString("createdDate");
//                Date testdate = rs.getDate("createdDate");
//                System.out.println(testdate.toString());
//                System.out.println(testdate);

                System.out.println("moved chunks to app folders");
                log("moved chunks to app folders");
                //addToAuditTrail("moved chunks to recovery folders", connection);
                statement.executeUpdate("insert into Files values('" + rs.getString("filename") + "','"+ rs.getLong("checksum") +"','" + rs.getString("owner") + "', '"+rs.getString("createdDate")+"', '"+rs.getString("lastModified")+"', '"+rs.getString("lastAccessTime")+"', "+rs.getInt("isDir")+", "+rs.getInt("isFile")+", "+rs.getInt("isSymLink")+", "+rs.getInt("isOther")+", "+rs.getLong("filesize")+", "+rs.getInt("numOfContainers")+",'" + rs.getString("firstChunk") + "', '" + rs.getString("secondChunk") + "', '" + rs.getString("thirdChunk") + "', '" + rs.getString("fourthChunk") + "')");
                System.out.println("inserted chunks to Files table");
                log("inserted chunks to Files table");
                //addToAuditTrail("inserted chunks to Files table", connection);
                addToMyACL(filename);

            }

            //delete file from table Files which will delete it from all acls
            statement.executeUpdate("delete from recoveryFiles where filename = '" + filename + "' and owner = '" + username+"'");

            System.out.println("deleted row from recoveryFiles table");
            log("deleted row from recoveryFiles table");
            //addToAuditTrail("deleted row from recoveryFiles table", connection);

        } catch (SQLException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            ex.printStackTrace();
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void updateLastModifiedDateAndUploadFile(String filename, String ownerUsername) throws IOException, JSchException, SftpException, SQLException, ClassNotFoundException {

        File file = new File(localAppPath+filename);
        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        long initSize = attr.size();

        byte[] bytes = Files.readAllBytes(Paths.get(localAppPath+filename));
        long crc32 = getCRC32Checksum(bytes);

        File zippedFile = new File(zipAFile(localAppPath+filename));

        int fileSize = getSizeInBytes(zippedFile.length());
        List<File> chunks = splitBySize(zippedFile, fileSize);


        String[] chunkName = new String[4];


        for (int i = 0; i < chunks.size(); i++) {
            ChannelSftp channelSftp = setupJsch(i);
            channelSftp.connect();

            File chunk = chunks.get(i);
            File renamedChunk = new File(localAppPath +username+"_"+ filename + "_Chunk_" + (i+1) + ".zip");
            if (chunk.renameTo(renamedChunk)) {
                System.out.println("success renaming");
                log("success renaming");
            }
            else {
                System.out.println("fail renaming");
                log("fail renaming");
            }

            chunkName[i] = renamedChunk.getName();
            System.out.println(chunkName[i]);
            

            channelSftp.put(renamedChunk.getAbsolutePath(), remoteDir + renamedChunk.getName());
            System.out.println("uploaded chunk " + (i + 1));
            log("uploaded chunk " + (i + 1));
            //addToAuditTrail( "uploaded chunk " + (i + 1));
            channelSftp.exit();
            channelSftp.getSession().disconnect();
            System.out.println("closed connection");
            log("closed connection");
            //addToAuditTrail("closed connection");

        }

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);


            statement.executeUpdate("update Files set filesize = '"+initSize+"',lastModified = datetime('now'), lastAccessTime = datetime('now'), checksum = '"+crc32+"' where filename = '"+filename+"' and owner = '"+ownerUsername+"'");
            System.out.println("updated lastModified in Files table for file "+filename);
            log("updated lastModified in Files table for file "+filename);
            //addToAuditTrail("updated lastModified in Files table for file "+filename, connection);

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public int getNumOfContainersForFile(String filename, String owner) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(dbFileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);
        int res = 0;
        ResultSet rs = statement.executeQuery("select numOfContainers from Files where filename = '"+filename+"' and owner = '"+owner+"'");
        while (rs.next()){
            res = rs.getInt("numOfContainers");
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            // connection close failed.
            System.err.println(e.getMessage());
        }
        return res;
    }

    public int getNumOfContainersForRecoveryFile(String filename, String owner) throws SQLException, ClassNotFoundException {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(dbFileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);
        int res = 0;
        ResultSet rs = statement.executeQuery("select numOfContainers from recoveryFiles where filename = '"+filename+"' and owner = '"+owner+"'");
        while (rs.next()){
            res = rs.getInt("numOfContainers");
        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            // connection close failed.
            System.err.println(e.getMessage());
        }
        return res;
    }


    public void deleteFileFromMyAcl(String filename, String ownerUsername) { //when user deletes the file, and he is not the owner of it
        // delete the file from db and containers or remove user from acl
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);


            statement.executeUpdate("delete from " + userAclTableName + " where filename = '" + filename + "' and owner = '" + ownerUsername + "'");
            System.out.println("deleted "+filename + " from "+userAclTableName);
            log("deleted "+filename + " from "+userAclTableName);
            //addToAuditTrail("deleted "+filename + " from "+userAclTableName, connection);

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void revokeSomeoneFromFile(String filename, String userToRevoke, String owner) { // when user deletes other user's access from his files
        // delete the file from db and containers or remove user from acl
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            String otherUserAcl = "User" + userToRevoke + "ACL";

            statement.executeUpdate("delete from " + otherUserAcl + " where filename = '" + filename + "' and owner = '" + owner + "'");
            System.out.println("deleted " + filename + " from " + otherUserAcl);
            log("deleted " + filename + " from " + otherUserAcl);
            //addToAuditTrail("deleted " + filename + " from " + otherUserAcl, connection);

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void addToMyACL(String filename) { //when user uploads or creates files
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);


            statement.executeUpdate("insert into " + userAclTableName + " values('" + filename + "','" + username + "',1,1)");
            System.out.println("added file to table " + userAclTableName);
            log("added file to table " + userAclTableName);
            //addToAuditTrail("added file to table " + userAclTableName, connection);

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

    }

    public void addToOtherACL(String user, String filename, String owner, boolean readable, boolean writable) { //when user shares a file with someone
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            String otherUserAcl = "User" + user + "ACL";

            statement.executeUpdate("insert into " + otherUserAcl + " values('" + filename + "','" + owner + "'," + readable + "," + writable + ")");
            System.out.println("added "+filename+" to table " + otherUserAcl);
            log("added "+filename+" to table " + otherUserAcl);
            //addToAuditTrail("added "+filename+" to table " + otherUserAcl, connection);

        } catch (ClassNotFoundException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }

    }

    // end of server part

    public String zipAFile(String filepath) throws ZipException, IOException, SQLException, ClassNotFoundException //TODO: check if it is a directory and use different code for zipping it
    {
        File fileToZip = new File(filepath);
        String zippedChunkName = localAppPath + fileToZip.getName() + ".zip";

        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setCompressionLevel(CompressionLevel.MAXIMUM);
        zipParameters.setEncryptionMethod(EncryptionMethod.AES);

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(dbFileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);

        String key = null;
        ResultSet rs = statement.executeQuery("select encryptionKey from Users where name = '"+username+"'");
        while (rs.next()){
            key = rs.getString("encryptionKey");
        }

        try{
            ZipFile zipFile = new ZipFile(zippedChunkName, key.toCharArray());
            //zipFile.addFolder(new File(localAppPath), zipParameters);
            zipFile.addFile(new File(filepath), zipParameters);

            System.out.println("File "+fileToZip.getName()+" zipped with success");
            log("File "+fileToZip.getName()+" zipped with success");
            //addToAuditTrail("File "+fileToZip.getName()+" zipped with success", connection);
        }
        catch(Exception e)
        {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();

        }
//        FileOutputStream fos = new FileOutputStream(zippedChunkName);
//        ZipOutputStream zipOut = new ZipOutputStream(fos);
//
//
//        FileInputStream fis = new FileInputStream(fileToZip);
//        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
//        zipOut.putNextEntry(zipEntry);
//
//        byte[] bytes = new byte[1024];
//        int length;
//        while ((length = fis.read(bytes)) >= 0) {
//            zipOut.write(bytes, 0, length);
//        }
//
//        zipOut.close();
//        fis.close();
//        fos.close();

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            // connection close failed.
            log(Arrays.toString(e.getStackTrace()));
            System.err.println(e.getMessage());
        }

        return zippedChunkName;

    }

    public void unzipFile(File fileZip, String whereToUnzip, String owner) throws ZipException, FileNotFoundException, IOException, ClassNotFoundException, SQLException {

        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection(dbFileName);
        var statement = connection.createStatement();
        statement.setQueryTimeout(timeout);

        String key = null;
        ResultSet rs = statement.executeQuery("select encryptionKey from Users where name = "+owner);
        while (rs.next()){
            key = rs.getString("encryptionKey");
        }

        try{
            ZipFile zipFile = new ZipFile(fileZip, key.toCharArray());
            zipFile.extractAll(whereToUnzip);
            System.out.println("File unzipped with success");
            log("File unzipped with success");
            //addToAuditTrail("File unzipped with success", connection);
        }
        catch(Exception e)
        {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();

        }
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            log(Arrays.toString(e.getStackTrace()));
            // connection close failed.
            System.err.println(e.getMessage());
        }

//        File destDir = new File(localAppPath);
//
//        byte[] buffer = new byte[1024];
//        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
//        ZipEntry zipEntry = zis.getNextEntry();
//        File newFile = new File(destDir, zipEntry.getName());
//        try (FileOutputStream fos = new FileOutputStream(newFile)) {
//            int len;
//            while ((len = zis.read(buffer)) > 0) {
//                fos.write(buffer, 0, len);
//            }
//        }
//        zis.closeEntry();
//        zis.close();
//        System.out.println("unzipped");

    }

    String columnChunkNames[] = {"firstChunk", "secondChunk", "thirdChunk", "fourthChunk"};
    public void moveFileToRecoveryAndDeleteFromFilesTable(String filename) throws JSchException, IOException { //called if it is the owner who is deleting the file
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);
            statement.execute("PRAGMA foreign_keys = ON");

            ResultSet rs = statement.executeQuery("select * from Files where filename = '" + filename + "' and owner = '" + username + "'");
            while (rs.next()) {

                for (int i = 0; i < numOfContainers; i++){
                    if (columnChunkNames[i]!=null) {
                        executeCommands(i, "mv /home/app/" + rs.getString(columnChunkNames[i]) + " /home/recovery/" + rs.getString(columnChunkNames[i]));
                        //addToAuditTrail("closed exec connection", connection); // it is here, because otherwise I have to create executecommands(i, comm, connecion) override function and i don't want to do that. i think it will look like a mess
                    }
                    else {
                        //addToAuditTrail("chunk " + (i + 1) + " is null", connection);
                        throw new RuntimeException("chunk " + (i + 1) + " is null");
                    }
                }

                System.out.println("moved chunks to recovery folders");
                log("moved chunks to recovery folders");
                //addToAuditTrail("moved chunks to recovery folders", connection);
                int d = rs.getInt("isDir");
                int f = rs.getInt("isFile");
                int sl = rs.getInt("isSymLink");
                int o = rs.getInt("isOther");
                long s = rs.getLong("filesize");
                statement.executeUpdate("insert into recoveryFiles values('" + rs.getString("filename") + "','"+ rs.getInt("checksum") +"','" + rs.getString("owner") + "', '"+rs.getString("createdDate")+"', '"+rs.getString("lastModified")+"', datetime('now'), '"+rs.getString("lastAccessTime")+"', '"+rs.getInt("isDir")+"', '"+rs.getInt("isFile")+"', '"+rs.getInt("isSymLink")+"', '"+rs.getInt("isOther")+"', '"+rs.getLong("filesize")+"', '"+rs.getInt("numOfContainers")+"','" + rs.getString("firstChunk") + "', '" + rs.getString("secondChunk") + "', '" + rs.getString("thirdChunk") + "', '" + rs.getString("fourthChunk") + "')");
                System.out.println("inserted chunks to recovery table");
                log("inserted chunks to recovery table");
                //addToAuditTrail("inserted chunks to recovery table", connection);
            }

            //delete file from table Files which will delete it from all acls
            statement.executeUpdate("delete from Files where filename = '" + filename + "' and owner = '" + username+"'");
            System.out.println("deleted row from files table (and from acls respectively)");
            //addToAuditTrail("deleted row from files table (and from acls respectively)", connection);

        } catch (SQLException | ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void checkRecoveryFilesDate() { // is called every time program is launched
        // delete files: resultset rs = select chunk1, chunk2, chunk3, chunk4 from recoveryTable where currentdate - deleteddate > 30
        // and delete all the chunks
        try {
            // create a database connection
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.createStatement();
            statement.setQueryTimeout(timeout);

            ResultSet rs = statement.executeQuery("select * from recoveryFiles where julianday('now') - julianday(deletedDate) > 30");
            while (rs.next()) {
                //get the names of 4 chunks

                for (int i = 0; i < numOfContainers; i++){
                    if (columnChunkNames[i]!=null) {
                        executeCommands(i, "rm /home/app/" + rs.getString(columnChunkNames[i]) );

                    }
                    else {
                        log("RuntimeException: chunk " + (i + 1) + " is null");

                        throw new RuntimeException("chunk " + (i + 1) + " is null");
                    }
                }


                //delete all rows from rs from recoveryFiles table
                statement.executeUpdate("delete from recoveryFiles where filename = '" + rs.getString("filename") + "' and owner = '" + rs.getString("owner") + "'");
                System.out.println("deleted "+rs.getString("filename")+" from recovery");
                log("deleted "+rs.getString("filename")+" from recovery");
            }


        } catch (SQLException | ClassNotFoundException | JSchException | IOException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public static long getCRC32Checksum(byte[] bytes) {
        Checksum crc32 = new CRC32();
        crc32.update(bytes, 0, bytes.length);
        return crc32.getValue();
    }

    //file chunking here
    private int getSizeInBytes(long totalBytes) {
        int numberOfFiles = numOfContainers;
        if (totalBytes % numberOfFiles != 0) {
            totalBytes = ((totalBytes / numberOfFiles) + 1) * numberOfFiles;
        }

        long x = totalBytes / numberOfFiles;
        if (x > Integer.MAX_VALUE) {
            log("NumberFormatException: Byte chunk too large");
            throw new NumberFormatException("Byte chunk too large");

        }
        return (int) x;
    }
    //int size = getSizeInBytes(largeFile.length(), noOfFiles);
    //List<File> chunks = splitBySize(largeFile, size);

    public List<File> splitBySize(File largeFile, int maxChunkSize){
        List<File> list = new ArrayList<>();
        try (InputStream in = Files.newInputStream(largeFile.toPath())) {
            final byte[] buffer = new byte[maxChunkSize];
            int dataRead = in.read(buffer);
            while (dataRead > -1) {
                File fileChunk = stageFile(buffer, dataRead);

                list.add(fileChunk);
                dataRead = in.read(buffer);
            }
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return list;
    }

    private File stageFile(byte[] buffer, int length) throws IOException {
        //Files.createDirectories(Paths.get(localAppPath + "tmp/"));
        File outPutFile = File.createTempFile("temp-", "-split", new File(localAppPath));
        try (FileOutputStream fos = new FileOutputStream(outPutFile)) {
            fos.write(buffer, 0, length);
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        return outPutFile;
    }

    public File joinChunks(List<File> list) {
        File outPutFile = null;
        FileOutputStream fos = null;
        try {
            outPutFile = File.createTempFile("temp-", "unsplit", new File(localAppPath));
            fos = new FileOutputStream(outPutFile);
            for (File file : list) {
                Files.copy(file.toPath(), fos);
            }
            System.out.println("joined chunks into 1 file");
            log("joined chunks into 1 file");
            fos.close();
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            throw new RuntimeException(e);
        }
        //addToAuditTrail("joined chunks into 1 file");

        return outPutFile;
    }

    // end of file chunking


    public void updatePassword(String username, String newPassword) throws InvalidKeySpecException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("userProfile.fxml"));
        try {
            loader.load(); // Load the FXML file
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        userProfileController controller = loader.getController();

        if (newPassword.length() >= 8) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(dbFileName);
                String secPass = generateSecurePassword(newPassword);
                var statement = connection.prepareStatement("UPDATE Users SET password = '" + secPass + "' WHERE name = '" + username + "'");
                System.out.println("user " + username + ", password " + secPass + " updated to the database");
                log("user " + username + ", password " + secPass + " updated to the database");
                //addToAuditTrail("user " + username + "updated password");
                statement.executeUpdate();
                controller.dialogueSuccess("Success window", "Password was successfully updated.");

                printData();


            } catch (SQLException | ClassNotFoundException ex) {
                log(Arrays.toString(ex.getStackTrace()));
                Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    log(Arrays.toString(e.getStackTrace()));
                    // connection close failed.
                    System.err.println(e.getMessage());
                }
            }
        } else {
            log("Updating password error: The password must be at least 8 characters long.");
            controller.dialogueError("Updating password error", "The password must be at least 8 characters long.");
        }
    }

    public void deletePassword(String username) throws InvalidKeySpecException {
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("userProfile.fxml"));
        try {
            loader.load(); // Load the FXML file
        } catch (IOException e) {
            log(Arrays.toString(e.getStackTrace()));
            e.printStackTrace();
        }
        userProfileController controller = loader.getController();
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);

            var statement = connection.prepareStatement("UPDATE Users SET password = null WHERE name = '" + username + "'");

            statement.executeUpdate();
            log("user " + username + " password deleted from the database");
            controller.dialogueSuccess("Success window", "Password was successfully deleted.");


            printData();


        } catch (SQLException | ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
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

    private void printData() { //temp. TODO: delete this after testing
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);


            var statement2 = connection.createStatement();
            ResultSet rs = statement2.executeQuery("select * from Users");
            while (rs.next()) {
                //log("name = " + rs.getString("name") + " password = " + rs.getString("password"));
                System.out.println("name = " + rs.getString("name") + " password = " + rs.getString("password"));
            }


        } catch (SQLException | ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }

    public void deleteUser(String username) {

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(dbFileName);
            var statement = connection.prepareStatement("delete from Users where name = '" + username + "'");

            statement.executeUpdate();

            printData();


        } catch (SQLException | ClassNotFoundException ex) {
            log(Arrays.toString(ex.getStackTrace()));
            Logger.getLogger(DB.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                log(Arrays.toString(e.getStackTrace()));
                // connection close failed.
                System.err.println(e.getMessage());
            }
        }
    }




//    public static void main(String[] args) throws InvalidKeySpecException {
//        DB myObj = new DB();
//        myObj.log("-------- Simple Tutorial on how to make JDBC connection to SQLite DB ------------");
//        myObj.log("\n---------- Drop table ----------");
//        myObj.delTable(myObj.getTableName());
//        myObj.log("\n---------- Create table ----------");
//        myObj.createTable(myObj.getTableName());
//        myObj.log("\n---------- Adding Users ----------");
//        myObj.addDataToDB("ntu-user", "12z34");
//        myObj.addDataToDB("ntu-user2", "12yx4");
//        myObj.addDataToDB("ntu-user3", "a1234");
//        myObj.log("\n---------- get Data from the Table ----------");
//        myObj.getDataFromTable(myObj.getTableName());
//        myObj.log("\n---------- Validate users ----------");
//        String[] users = new String[]{"ntu-user", "ntu-user", "ntu-user1"};
//        String[] passwords = new String[]{"12z34", "1235", "1234"};
//        String[] messages = new String[]{"VALID user and password",
//            "VALID user and INVALID password", "INVALID user and VALID password"};
//
//        for (int i = 0; i < 3; i++) {
//            System.out.println("Testing " + messages[i]);
//            if (myObj.validateUser(users[i], passwords[i], myObj.getTableName())) {
//                myObj.log("++++++++++VALID credentials!++++++++++++");
//            } else {
//                myObj.log("----------INVALID credentials!----------");
//            }
//        }
//    }
}