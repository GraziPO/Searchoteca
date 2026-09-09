package searchoteca.v0.service;

import org.springframework.stereotype.Service;

@Service
public class BookService {

    public String Findbook (int isbn){
        //chama a função de Select com base no ID e devolve lista com todas as informações
        return "Livro";
    };

    public void InsertBook(int isbn){
        //chama a api externa do ISBNDB e retorna as informações, envia ela pro banco e retorna vazio
    };

    public void EditBook (int isbn, String old_local, String new_local){
        /*função par alteração da localização do livro. já que as informações chegam via API,
         não é possível alterá-las. */
    };

    public void DeleteBook (int isbn){
        //exclui livro com base no isbn
    };
}
