package searchoteca.service.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.archive.BatchMoveRequest;
import searchoteca.DTO.archive.BatchMoveResult;
import searchoteca.DTO.archive.CopyLabel;
import searchoteca.DTO.archive.CopyRequest;
import searchoteca.auditTrail.AuditAction;
import searchoteca.auditTrail.AuditLogService;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.BookModel;
import searchoteca.model.Books.CopyModel;
import searchoteca.model.LocationModel;
import searchoteca.model.Books.status.CopyStatus;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.repository.archive.LocationRepository;
import searchoteca.service.WaitlistService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CopyService {
    private final CopyRepository copyRepository;
    private final LocationRepository locationRepository;
    private final BookRepository bookRepository;
    private final AuditLogService auditLogService;
    private final WaitlistService waitlistService;
    private static final int MAX_BATCH = 200; //limite de cópias que podem ser localizadas por vez

    public CopyService(CopyRepository copyRepository, LocationRepository locationRepository, BookRepository bookRepository, AuditLogService auditLogService, WaitlistService waitlistService) {
        this.copyRepository = copyRepository;
        this.locationRepository = locationRepository;
        this.bookRepository = bookRepository;
        this.auditLogService = auditLogService;
        this.waitlistService = waitlistService;
    }

    public List<CopyModel> findAllByBook(String isbn) {
        return copyRepository.findAllByIsbn(isbn);
    }


    @Transactional
    public List<CopyLabel> create(CopyRequest request) {
        if(request.quantity()< 1 || request.quantity()>100) {
            throw new ResourceConflictException("A quantidade deve estar entre 1 e 100");
        }
        if(bookRepository.findByIsbn(request.isbn()) == null){
            throw new ResourceNotFoundException("Este livro não existe");
        }
        LocationModel local = locationRepository.findByLocalCode(request.localCode());
        if(local == null){
            throw new ResourceNotFoundException("Localização não encontrada");
        }
        if(!local.getDepartCode().equals(request.departCode())){
            throw new ResourceConflictException("Esta localização não pertence ao departamento");
        }

        int lastCreatedIndex = copyRepository.findMaxIndexByIsbn(request.isbn());

        List<CopyModel> copies = new ArrayList<>();

        for(int i = 1; i <= request.quantity(); i++) {
            CopyModel copy = new CopyModel();
            copy.setIsbn(request.isbn());
            copy.setLocalCode(request.localCode());
            copy.setDepartCode(request.departCode());
            copy.setCopy_index(lastCreatedIndex + i);
            copy.setStatus(CopyStatus.AVAILABLE.name());
            copies.add(copy);
        }

        List<CopyModel> saved = new ArrayList<>();
        copyRepository.saveAll(copies).forEach(saved::add);

        if (waitlistService.hasWaiting(request.isbn())) {
            for (CopyModel copy : saved) {
                waitlistService.releaseCopy(copy);
            }
        }
        return findLabels(saved.stream().map(CopyModel::getCopyId).toList());
    }

    public List<CopyLabel> findLabels(List<Long> copyIds){
        if(copyIds == null || copyIds.isEmpty() ){
            throw new ResourceNotFoundException("informe ao meonso um exemplar");
        }

        List<CopyModel> copies = new ArrayList<>();
        copyRepository.findAllById(copyIds).forEach(copies::add);

        if(copies.size()!=copyIds.size()){
            Set<Long> found = copies.stream()
                    .map(CopyModel::getCopyId)
                    .collect(Collectors.toSet());

            List<Long> missing = copyIds.stream().filter(id -> !found.contains(id)).toList();
            throw new ResourceNotFoundException("exemplares não encontrados" + missing);
        }

        Set<String> isbns = copies.stream().map(CopyModel::getIsbn).collect(Collectors.toSet());
        Map<String, BookModel> books = bookRepository.findByIsbnIn(isbns).stream()
                .collect(Collectors.toMap(BookModel::getIsbn, b -> b));

        return copies.stream()
                .sorted(Comparator.comparing(CopyModel::getCopy_index))
                .map(c ->{
                    BookModel book = books.get(c.getIsbn());
                    return  new CopyLabel(c.getCopyId(),c.getCopy_custom_code(),
                            book.getTitle(),book.getAuthor(),c.getDepartCode(),c.getLocalCode());
                })
                .toList();
    }

    @Transactional
    public BatchMoveResult moveBatch(BatchMoveRequest request) {
        if (request.copyCodes() == null || request.copyCodes().isEmpty()) {
            throw new ResourceConflictException("Leia ao menos um exemplar");
        }

        if (request.copyCodes().size() > MAX_BATCH) {
            throw new ResourceConflictException("Máximo de " + MAX_BATCH + " exemplares por movimentação");
        }

        Set<String> codes = request.copyCodes().stream()
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (codes.size() != request.copyCodes().size()) {
            throw new ResourceConflictException("Há códigos lidos mais de uma vez");
        }

        if (request.expectedQuantity() != null && request.expectedQuantity() != codes.size()) {
            throw new ResourceConflictException("Foram lidos " + codes.size() + " exemplares, mas a quantidade informada é "
                    + request.expectedQuantity() + ". Confira antes de confirmar.");
        }

        //O departamento vem da localização, nunca da requisição
        LocationModel target = locationRepository.findByLocalCode(request.localCode());
        if (target == null) {
            throw new ResourceNotFoundException("Localização de destino não encontrada");
        }

        //Valida todos os exemplares e juntar todos os problemas numa resposta só
        List<CopyModel> copies = copyRepository.findByCustomCodes(codes);
        Set<String> found = copies.stream().map(CopyModel::getCopy_custom_code).collect(Collectors.toSet());

        List<String> missing = codes.stream().filter(c -> !found.contains(c)).toList();
        List<String> unavailable = copies.stream()
                .filter(c -> !CopyStatus.AVAILABLE.name().equals(c.getStatus()))
                .map(CopyModel::getCopy_custom_code)
                .toList();

        if (!missing.isEmpty() || !unavailable.isEmpty()) {
            StringBuilder msg = new StringBuilder("Movimentação cancelada, nenhum exemplar foi movido.");
            if (!missing.isEmpty()) {
                msg.append(" Não encontrados: ").append(missing).append(".");
            }
            if (!unavailable.isEmpty()) {
                msg.append(" Indisponíveis (emprestados ou fora do acervo): ").append(unavailable).append(".");
            }
            throw new ResourceConflictException(msg.toString());
        }

        //Mover e montar o rastro para a auditoria
        List<CopyModel> toMove = new ArrayList<>();
        List<String> alreadyThere = new ArrayList<>();
        StringJoiner trail = new StringJoiner("; ");

        for (CopyModel copy : copies) {
            if (target.getLocalCode().equals(copy.getLocalCode())) {
                alreadyThere.add(copy.getCopy_custom_code());
                continue;
            }
            trail.add(copy.getCopy_custom_code() + ": " + copy.getLocalCode() + " -> " + target.getLocalCode());
            copy.setLocalCode(target.getLocalCode());
            copy.setDepartCode(target.getDepartCode());
            toMove.add(copy);
        }

        if (toMove.isEmpty()) {
            return new BatchMoveResult(target.getLocalCode(), 0, List.of(), alreadyThere);
        }

        copyRepository.saveAll(toMove);
        auditLogService.record(AuditAction.UPDATE,
                "movimentação em lote para " + target.getLocalCode() + " — " + trail);

        List<CopyLabel> labels = findLabels(toMove.stream().map(CopyModel::getCopyId).toList());

        return new BatchMoveResult(target.getLocalCode(), toMove.size(), labels, alreadyThere);
    }

}
