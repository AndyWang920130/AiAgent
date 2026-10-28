package com.example.myapp.contants.enumeration;

/**
 * The two role codes the application itself depends on: {@code ADMIN}, which the security
 * rules grant privileged access to, and {@code USER}, the fallback for an account with no
 * role set.
 *
 * <p>This is no longer the full set of roles. {@code User.role} holds the {@code code} of a
 * row in {@code twsny_role_config}, the catalog managed on the Role Config page, so further
 * roles can be added at runtime. These constants exist so the seeding and last-administrator
 * logic can name the built-in codes without string literals.
 */
public enum Role {
    USER,
    ADMIN,
}
