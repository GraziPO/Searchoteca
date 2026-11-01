package searchoteca;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")   // usa o H2 do perfil de teste, e não o PostgreSQL do .env
class V0ApplicationTests {

	@Test
	void contextLoads() {
	}

}
