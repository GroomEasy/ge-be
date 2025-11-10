package com.ceos.menual.entity;

import com.ceos.menual.entity.enums.AuthProvider;
import com.ceos.menual.entity.enums.UserType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nickname;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private LocalDate birth; //Dto에서 String으로 받아서 LocalDate(YYYY-MM-DD)로 변환

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;

    @Column(nullable = false)
    private Boolean agreeTerms;

    @Column(nullable = false)
    private Boolean agreePrivacy;
}
