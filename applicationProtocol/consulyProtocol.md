# Consuly protocol

**SECTION 3 NOT UP TO DATE**

## Section 1 - Overview

Consuly protocol is a custom application protocol used by the application consuly. The goals is to allow a server to communicates with all its connected client to exchange information on which user joins which group, and to exchange contents of preference json files with only messages.

## Section 2 - Transport Protocol

Consuly protocol uses multithreaded TCP to be able to handle multiple communications at the same time.  Default port is UTF-8.
Every message must be encoded in UTF-8 and delimited by a newline character (\n). The messages are treated as text messages.  
The emitter must initiate the communication with the server. Once connected to a server, a user can join or create a group.  
When the client is done, it closes communication with the server

Unknown messages are ignored.

## Section 3 - Messages

### Establish connection

The client wants to establish first contact with the server.

#### Connect to the server

```CONNECT_SRV```
a client indicates it wants to connect to the server

#### Response

- `ACCEPT_CONNECT` : the server accepts the connection, and indicates the client it's now connected to server
- `REFUSE_CONNECT` : the server indicates it can not accept client connection

### Create a group

#### Asking to create a group

`CREATE_GROUP <groupName>` : a connected client indicates it wants to create a new group, with groupName beeing the
name the group.

#### Response

- `CHOOSE_PASSWORD` : server indicates it wants client to choose a password for the group

#### Set a password for the group

`PASSWORD <password> ` : client indicates the chosen password for the group

#### Response

- `VALID_PASSWORD` : server indicates the password is valid. Client is then considered to have joined the group, and to be it's admin.
- `INVALID_PASSWORD` : server indicates the password is not valid

### Join a group

#### Signify the group to join

`JOIN <groupName>` : client indicates it wants to join the group with name <groupName>

#### Response

- `VERIFY_PASSWD` : server asks the client to provide the group password
- `INVALID_GROUP` : server indicates the group doesn't exist

#### Asking to create a group

`TRY_PASSWD <password>` : user tries <password> as the password to join the group

#### Response

- `PASSWD_SUCCESS` : server indicates that password was correct, and that it's now in the group
- `RETRY_PASSWD` : server indicates that password was not correct, but that client can try again
- `NO_MORE_TRIES` : server indicates that password was not correct, and that client can not try again
- `ERROR_8` : unknown error happened during password verification

### List existing groups

A user may want to know the already existing group, to make his choice on which to join.

#### Asking for existing groups

`LIST_GROUPS` : client indicates the server that it wants to receive the list of existing groups

#### Response

- `NO_GROUP` : server indicates there is currently no group created
- `AVAILABLE_GROUP <groupName>` : server indicates that group "groupName" is available. Server sends this message as long as
  it has group names to send. Server ends the sending of group names with `END_OF_LIST`

### Once in a group

Once in a group, user can perform a few actions before the final playlist is made

#### Quit a group

`QUIT` : client indicates server it's quitting it's group. Only normal user can do that, no admin.

#### Response

- `ACK` : server indicates it acknowledged that client left his group
- `ERROR_9` : server sends this error to indicate that user is not in a group

#### Delete a group

Once in group, an admin may want to delete his group. Normal user can not do that.

- `DELETE_GROUP` : client indicates that it wants to delete it's group

#### Response

- `SUCCESS_DELETION` : server indicates that deletion succeeded
- `FAILURE_DELETION` : server indicates that deletion failed
- `ERROR_9` : server sends this error to indicate that user is not in a group

#### Signify readyness

Since this a bit particular, we will add a few precision.

Consuly works with the logic that for (almost) each message client sends, server sends once answer. Each client has only
on communication with the server, and can not listen for server message AND user input. That's how we can with this command.
A user being "ready" indicates that it's ready to receive an order from the server. This order can either be to quit the server
(because admin wanted to delete) or to send the preference list (in the background).

`READY` : client indicates it's ready to received order

#### Response

- `FORCE_QUIT` : server indicates that it wants client to leave
- `SEND_PREF_LIST` : server indicates that it wants the client to send its preference list
- `RELEASE_READY` : server indicates client it releases the hold it had on it

#### Make the final playlist

Only admin can do that

`MAKE` : client indicates the server that it wants to make the final playlist

#### Response

- `SEND_PREF_LIST` : server indicates that it wants the client to participate and to send its preference list

#### Signify we start to send the list

`READY_SEND` : client indicates the server it's ready to send it's preference list

#### Response

- `READY_RECEIVE` : server indicates it's ready to receive the informations
- `ERROR` : an error occurred. Client should abord the process

#### Specifying which sublist is sent now

Client indicates which sublist styles will be sent now

`SENDING_<SUBLISTNAME>` : client indicates the server it's going to send the styles from list <SUBLISTNAME>. <SUBLISTNAME>
is either "LIKES", "DISLIKES", or "NEUTRAL"

#### Response

- `ACK` : server indicates it acknowledged the information
- `ERROR` : an error occurred. Client should abord the process

#### Sending a style

`STYLE <nomStyle>` : client sends on the style <nomStyle>

#### Response

- `ACK` : server indicates it registered the style in the previously specified sublist
- `ERROR_89` : server indicates that no sublist was specified

#### End process of sending list

`FINISHED` : client indicates it's done with its user preference list

#### Response

- `ACK` : server indicates it acknowledged

## Section 4 - Examples

![Examples](consulyProtocol.png)