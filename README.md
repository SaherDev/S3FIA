## Secure File Storage and Sync with Integrity and Authentication

#### client.jar (in Client_Jar folder)

- open terminal and enter java -jar client.jar to run the app.
- choose server: choose server from list view and click choose server btn (get version from all no need to choose any server).
- strat: enter client - client name, key , hmac password  and click start.
- Upload: choose from combobox list Upload option enter path for file (dir1/sub2) without file name after that press enter to choose file to send.
- Download: choose Download option enter path and file name (path/file) and press enter.
- Delete: choose Delete option enter path and file name (path/file) and press enter.
- Move: choose Move option enter (path/file ; newpath/file) and press enter.
- Get_List: choose Get_List option and press enter.
- Get_Version: choose get_version option enter (path/file) and press enter.
- GET_VERSION_ALL: choose and enter (path/file) and press enter => send getversion for all servers
- attack - click in on from 3 options (no attack, wrong nonce, wrong time)

- all out/in messages/error Shown on the screen
- all out/in messages/error Shown on the screen and saved to log file.

#### Server.jar (in Server_Jar folder)

- open terminal and enter java -jar Server.jar to run the app.
- start : enter server name, choose ip from list view to listen (you can change the port), click start [start listening to ip/port].
- stop : click stop [stop listening].
- update password : enter client-server or server-server password or hmac password and click update passwords.
- update server info : select server from list, change the server info and click update server.
- update user info : select user from list, change the user info and click update server.
- attack - click in on from 4 options (no attack, wrong public key, wrong nonce, wrong time)

- all out/in messages/error Shown on the screen and saved to log file.
- the server when start retrieve the version numbers and digests for each file, and download needed file by its version.
- when the client upload/move/delete file the server should send request to other servers offer/move/delet.
- all the

---

#### Configuration files

- server(in Server_Jar):

##### AppData.txt

1] line 1: ("root/") \
2] line 2: (port)
3] line 3:  RSA private key
4] line 4]: RSA public Key
5] line 5]: hashed hmac password in hex format
5] line 6]: server name

- Servers.txt ("servername" = "ip:port" = "RSA public key" ) contains servers list.
- Log.txt (time | fromorto| client| message/error | iv | hmac | valid/Invalid  ).
- server db: when the server start running create new tblFile(file,version,hash) in the FileServer.db (if not exist)
- client(in Client_Jar): use the Servers.txt ("serverName" = "ip:port")

---

#### Thread safety

- Thread safety ensured in the server by maintain map of filenames to ReadWriteLock that allows thread to acquire/release a lock by "path/file"
- when the thread start using the file the thread acquire lock by "path/file", and release is when the process is ended.
- Two threads can write to two different files concurrently, but not to the same "path/file".
- when the server1 offer file to server 2, server 2 ,must acquire/release a lock for the file when its download.

---

#### encryption

- all the server-server communication encrepted using aes/GCM/NoPadding and servers shared password.
- all the client-server communication encrepted using aes/cbc/PKCS5Padding and client-server shared password.
- to encrypt any message/file need 4 params (cipher,password,iv,message/file).
- in the server-client Communication  we encrypt the message/file and attached it with the iv amd hmac (iv, encryptedmessage, hmac)and send it to other side to decrypt it.
- when GCM decryption is failed then shows error message.
- all the password are hashed and converted to aes key.


---

#### HMAC

- digest of sent or received messages in hexadecimal notation.
- the mac computation includes the contents of the message and the IV combined together.
- when the message arrived the client or the server app check if the hmac valid or not, if not valid then shows an error message.

---

#### RSA PUBLIC / PRIVATE KEY 

- server RSA public / private keys stored in Appdata.txt, other servers RSA public key stored in Servers.txt.
- public key load: read the key from file into a key specification class able to handle a public key material. In our case, we’re going to use the X509EncodedKeySpec class, and we generate a public key object from the specification using the KeyFactory class. 
- private key load: read the key from file into a key specification class able to handle a public key material, using  PKCS8EncodedKeySpec class, generate a private key object from the specification using the KeyFactory class. 

---

#### CHALLENGE REPONSE

##### SERVER- CLIENT
- Before a client and server begin a command session, the client must first authenticate using the private client password and the challenge-response protocol.
1]  client send to the name to server ("name").
2]  server create nonce 16 in hex format and send to the client ("nonce").
3]  client retrievs the nonce, encrypts the the nonce + time and esnd to server ("nonce"+ "time").
4]  server decrypts with client key, verify the time, nonce (wrong nonce or time the server close the connection).
5] client can send to server 1.

##### SERVER- SERVER
- Before two servers begin a command or synchronization session, the initiating server must first authen-ticate using the public key based challenge-response protocol.
1]  server1 send to the name to server2 ("name").
2]  server2 create nonce 16 in hex format encrypts with server server1 public key and send to the server1 ("nonce").
3]  server1 retrievs and decrypts with private key the the nonce, encrypts new message nonce + time + aes key and esnd send server2 ("nonce"+ "time"+"aes key").
4]  server2 decrepts the message, verify the time, nonce (wrong nonce or time the server close the connection).
5] server1 can send to server 2.

