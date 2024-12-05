# consuly

> Authors : Yoann Changanaqui, Camille Theubet

Welcome ! This is todo-manager's repository, a simple command line application to create and manage simple todo lists. Using todo-manager, you can write and read simple tasks with todo-manager, with different status each.

Each user has it's personnal file so that everyone can have it's own todo list.

This application requires a terminal environment and Java installed on your system.

Don't know where to start? See [Getting Started](#getting-started).

Curious about how it works? Check [How it Works](#how-it-works).

Need usage instructions? See [How to Use](#how-to-use).

For available commands, see [Commands](#commands).

## Getting Started
These instructions will help you set up the project on your local machine.

Prerequisites
Ensure you have Java 11 or later installed. If you're unsure how to install Java, refer to this [guide](https://github.com/heig-vd-dai-course/heig-vd-dai-course/blob/e57a2205b48ce2a435624adbb713d83e30b408b0/04-java-intellij-idea-and-maven/COURSE_MATERIAL.md#install-sdkman).

Have docker install in your linux environnment.

Installing
Get the consuly image with the command :
```bash
docker pull ghcr.io/yoy017/consuly:latest
```

Create your own docker network for communication between two terminal.
```bash
docker network create <networkname>
```

Launch the server.
```bash
docker run --rm -it --network <networkname> --name <servername> consuly server
```

Launch a client.
```bash
docker run --rm -it --network <networkname> consuly client --host <servername>
```

You can now run the application by running the docker image.
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
This section outlines the steps to interact with the application.

Starting the Server
To run the server:

```bash
java -jar target/Consuly-1.0-SNAPSHOT.jar server
```

Starting the Client
To run the client:

```bash
java -jar target/Consuly-1.0-SNAPSHOT.jar client
```
Group Interaction
Once connected, use the following commands:

CREATE: Start a new group.
JOIN: Enter an existing group.
LIST: View all available groups.
QUIT: Exit the application or leave a group.
Group admins can finalize playlists or delete groups.

Commands
Server
The server does not require additional commands; it runs autonomously to handle client requests.

Client
Here are some example client commands:

CREATE GROUP:

```bash
CREATE
```
Follow prompts to name the group and set a password.

JOIN GROUP:
```bash
JOIN
```
View the group list, select one, and provide the password.

LIST GROUPS:
```bash
LIST
```

QUIT GROUP:
```bash
QUIT
```

For help on any command, use:
```bash
[COMMAND] -h
```
