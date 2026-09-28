package searchoteca.mfa;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CodeRepository extends CrudRepository<CodeModel, Long> {

    /** Código mais recente que ainda pode ser usado. */
    @Query("SELECT * FROM sys_mfa WHERE user_id = :userId AND used_at IS NULL AND expires_at > :now " +
           "ORDER BY created_at DESC LIMIT 1")
    Optional<CodeModel> findActive(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Último código gerado (usado ou não), para o intervalo mínimo de reenvio. */
    @Query("SELECT * FROM sys_mfa WHERE user_id = :userId ORDER BY created_at DESC LIMIT 1")
    Optional<CodeModel> findLatest(@Param("userId") Long userId);

    @Query("SELECT COUNT(*) FROM sys_mfa WHERE user_id = :userId AND created_at > :since")
    long countCreatedSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /** Invalida todos os códigos pendentes do usuário (ao gerar um novo). */
    @Modifying
    @Query("UPDATE sys_mfa SET used_at = :now WHERE user_id = :userId AND used_at IS NULL")
    int invalidateAll(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    /** Incremento feito no banco, para não perder contagem com requisições simultâneas. */
    @Modifying
    @Query("UPDATE sys_mfa SET attempts = attempts + 1 WHERE id = :id")
    int incrementAttempt(@Param("id") Long id);

    /** Retorna 1 se marcou agora, 0 se outro pedido já tinha usado o código. */
    @Modifying
    @Query("UPDATE sys_mfa SET used_at = :now WHERE id = :id AND used_at IS NULL")
    int usedCode(@Param("id") Long id, @Param("now") LocalDateTime now);
}
