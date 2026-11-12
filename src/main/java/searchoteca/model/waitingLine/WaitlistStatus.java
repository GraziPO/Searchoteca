package searchoteca.model.waitingLine;

public enum WaitlistStatus {
    WAITING,
    NOTIFIED,
    FULFILLED, //aluno pegou a cópia
    EXPIRED, //passou do prazo e passa a vex
    CANCELLED,
}
