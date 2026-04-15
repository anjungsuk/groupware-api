package com.company.groupware.user;

import com.company.groupware.common.entity.BaseSoftDeleteEntity;
import com.company.groupware.common.security.SystemRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users", indexes = {
        @jakarta.persistence.Index(name = "idx_users_email", columnList = "email", unique = true)
})
public class User extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SystemRole role;

    protected User() {
    }

    public User(String email, String password, String name, SystemRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public SystemRole getRole() { return role; }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
