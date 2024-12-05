package ch.heigvd.dai.consulyProtocolEnums;

public enum ServAns {
    ACCEPT_CONNECT,
    REFUSED_CONNECT,
    PASSWORD_FOR,
    VALID_PASSWORD,
    INVALID_PASSWORD,
    GRANT_ADMIN,
    ACK_QUIT,
    FORCE_QUIT,
    INVALID_GROUP,


    VERIFY_PASSWD,
    PASSWD_SUCCESS,
    RETRY_PASSWD,
    NO_MORE_TRIES,

    INVALID_ID,
    WAITING_USER_TO_QUIT,
    SUCCESS_DELETION,
    FAILURE_DELETION,

    END_OF_LIST,
    NO_GROUP,

    WEIRD_ANSWER,

    ERROR,      // signifies an error, no matter the type
    ERROR_7,    // to signify user that it does not have admin rights
    ERROR_13,   // used to signify user that it's not in a group
    NONE
}