# consuly

> Authors : Yoann Changanaqui, Camille Theubet

Welcome ! This is the consuly's repository, you create your server and each client send his lists of preference. Using consuly, you can finally decide what's the common point in term of music taste in a group. No more arguments !

Each user has it's personnal file so that everyone can have it's custom list of preferences.


Don't know where to start? See [Getting Started](#getting-started).

Curious about how it works? Check [How it Works](#how-it-works).

Need usage instructions? See [How to Use](#how-to-use).

## Getting Started
These instructions will help you set up the project on your local machine.

Prerequisites
Ensure you have Java 11 or later installed. If you're unsure how to install Java, refer to this [guide](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/e57a2205b48ce2a435624adbb713d83e30b408b0/04-java-intellij-idea-and-maven/COURSE_MATERIAL.md#install-sdkman).

Have docker install in your environnment.

Installing
**Get the consuly image** with the command :
```bash
docker pull ghcr.io/camillethheig/consuly:latest
```

**Create your own docker network** for communication between two terminal.
```bash
docker network create <network-name>
```

**Launch the server**.
```bash
docker run --rm -it --network <network-name> --name <server-name> consuly server
```

launch a complete new **client session**
```bash
docker run --rm -it --network <network-name> consuly client --host <server-name>
```

Create your **list of preferences** if not done yet.
```bash
docker run --rm -it --network consuly-network consuly client -e --host the-server
```

You can now **start the application** by running the docker image with the commands above.

## How it Works
This application operates on a Client-Server architecture:

The server maintains group data, manages preferences, and computes final playlists.
The client interacts with the server to create or join groups, submit preferences, and manage group settings.

## Data Management
User preferences are stored in JSON format:

like: Preferred genres/styles.
dislike: Genres/styles to avoid.
neutral: Genres/styles with no preference.
The server aggregates preferences across group members to generate the final playlist, prioritizing likes while removing conflicting dislikes.

## How to Use
This section is for users that simply want to use the application without knowing how it works.

Consuly allow user to create groups that other user can then join.

After launching your client _(if the server has been started first of course)_, your connection will be maintained until you quit it.

## Commands

### CREATE
#### Description:
This command allows a client to create a new group on the server. The creator of the group becomes its admin and is responsible for managing it (e.g., finalizing playlists or deleting the group).

#### Usage:

```bash
CREATE
```
#### Flow:
- The server prompts the client to provide a group name.
- The server asks for a password to protect the group.
- Once successfully created, the group is listed as available for others to join.

#### Notes:
Only the admin can delete the group or finalize the playlist.
Group names must be unique on the server.

### JOIN
#### Description:
This command allows a client to join an existing group using the group name and password.

#### Usage:

```bash
JOIN
```

#### Flow:
Use the LIST command to see available groups.
Enter the desired group name.
Provide the group password.
Upon successful login, the user is added to the group.

#### Notes:
- A user can attempt the password up to three times. Failing this, access is denied.
- Only one group can be joined at a time.

### LIST
Description:
Displays all groups currently available on the server.

Usage:

```bash
LIST
```
Flow:

The server returns a list of group names.
Use this list to decide which group to join using the JOIN command.
Notes:

If no groups exist, the server will notify the client.


Here’s a more detailed and polished section for your README.md with a complete review of the commands:

Commands
The following commands are available in the Collaborative Playlist Manager. Each command serves a specific purpose, either for group management or for interacting with the server. You can always get help for a command by using the -h flag. For example:

```bash
CREATE
```
Description:
This command allows a client to create a new group on the server. The creator of the group becomes its admin and is responsible for managing it (e.g., finalizing playlists or deleting the group).

Usage:

```bash
CREATE
```

#### Flow:

The server prompts the client to provide a group name.
The server asks for a password to protect the group.
Once successfully created, the group is listed as available for others to join.

#### Notes:

Only the admin can delete the group or finalize the playlist.
Group names must be unique on the server.
JOIN
Description:
This command allows a client to join an existing group using the group name and password.

#### Usage:

```bash
JOIN
```

#### Flow:

Use the LIST command to see available groups.
Enter the desired group name.
Provide the group password.
Upon successful login, the user is added to the group.

#### Notes:
A user can attempt the password up to three times. Failing this, access is denied.
Only one group can be joined at a time.
LIST
#### Description:
Displays all groups currently available on the server.

#### Usage:

```bash
LIST
```

#### Flow:

The server returns a list of group names.
Use this list to decide which group to join using the JOIN command.
Notes:

If no groups exist, the server will notify the client.

### QUIT
Description:
This command allows a client to either leave the currently joined group or disconnect from the server entirely.

Usage:

```bash
QUIT
```

#### Flow:

If the user is in a group:
The user is removed from the group.
Admins leaving will trigger group deletion if it’s not handed over.
If the user is not in a group:
The client disconnects from the server.
Notes:

Leaving a group does not delete the group unless the user is the admin.

### READY
Description:
Signals the server that the user is ready to submit their playlist preferences. This is typically used in preparation for finalizing a group playlist.

Usage:

```bash
READY
```

#### Flow:

The server may prompt the client to send their preference list (likes, dislikes, neutral).
The user uploads their preferences to the server.
#### Notes:

If the user is not ready, the admin may finalize the playlist without their preferences.
The server can kick a user who is not ready after a certain time.

### MAKE
Description:
Allows the admin of a group to finalize the playlist for the group. This command aggregates preferences from all group members to create the final playlist.

Usage:

```bash
MAKE
```

#### Flow:

The admin signals the server to start the finalization process.
The server collects preferences from all members marked as "ready."
The server generates the final playlist by:
Prioritizing liked styles.
Removing disliked styles that conflict.
The finalized playlist is shared with the group.
Notes:

Only the admin can execute this command.
If members are not ready, their preferences will not be included.
