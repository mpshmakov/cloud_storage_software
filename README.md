# cloud_storage_software
made with java and javafx library. 

currently for the software to function, you have to ssh into every file storage container and create home/app and home/recovery directories:
mkdir /home/app
mkdir /home/recovery

Features:
* login/register
* update/delete user's password
* upload, download and edit files in the cloud
* file sharing with other users
* recover recently deleted files
* utilizes docker containers for file storage
* files are chunked, encrypted and the chunks are stored on different containers for security
* database integration to store information about users, files and acl's
* local/remote terminal within the software