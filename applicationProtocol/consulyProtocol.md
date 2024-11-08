# Consuly protocol

## Section 1 - Overview

Consuly protocol is a custom application protocol used by the application consuly.

## Section 2 - Transport Protocol

Consuly protocol uses UDP to be able to handle multiple communications at the same time.  
Every message must be encoded in UTF-8 and delimited by a newline character (\n). The messages are treated as text messages.  
The emitter must initiate the communication with the server. Once connected to a server, a user can join or create a group.  
When the client is done, it closes communication with the server, and when nobody is connected to the server for a delimited time,  
it closes itself.

Unknown messages are ignored.

## Section 3 - Messages

*Work in progress*

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

## Section 4 - Examples