# Política de Privacidade — Searchoteca

**Versão 1.0 — vigente a partir de 27/09/2026**

> **Nota acadêmica.** O Searchoteca é um protótipo desenvolvido como Projeto de Final de Curso na Universidade de Mogi das Cruzes. Em uma implantação real, as decisões descritas aqui (bases legais, prazos de retenção e fornecedores) devem ser validadas pela instituição que operar o sistema e, quando necessário, por assessoria jurídica. Os trechos entre [colchetes] devem ser preenchidos antes da publicação.

## 1. Sobre esta política

O Searchoteca é um sistema de gestão de acervo de biblioteca. Ele permite cadastrar departamentos, locais de armazenamento, livros e exemplares, e controlar quem pode fazer cada operação.

Esta política explica quais dados pessoais o sistema trata, para quê, com quem são compartilhados, por quanto tempo são guardados e como você pode exercer seus direitos. Ela se aplica a todas as pessoas que têm conta no Searchoteca.

O sistema é de uso interno: as contas são criadas por um administrador da instituição, e não há cadastro aberto ao público.

## 2. Quem é responsável pelos seus dados

| Papel | Quem é | O que faz |
|---|---|---|
| **Controlador** | [NOME DA INSTITUIÇÃO QUE OPERA A BIBLIOTECA], CNPJ [XX.XXX.XXX/XXXX-XX] | Decide como e por que os dados são tratados. No protótipo, é a instituição (escola, universidade ou empresa) que adotaria o sistema. |
| **Operador — envio de e-mails** | Resend (Resend, Inc.) | Entrega o e-mail com o código de verificação em duas etapas. |
| **Operador — hospedagem** | [PROVEDOR DE HOSPEDAGEM E BANCO DE DADOS] | Hospeda a aplicação e o banco de dados. |
| **Encarregado (DPO)** | [NOME DO ENCARREGADO] | Canal de contato para assuntos de privacidade: **[privacidade@dominio.com.br]** |

## 3. Quais dados tratamos

Coletamos apenas o necessário para dar acesso ao sistema e mantê-lo seguro. Todos os dados abaixo são fornecidos pelo administrador que cria a sua conta ou gerados automaticamente quando você usa o sistema.

| Categoria | Dados | Origem | Obrigatório? |
|---|---|---|---|
| **Identificação e conta** | Nome completo, nome de usuário, identificador interno (ex.: matrícula), e-mail, perfil de acesso e situação da conta (ativa ou desativada) | Cadastro feito pelo administrador | Sim; sem eles não é possível criar a conta |
| **Credencial** | Senha, guardada apenas em formato de hash irreversível (bcrypt). Ninguém, nem os administradores, consegue ver a sua senha | Definida no cadastro | Sim |
| **Verificação em duas etapas** | Código de 6 dígitos (guardado apenas como hash), horário de envio, de expiração e de uso, e número de tentativas | Gerado pelo sistema a cada login | Sim, quando a verificação em duas etapas está ativa |
| **Registros de auditoria** | Nome de usuário, data e hora, ação realizada (login, tentativa de login, criação, alteração, exclusão, acesso negado) e descrição da ação, como o registro afetado e os campos alterados | Gerado pelo sistema | Automático |
| **Registros técnicos** | Data e hora, nome de usuário, rota acessada, resultado da requisição, tempo de resposta e um identificador da requisição | Gerado pelo sistema | Automático |
| **Token de acesso** | Nome de usuário, identificador e perfil, com prazo de validade | Gerado no login e guardado no seu dispositivo | Sim, para manter a sessão |

**O que não coletamos:** CPF, endereço, telefone, data de nascimento, fotografia, localização, endereço IP em registros de auditoria nem qualquer dado pessoal sensível (saúde, biometria, origem racial, religião, opinião política etc.). O sistema não usa inteligência artificial para processar os seus dados.

## 4. Para que usamos os dados e com qual base legal

Não usamos o consentimento como base para o funcionamento do sistema. Cada uso tem a sua base legal própria, prevista no artigo 7º da LGPD.

| Finalidade | Dados usados | Base legal |
|---|---|---|
| Criar e manter a sua conta, identificar você e controlar o que o seu perfil pode fazer | Identificação e conta, credencial, token de acesso | Execução de contrato ou de procedimentos ligados à sua relação com a instituição (art. 7º, V) |
| Confirmar a sua identidade no login, enviando um código de verificação ao seu e-mail | E-mail, nome, código de verificação | Legítimo interesse na segurança da conta (art. 7º, IX) |
| Registrar quem fez cada alteração no acervo e nos usuários, investigar incidentes e comprovar ações realizadas | Registros de auditoria | Legítimo interesse (art. 7º, IX) e exercício regular de direitos (art. 7º, VI) |
| Diagnosticar erros e falhas técnicas | Registros técnicos | Legítimo interesse (art. 7º, IX) |

**Sobre o uso do legítimo interesse.** A verificação em duas etapas e os registros de auditoria existem para proteger a sua conta e o acervo da instituição contra acessos indevidos. São medidas esperadas em um sistema interno com diferentes níveis de permissão. Para limitar o impacto sobre você:

- os registros guardam apenas o necessário para identificar a ação;
- códigos de verificação e senhas nunca são gravados em registros;
- o e-mail aparece mascarado nos registros de auditoria (ex.: `g*******e@dominio.com`);
- os registros têm prazo de exclusão definido (seção 9);
- apenas perfis autorizados consultam a auditoria.

Você pode se opor a esses tratamentos pelo canal da seção 10. A oposição será analisada, mas alguns registros podem ser mantidos quando forem indispensáveis à segurança do sistema.

## 5. Quem tem acesso aos seus dados

O acesso é verificado pelo servidor em cada operação, conforme o perfil de cada usuário, e não apenas pela interface.

| Perfil | Acesso a dados pessoais de usuários |
|---|---|
| **Root** | Acesso total, restrito aos responsáveis técnicos pela administração do sistema |
| **Administrador de Usuários** | Consulta, cadastra, altera, ativa e desativa contas |
| **Operador de Acervo** | Nenhum; gerencia apenas departamentos, locais e livros |
| **Operador de Empréstimo** | Nenhum; consulta o acervo e altera quantidades |
| **Perfis com permissão de auditoria** | Consultam os registros de auditoria |

## 6. Com quem compartilhamos

Não vendemos, alugamos nem usamos seus dados para publicidade. Os dados são compartilhados apenas com:

- **Resend**, para enviar o e-mail com o código de verificação. Recebe somente o seu e-mail, o seu nome e o código.
- **[Provedor de hospedagem]**, que armazena a aplicação e o banco de dados.
- **Autoridades públicas**, quando houver obrigação legal ou ordem judicial.

## 7. Transferência internacional

O Resend é uma empresa sediada nos Estados Unidos, e o envio dos e-mails pode ser processado fora do Brasil. Para reduzir riscos:

- enviamos a esse fornecedor apenas o mínimo necessário (e-mail, nome e um código válido por poucos minutos);
- a comunicação é feita por conexão criptografada;
- a transferência se apoia nos mecanismos do artigo 33 da LGPD, como cláusulas contratuais de proteção de dados. [Confirmar o acordo de tratamento de dados (DPA) com o fornecedor.]

[Se a hospedagem ficar fora do Brasil, informar aqui o país e as salvaguardas.]

## 8. Cookies e armazenamento no dispositivo

O Searchoteca não usa cookies de publicidade, de análise de comportamento nem de terceiros. Para manter você conectado, o aplicativo guarda o token de acesso no seu dispositivo durante a sessão. Esse armazenamento é estritamente necessário e não pode ser desativado sem impedir o login. [Confirmar com a implementação do frontend.]

## 9. Por quanto tempo guardamos os dados

| Dado | Prazo | O que acontece depois |
|---|---|---|
| Conta ativa | Enquanto você precisar de acesso ao sistema | — |
| Conta desativada | [6 meses] após a desativação, para apurar eventuais incidentes | Os dados de identificação são excluídos ou anonimizados |
| Senha | Enquanto a conta existir; a anterior é descartada a cada troca | Excluída com a conta |
| Código de verificação | Válido por 10 minutos; o registro é mantido por [7 dias] para controle de tentativas | Excluído |
| Token de acesso | Expira em 1 hora; o token intermediário da verificação em duas etapas expira em 15 minutos | Deixa de funcionar; não fica guardado no servidor |
| Registros de auditoria | [12 meses] | Excluídos com segurança |
| Registros técnicos | 30 dias, com rotação automática | Excluídos automaticamente |
| Cópias de segurança | [Prazo e critério dos backups] | Sobrescritas ou excluídas |
| Dados de demonstração do protótipo | Somente durante o projeto | Excluídos ao final |

Os dados usados em testes e demonstrações do protótipo são fictícios.

## 10. Seus direitos

A LGPD garante a você, entre outros, os direitos de:

- confirmar se tratamos seus dados e acessá-los;
- corrigir dados incompletos, inexatos ou desatualizados;
- saber com quem seus dados foram compartilhados;
- pedir anonimização, bloqueio ou eliminação de dados desnecessários ou tratados em desconformidade com a lei;
- receber seus dados em formato estruturado (portabilidade), quando aplicável;
- opor-se a tratamentos baseados em legítimo interesse;
- pedir revisão de decisões tomadas unicamente de forma automatizada, como o bloqueio da verificação após tentativas incorretas.

**Como exercer:**

1. Envie o pedido para **[privacidade@dominio.com.br]**, informando o seu nome de usuário.
2. Podemos pedir uma confirmação para garantir que o pedido é seu.
3. Um administrador executa o pedido no sistema, e o atendimento fica registrado.
4. Responderemos em até 15 dias.

Alguns dados podem ser mantidos mesmo após um pedido de exclusão quando a lei permitir, por exemplo registros de auditoria necessários para apurar um incidente ou para defesa em processo. Nesse caso, explicaremos o motivo.

## 11. Como protegemos os dados

- senhas guardadas apenas com hash forte (bcrypt);
- verificação em duas etapas por e-mail no login;
- tokens de acesso com prazo de validade;
- controle de permissões verificado pelo servidor em cada operação;
- registro das ações relevantes em trilha de auditoria;
- limite de tentativas na verificação de identidade;
- chaves e credenciais mantidas fora do código-fonte;
- conexão criptografada (HTTPS) em produção.

Nenhum sistema é totalmente imune a falhas, mas revisamos essas medidas continuamente.

## 12. Incidentes de segurança

Se houver suspeita de incidente com dados pessoais, a instituição:

1. confirma o incidente;
2. contém o problema e preserva as evidências;
3. identifica os dados e as pessoas afetadas;
4. avalia o risco;
5. comunica a Autoridade Nacional de Proteção de Dados (ANPD) e as pessoas afetadas quando houver risco ou dano relevante, no prazo previsto na regulamentação da ANPD;
6. corrige a causa e registra as medidas adotadas.

Se você notar algo suspeito, como um código de verificação que não pediu, avise imediatamente pelo canal da seção 10 e troque a sua senha.

## 13. Alterações nesta política

Esta política pode ser atualizada quando o sistema mudar. A versão e a data ficam sempre no topo do documento. Mudanças importantes serão avisadas dentro do aplicativo antes de entrarem em vigor.

## 14. Contato

- **Encarregado:** [NOME]
- **E-mail:** [privacidade@dominio.com.br]
- **Controlador:** [NOME DA INSTITUIÇÃO], [endereço]
