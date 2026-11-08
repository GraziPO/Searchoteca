package searchoteca.model.Books.status;

public enum ExitType {
    LOAN(CopyStatus.LOANED),
    SALE(CopyStatus.SOLD),
    DONATION(CopyStatus.DONATED),
    WRITE_OFF(CopyStatus.WRITTEN_OFF);

    private final CopyStatus resultingStatus;

    ExitType(CopyStatus resultingStatus) {
        this.resultingStatus = resultingStatus;
    }

    public CopyStatus resultingStatus() {
        return resultingStatus;
    }
}
