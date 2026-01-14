import com.dv.config.api.impl.auth.encoder.SM3PasswordEncoder;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class ApplicationTests {


    @Test
    public void test(){
        PasswordEncoder passwordEncoder = new SM3PasswordEncoder();
        String encoded = passwordEncoder.encode("74D839D98630E280DF752E8939454A6B");
        log.info("{},{}",encoded,encoded.length());
        boolean matches = passwordEncoder.matches("74D839D98630E280DF752E8939454A6B", encoded);
        log.info("{}",matches);
    }
}
