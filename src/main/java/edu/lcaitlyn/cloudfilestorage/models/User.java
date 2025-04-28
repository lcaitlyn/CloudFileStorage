package edu.lcaitlyn.cloudfilestorage.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NonNull
    @Column(unique = true, nullable = false)
    String username;

    @Column(nullable = false)
    @NonNull
    String password;
}
