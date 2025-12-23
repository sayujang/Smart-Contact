package com.smartcontact.scm.Helpers;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtHelper {

    // 1. Valid for 5 Hours (milliseconds)
    public static final long JWT_TOKEN_VALIDITY = 5 * 60 * 60 * 1000;

    // 2. Secret Key
    // Must be at least 256 bits (32 chars)
    private final String SECRET = "afafasfafafasfasfasfafacasdasfasxASFACASDFACASDFASFASFDAFASFASDAADSCSDFADCVSGCFVADXCcadwavfsfarvf";
    
    private final SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes());

    // --- Retrieve Username from Token ---
    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // --- Retrieve Expiration Date ---
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);//claims -> claims.getexpiration
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // --- Secret Key is needed here to decrypt the token ---
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder() //prepare jwt reader
        .setSigningKey(key) //use secret key
        .build()
        .parseClaimsJws(token)//verify and decode token
        .getBody();//get body(acutal data)
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // --- GENERATE TOKEN (The "Passport Printer") ---
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return doGenerateToken(claims, username);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)//for custom claims in our case theres none
                .setSubject(subject) // The "User" this token belongs to
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    // --- VALIDATE TOKEN (The "Bouncer") ---
    public Boolean validateToken(String token, String username) {
        final String usernameFromToken = getUsernameFromToken(token);
        return (usernameFromToken.equals(username) && !isTokenExpired(token));
    }
}

//content of jwt token where each key-value is a default claim
// {
//   "sub": "sayuj",
//   "iat": 1700000000,
//   "exp": 1700018000,
//   "role": "ADMIN"
// }
