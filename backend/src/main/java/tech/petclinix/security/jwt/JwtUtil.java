package tech.petclinix.security.jwt;

import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tech.petclinix.persistence.entity.AdminEntity;
import tech.petclinix.persistence.entity.OwnerEntity;
import tech.petclinix.persistence.entity.UserEntity;
import tech.petclinix.persistence.entity.UserEntity.UserVisitor;
import tech.petclinix.persistence.entity.VetEntity;

import java.util.Collections;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expirationMs}")
    private long jwtExpirationMs;

    public String generateToken(UserEntity userEntity) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(userEntity.getUsername())
                .claim("username", userEntity.getUsername())
                .claim("scope", toRole(userEntity))
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(SignatureAlgorithm.HS256, jwtSecret)
                .compact();
    }

    private static String toRole(UserEntity userEntity) {
        return userEntity.accept(new UserVisitor<>() {
            @Override
            public String visitOwner(OwnerEntity owner) {
                return "OWNER";
            }

            @Override
            public String visitVet(VetEntity vet) {
                return "VET";
            }

            @Override
            public String visitAdmin(AdminEntity admin) {
                return "ADMIN";
            }
        });
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token);
            return true;
        } catch (SignatureException | MalformedJwtException | ExpiredJwtException |
                 UnsupportedJwtException | IllegalArgumentException ex) {
            return false;
        }
    }
}
