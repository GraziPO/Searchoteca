package searchoteca.auditTrail;

public enum AuditAction {
    ACCESS,
    CREATE,
    UPDATE,
    DELETE,
    ACTIVATE_USER,
    DEACTIVATE_USER,
    DEACTIVATE_REQUEST,
    ACTIVATE_REQUEST,
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    ACCESS_DENIED,
}
