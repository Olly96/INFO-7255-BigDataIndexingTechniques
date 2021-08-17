package com.INFO7255.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

@Service
public class JwtUtil {

    private String SECRET_KEY = "secret";
    RSAKey rsaJWK;
    RSAKey rsaPublicJWK;

    JwtUtil() {
        try {
            rsaJWK = new RSAKeyGenerator(2048).keyID("123").generate();
            rsaPublicJWK = rsaJWK.toPublicJWK();

        } catch (Exception e) {

        }
    }

    public String extractUsername(String token) {
        String username = "foo";
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            username = signedJWT.getJWTClaimsSet().getSubject();

        } catch (Exception e) {

        }
        return username;

        // return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser().setSigningKey(SECRET_KEY).parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        String s = "";
        // rsaPublicJWK = rsaJWK.toPublicJWK();
        try {

            // Create RSA-signer with the private key
            JWSSigner signer = new RSASSASigner(rsaJWK);

            // Prepare JWT with claims set
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(subject).issuer("https://c2id.com")
                    .expirationTime(new Date(new Date().getTime() + 60 * 60 * 1000)).build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJWK.getKeyID()).build(), claimsSet);

            // Compute the RSA signature
            signedJWT.sign(signer);

            // To serialize to compact form, produces something like
            // eyJhbGciOiJSUzI1NiJ9.SW4gUlNBIHdlIHRydXN0IQ.IRMQENi4nJyp4er2L
            // mZq3ivwoAjqa1uUkSBKFIX7ATndFF5ivnt-m8uApHO4kfIFOrW7w2Ezmlg3Qd
            // maXlS9DhN0nUk_hGI3amEjkKd0BWYCB8vfUbUv0XGjQip78AI4z1PrFRNidm7
            // -jPDm5Iq0SZnjKjCNS5Q15fokXZc8u0A
            s = signedJWT.serialize();
        } catch (Exception e) {

        }
        return s;

        // return Jwts.builder().setClaims(claims).setSubject(subject).setIssuedAt(new
        // Date(System.currentTimeMillis()))
        // .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 5))
        // .signWith(SignatureAlgorithm.HS256, SECRET_KEY).compact();
    }

    // ---------------------------------------------------------------------
    public Boolean validateToken(String token, UserDetails userDetails) {
        boolean var1 = false, var2 = false, var3 = false, var4 = false;
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
            var4 = signedJWT.verify(verifier);

            // Retrieve / verify the JWT claims according to the app requirements
            // var1 =
            var1 = userDetails.getUsername().equals(signedJWT.getJWTClaimsSet().getSubject());
            var2 = signedJWT.getJWTClaimsSet().getIssuer().equals("https://c2id.com");
            var3 = new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime()) == true;

        } catch (Exception e) {
            return false;
        }
        return var4 && var2 && var3 && var1;
        // final String username = extractUsername(token);
        // return (username.equals(userDetails.getUsername()) &&
        // !isTokenExpired(token));
    }
}