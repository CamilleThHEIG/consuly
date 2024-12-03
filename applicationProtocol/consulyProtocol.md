# Consuly protocol

## Section 1 - Overview

Consuly protocol is a custom application protocol used by the application consuly. 

## Section 2 - Transport Protocol

Consuly protocol uses UDP to be able to handle multiple communications at the same time.  **Used port is yet to be defined.**
Every message must be encoded in UTF-8 and delimited by a newline character (\n). The messages are treated as text messages.  
The emitter must initiate the communication with the server. Once connected to a server, a user can join or create a group.  
When the client is done, it closes communication with the server, and when nobody is connected to the server for a delimited time,  
it closes itself.

Unknown messages are ignored.

## Section 3 - Messages

*Work in progress. Would it better better to split client errors and server errors ?*

ERROR <number> : indicates an error with it's number *number specifications yet to come*

### Establish connection

CONNECT_SRV : a client indicates it wants to connect to it

ACCEPT_CONNECT : server indicates the client connection is accepted. The client is now connected to the server.

REFUSE_CONNECT : server indicates the client connection is refused. The connection process ends here.

### Create a group

CREATE_GROUP : connected client indicates it wants to create a group.

PASSWORD_FOR <groupeNumber> : in response to CREATE_GROUP, server gives the client the group number, and asks for the password.

PASSWORD <password> : the client indicates a password for a group. <password> should be some text containing only letters. Password will be reviewed by the server.

VALID_PASSWD : server indicates the client that chosen password is valid. When sending this, the server needs to remember that this client is **admin** for this group number.

INVALID_PASSWD : server indicates the client that chosen password is not valid

### Delete a group

DELETE_GROUP : an admin indicates server that it want to delete its group.  Sender client now waits for the server to return SUCCESS_DELETION.

Return ERROR 1 if the sender is not an admin

FORCE_QUIT : asks a client to quit the server 

SUCESS_DELETION : server indicates the admin of a group that the group was successfully deleted.

### Join a group

JOIN <groupNumber> : client indicates it wants to join the group number <groupNumber>. Server will answer ERROR 3 if the group does not exist.

VERIFY_PSSWD : the server asks the the client who sent the previous JOIN for the group password.

TRY_PASSWD <password> : client indicates the server that it thinks the password is <password>.

### Quit a group

QUIT : the client indicates the server it wants to quit the group its in. 

ACK_QUIT : the server indicates the server that it understood that the client quitted its group

### Send preferences

READY_SEND : the client indicates the server that it's ready to send it's preference list. 

READY_RECEIVE : the server indicates the client that it's ready to receive the list.

SENDING_LIKES : the client indicates the server that it will now send what is in the like section of it's user file

SENDING_DISLIKES : the client indicates the server that it will now send what is in the like section of it's user file

SENDING_NEUTRAL : the client indicates the server that it will now send what is in the like section of it's user file

STYLE <name> : client sends a style from it's user file, <name> being the name of the style.

ACK : server indicates client that it correctly received the client's message

FINISHED : client indicates the server that it finished to send it's user list



### Listing existing groups

LIST_GROUPS : client indicates it wants to sever to list the existing groups. Server can answer by START_SEND or NO_GROUPS

GROUPS : server indicates their is no group to send

START_SEND : server indicates it will now begin to send the existing groups. Client answer with ACK to inidcate it's ready to receive.

GROUP <id> : server indicates that group with id <id> it available. Client answers with ACK to indicate it acknoledged the information.

END_SEND_GROUPS : server indicates there is no more groups to send.





## Section 4 - Examples