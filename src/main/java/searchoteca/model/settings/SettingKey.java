package searchoteca.model.settings;

/**
 * Configurações da organização que o root pode mudar pela tela, sem reiniciar o sistema.
 * Cada uma tem um tipo e, se for número, o mínimo e o máximo aceitos.
 *
 * Os valores de segurança (bloqueio de login, MFA, sessão) NÃO entram aqui:
 * ficam só no application.yaml, de propósito.
 */
public enum SettingKey {

    SALES_ENABLED("sales.enabled", Type.BOOLEAN, "Módulo de vendas ativo", 0, 0),
    LOAN_PERIOD_DAYS("loan.period-days", Type.INTEGER, "Prazo do empréstimo (dias)", 1, 90),
    MAX_OPEN_LOANS("loan.max-open-per-student", Type.INTEGER, "Limite de livros emprestados por aluno", 1, 50),
    BLOCK_OVERDUE("loan.block-overdue", Type.BOOLEAN, "Bloquear novo empréstimo de aluno com livro atrasado", 0, 0),
    WAITLIST_MAX_SIZE("waitlist.max-size", Type.INTEGER, "Tamanho da fila prioritária", 1, 20),
    INACTIVITY_DAYS("students.inactivity-days", Type.INTEGER, "Dias sem movimentação até desativar o aluno", 30, 730);

    public enum Type { BOOLEAN, INTEGER }

    private final String key;
    private final Type type;
    private final String label;
    private final int min;
    private final int max;

    SettingKey(String key, Type type, String label, int min, int max) {
        this.key = key;
        this.type = type;
        this.label = label;
        this.min = min;
        this.max = max;
    }

    /** Nome gravado na coluna setting_key. */
    public String key()   { return key; }
    public Type type()    { return type; }
    public String label() { return label; }
    public int min()      { return min; }
    public int max()      { return max; }

    public static SettingKey fromKey(String key) {
        for (SettingKey k : values()) {
            if (k.key.equals(key)) return k;
        }
        return null;   // chave antiga no banco que não existe mais: é ignorada
    }
}
