package searchoteca.auditTrail;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends CrudRepository<AuditLogModel, Long> {
    @Query("SELECT * FROM auditTrail_logs ORDER BY occurred_at DESC LIMIT :limit")
    List<AuditLogModel> findRecent(@Param("limit")int limit);

    @Query("SELECT * FROM auditTrail_logs WHERE username = :username ORDER BY time_stamp DESC LIMIT :limit")
    List<AuditLogModel> findRecentByUsername (@Param("username") String username, @Param("limit") int limit);
}
