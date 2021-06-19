package main.repository;

import main.enums.ModerationStatus;
import main.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(id) " +
            "FROM Post " +
            "WHERE moderationStatus = :modStatus AND isActive = :isActive")
    int countPostsForModeration(ModerationStatus modStatus, short isActive);

    Optional<User> findByCode(String code);
}
