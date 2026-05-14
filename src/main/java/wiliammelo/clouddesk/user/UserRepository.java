package wiliammelo.clouddesk.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Optional<User> findByEmailIgnoreCase(String email);

    List<User> findAllByRoleAndActiveTrueOrderByCreatedAtDesc(UserRole role);

    Optional<User> findByIdAndRole(UUID id, UserRole role);
}
