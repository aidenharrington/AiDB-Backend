package com.aidb.aidb_backend.security.authorization;

import com.aidb.aidb_backend.exception.http.ForbiddenException;
import com.aidb.aidb_backend.exception.http.UnauthorizedException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAuthService {


    public String authorizeUser(String authToken) {
        try {
            if (authToken == null || !authToken.startsWith("Bearer ")) {
                throw new UnauthorizedException("Unable to authenticate user.");
            }

            String idToken = authToken.substring(7);

            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);

            return decodedToken.getUid();

        } catch (Exception e) {
            throw new ForbiddenException("Error during authentication.");
        }
    }
}
