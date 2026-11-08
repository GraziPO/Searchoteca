package searchoteca.DTO.archive;

import java.util.List;

public record BatchMoveRequest(
        String localCode,
        List<String> copyCodes,
        Integer expectedQuantity
) {
}
