package searchoteca.v0.service;

import org.springframework.stereotype.Service;

@Service
public class DepartmentService {
    public String FindDepartment (int isbn){
        //chama a função de Select com base no ID e devolve lista com todas as informações
        return "Departamentos";
    };

    public void InsertDepartment(int isbn){
        //cadastra Departamento com base nas informações passadas no Body
    };

    public void EditDepartment (int id, String name, String depart_dscrp){
        // função par alteração dos detalhes do departamento
    };

    public void DeleteDepartment (int id){
        //exclui departamento com base no id caso este esteja vazio. Caso esteja populado, não é possível deletar.
    };
}
