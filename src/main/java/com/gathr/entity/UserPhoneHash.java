package com.gathr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "user_phone_hash", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "phone_hash"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPhoneHash {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "phone_hash", nullable = false, length = 128)
    private String phoneHash;
}

