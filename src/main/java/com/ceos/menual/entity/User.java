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
    private UserType userType; // EXPERT, MEMBER, TMP_USER

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    //프로필이미지
    private String profileImage;

    // 회원 프로필
    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private GeneralProfile generalProfile;

    // 전문가 프로필 (회원은 null)
    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private ExpertProfile expertProfile; // 전문가만 존재

    @Column
    private String providerId;

    @Column(nullable = false)
    private Boolean agreeTerms;

    @Column(nullable = false)
    private Boolean agreePrivacy;

    public void updateSocialExtraInfo(String nickname, LocalDate birth, String email, Boolean agreeTerms, Boolean agreePrivacy) {
        this.nickname = nickname;
        this.birth = birth;
        this.email = email;
        this.agreeTerms = agreeTerms;
        this.agreePrivacy = agreePrivacy;

        if(this.userType == UserType.TMP_USER){
            this.userType = UserType.MEMBER;
        }
    }

    public boolean isExpert() {
        return this.userType == UserType.EXPERT && this.expertProfile != null;
    }

    public void updateProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public void convertToExpert(ExpertProfile expertProfile) {
        this.userType = UserType.EXPERT;
        this.expertProfile = expertProfile;
    }
}

