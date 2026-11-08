package searchoteca.DTO.archive;

import java.util.List;

public record BatchMoveResult(
        String localCode,
        int moved,
        List<CopyLabel> labels,
        List<String> alreadyInLocation
) {
}
