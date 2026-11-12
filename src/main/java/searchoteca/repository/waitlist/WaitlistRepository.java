package searchoteca.repository.waitlist;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import searchoteca.model.waitingLine.WaitlistModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitlistRepository extends CrudRepository<WaitlistModel, Long> {

    @Query("SELECT isbn FROM org_books WHERE isbn = :isbn FOR UPDATE")
    String lockBook(@Param("isbn") String isbn);

    @Query("SELECT COUNT(*) FROM org_waitlist WHERE isbn = :isbn AND status IN ('WAITING', 'NOTIFIED')")
    long countActive(@Param("isbn") String isbn);

    @Query("SELECT COUNT(*) FROM org_waitlist WHERE isbn = :isbn AND status = 'WAITING'")
    long countWaiting(@Param("isbn") String isbn);

    @Query("""
            SELECT * FROM org_waitlist
            WHERE isbn = :isbn AND student_id = :studentId AND status IN ('WAITING', 'NOTIFIED')
            """)
    Optional<WaitlistModel> findActive(@Param("isbn") String isbn, @Param("studentId") Long studentId);

    /** Próximo da fila: o mais antigo aguardando, ignorando alunos desativados. */
    @Query("""
            SELECT w.* FROM org_waitlist w
            JOIN org_students s ON s.student_id = w.student_id
            JOIN org_users u    ON u.user_id = s.user_id
            WHERE w.isbn = :isbn AND w.status = 'WAITING' AND u.status
            ORDER BY w.created_at, w.waitlist_id
            LIMIT 1
            """)
    Optional<WaitlistModel> findNextWaiting(@Param("isbn") String isbn);

    @Query("SELECT * FROM org_waitlist WHERE copy_id = :copyId AND status = 'NOTIFIED'")
    Optional<WaitlistModel> findReservationByCopy(@Param("copyId") Long copyId);

    @Query("SELECT * FROM org_waitlist WHERE status = 'NOTIFIED' AND expires_at < :now ORDER BY expires_at")
    List<WaitlistModel> findExpired(@Param("now") LocalDateTime now);
}

