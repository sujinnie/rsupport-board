package com.rsupport.board.member.domain.entity;

import com.rsupport.board.common.entity.BaseTimeEntity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 사용자 엔티티
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member extends BaseTimeEntity  {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(length = 60, nullable = false)
    private String password;

    @Builder
    public Member(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // todo: 회원기능?
//    public void updateName(String newName) {
//        this.name = newName;
//    }
//
//    public void updatePassword(String newPassword) {
//        this.password = newPassword;
//    }
}
