package br.com.testetecnico.cadastro.service;

import br.com.testetecnico.cadastro.security.AuthProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {

    public record Token(String token, Instant expiraEm) {
    }

    private final AuthProperties props;
    private final PasswordEncoder encoder;
    private final JwtEncoder jwtEncoder;

    public Token autenticar(String usuario, String senha) {
        // `&` (não `&&`) para sempre executar o BCrypt e não revelar, pelo tempo, se o usuário existe
        boolean ok = MessageDigest.isEqual(bytes(usuario), bytes(props.user()))
                & encoder.matches(senha, props.passwordHash());
        if (!ok) {
            throw new BadCredentialsException("Usuário ou senha inválidos");
        }
        Instant agora = Instant.now();
        Instant expira = agora.plus(props.tokenTtl());
        var claims = JwtClaimsSet.builder().subject(usuario).issuedAt(agora).expiresAt(expira).build();
        var cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        String jwt = jwtEncoder.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
        return new Token(jwt, expira);
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }
}
