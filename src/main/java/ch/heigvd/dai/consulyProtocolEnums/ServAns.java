package ch.heigvd.dai.consulyProtocolEnums;

public enum ServAns {
    ACCEPT_CONNECT,
    REFUSED_CONNECT,
    CHOOSE_PASSWORD,
    VALID_PASSWORD,
    INVALID_PASSWORD,

    VERIFY_PASSWD,
    PASSWD_SUCCESS,

    ACK,
    FORCE_QUIT,
    INVALID_GROUP,
    READY_RECEIVE,
    AVAILABLE_GROUP,
    RELEASE_READY,
    SEND_PREF_LIST,


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
    ERROR_8,
    ERROR_9,    // error linked to the fact that user is not in a group
    ERROR_13,   // used to signify user that it's not in a group
    NONE
}