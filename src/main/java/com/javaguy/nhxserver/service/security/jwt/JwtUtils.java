package com.javaguy.nhxserver.service.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    private final Logger logger = LoggerFactory.getLogger(JwtUtils.class);
    @Value( "${nhx.app.jwtSecret}")
    private String secretKey;
    @Value( "${nhx.app.jwtExpirationMs}")
    private long jwtExpirationTime;
    @Value( "${nhx.app.jwtRefreshExpirationMs}")
    private long refreshExpirationTime;
    @Value( "${nhx.app.jwtCookieName}")
    private String jwtCookieName;
    @Value( "${nhx.app.jwtRefreshCookieName}")
    private String jwtRefreshCookieName;
    private final String cookiePath = "/";
    private final Environment environment;

    public JwtUtils(Environment environment) {
        this.environment = environment;
    }

    public long getJwtExpirationTimeMs() {
        return jwtExpirationTime;
    }

    public long getRefreshExpirationTimeMs() {
        return refreshExpirationTime;
    }

    public String getJwtCookieName() {
        return jwtCookieName;
    }

    public String getJwtRefreshCookieName() {
        return jwtRefreshCookieName;
    }

    //generate the token with custom claims
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername(), jwtExpirationTime);
    }
    //generate refresh token
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return createToken(claims, userDetails.getUsername(), refreshExpirationTime);
    }
    //create token with claims
    private String createToken(Map<String, Object> claims, String subject, long expirationTime){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    //extract username from token
    public String getUsernameFromToken(String token){
        return getClaimsFromToken(token, Claims::getSubject);
    }
    //expiration date
    public Date getExpirationDateFromToken(String token){
        return getClaimsFromToken(token, Claims::getExpiration);
    }
    //specific claim
    public <T> T getClaimsFromToken(String token, Function<Claims, T> claimsResolver){
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }
    //all claims
    private Claims getAllClaimsFromToken(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
    //check if token is expired
    public boolean isTokenExpired(String token){
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }
    //validate token against user details
    public boolean validateToken(String token, UserDetails userDetails){
        try{
            final String username = getUsernameFromToken(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }catch (Exception e){
            logger.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        }
    }
    /**
     * Validate token structure and signature
     */
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    //generate jwt cookie
    public ResponseCookie generateJwtCookie(UserDetails userDetails){
        String jwt = generateToken(userDetails);
        return generateCookie(jwtCookieName, jwt,  jwtExpirationTime);
    }

    private ResponseCookie generateCookie(String cookieName, String value, long maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
                .maxAge(maxAge / 1000)
                .httpOnly(true)
                .path(cookiePath)
                .sameSite("Strict");

        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")) {
            builder.secure(true);
            builder.domain("https://nhxwebserver-hzbrdehsdffqhugw.southafricanorth-01.azurewebsites.net/");
        }
        return builder.build();
    }
    public ResponseCookie generateJwtCookie(String jwt) {
        return generateCookie(jwtCookieName, jwt, jwtExpirationTime);
    }
    public ResponseCookie getCleanJwtRefreshCookie() {
        return generateCookie(jwtRefreshCookieName, "", 0L);
    }
    public ResponseCookie generateJwtRefreshCookie(String refreshToken) {
        return generateCookie(jwtRefreshCookieName, refreshToken, refreshExpirationTime);
    }
    public ResponseCookie getClean1JwtCookie() {
        return generateCookie(jwtCookieName, "", 0L);
    }
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
