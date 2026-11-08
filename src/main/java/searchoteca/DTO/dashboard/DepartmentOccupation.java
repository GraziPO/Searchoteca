package searchoteca.DTO.dashboard;

public record DepartmentOccupation(
        String department,
        long loaned,
        long total
) {

    public int percentual(){
        return total == 0 ? 0 : (int)Math.round(loaned * 100.0 / total);
    }
}
