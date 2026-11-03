package searchoteca.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchoteca.DTO.archive.SalesNExists.ExitRequest;
import searchoteca.DTO.archive.SalesNExists.ReceiptItem;
import searchoteca.DTO.archive.SalesNExists.ReceiptSnapshot;
import searchoteca.exception.ResourceConflictException;
import searchoteca.exception.ResourceNotFoundException;
import searchoteca.model.Books.BookModel;
import searchoteca.model.Books.CopyModel;
import searchoteca.model.Books.status.CopyStatus;
import searchoteca.model.Books.status.ExitType;
import searchoteca.repository.catalog.BookRepository;
import searchoteca.repository.catalog.CopyRepository;
import searchoteca.security.CustomUserDetails;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class exitService {
    private final CopyRepository copyRepository;
    private final BookRepository bookRepository;

    public exitService(CopyRepository copyRepository, BookRepository bookRepository) {
        this.copyRepository = copyRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional
    public ReceiptSnapshot register(ExitRequest request, CustomUserDetails operator) {
        ExitType type = request.type();
        if(type == null || type ==ExitType.LOAN){
            throw new ResourceConflictException("Informe o tipo de saída. Empréstimos não são contabilizados aqui");
        }

        if(type == ExitType.SALE && !salesEnabled){
            throw new ResourceConflictException("Modo de Vendas não está implementado para esta organização");
        }

        if(type == ExitType.WRITE_OFF &&(request.notes() == null || request.notes().isBlank())){
            throw new ResourceConflictException("informe o motivo da baixa");
        }

        if(request.copyIds() == null || request.copyIds().isEmpty()){
            throw new ResourceConflictException("informe ao menos sum exemplar");
        }

        Set<Long> ids = new LinkedHashSet<>(request.copyIds());
        if (ids.size() != request.copyIds().size()) {
            throw new ResourceConflictException("Há exemplares repetidos na lista");
        }

        List<CopyModel> copies = new ArrayList<>();
        copyRepository.findAllById(ids).forEach(copies::add);
        if(copies.size() != ids.size()){
            throw new ResourceNotFoundException("Um ou mais exemplares não existem");
        }

        Map<String, BookModel> books = bookRepository.findByIsbn(
                        copies.stream().map(CopyModel::getIsbn).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(BookModel::getIsbn, b -> b));

        List<ReceiptItem> items = new ArrayList<>();
        for(CopyModel copy : copies){
            if(!CopyStatus.AVAILABLE.name().equals(copy.getStatus())){
                throw new ResourceConflictException("Exemplar "+ copy.getCopy_custom_code() + " não está disponível");
            }

            BookModel book = books.get(copy.getIsbn());
            BigDecimal price = null;
            if(type == ExitType.SALE){
                price = book.getSalePrice();
                if(price == null){
                    throw new ResourceConflictException("O livro "+ book.getTitle() + "não tem preco de venda");
                }
            }

            copy.setStatus(type.resultingStatus().name());
            items.add(new ReceiptItem(copy.getCopyId(), copy.getCopy_custom_code(),book.getIsbn(), book.getTitle(), book.getAuthor(),price));
        }

        copyRepository.saveAll(copies);
        return ReceiptService.issue(type,operator, null, null, null, items, request.notes());
    }
}
