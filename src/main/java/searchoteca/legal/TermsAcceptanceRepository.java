package searchoteca.legal;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TermsAcceptanceRepository extends CrudRepository<TermsAcceptanceModel, Long> {

    @Query("SELECT COUNT(*) FROM sys_terms_acceptance " +
           "WHERE user_id = :userId AND terms_version = :termsVersion AND privacy_version = :privacyVersion")
    long countAccepted(@Param("userId") Long userId,
                       @Param("termsVersion") String termsVersion,
                       @Param("privacyVersion") String privacyVersion);
}
