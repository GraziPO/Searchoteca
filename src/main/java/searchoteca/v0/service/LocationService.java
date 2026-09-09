package searchoteca.v0.service;

public class LocationService {
    public String FindLocation (int id){
        //chama a função de Select com base no ID e devolve lista com todas as localizações
        return "Localizaçao";
    };

    public void InsertLocation(int depart_name) {
        //cadastra localização com base no departamento relacionado
    };

    public void EditLocation(int id, String name, String local_dscrp){
        /*função par alteração da localização do livro. já que as informações chegam via API,
         não é possível alterá-las. */
    };

    public void DeleteLocation (int id){
        //exclui localização com base no id. não é possível excluir se não estiver vazio.
    };
}
