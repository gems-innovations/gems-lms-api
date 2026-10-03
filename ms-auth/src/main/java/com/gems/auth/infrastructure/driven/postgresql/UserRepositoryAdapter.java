package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Repository
public class UserRepositoryAdapter implements UserGateway {
  private final IUserRepository userRepository;

  public UserRepositoryAdapter(IUserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public Mono<User> save(User user) {
    UserEntity userEntity = mapToEntity(user);
    return userRepository.save(userEntity)
      .map(this::mapToDomain);
  }

  @Override
  public Mono<User> findById(UserId id) {
    return userRepository.findById(id.getValue())
      .map(this::mapToDomain);
  }

  @Override
  public Mono<User> findByEmail(Email email) {
    return userRepository.findByEmail(email.getValue())
      .map(this::mapToDomain);
  }

  @Override
  public Mono<Boolean> existsByEmail(Email email) {
    return userRepository.existsByEmail(email.getValue());
  }

  @Override
  public Mono<Boolean> existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  @Override
  public Mono<Void> deleteById(UserId id) {
    return userRepository.deleteById(id.getValue());
  }

  @Override
  public reactor.core.publisher.Flux<User> findByInstitutionId(String institutionId) {
    return userRepository.findByInstitutionId(institutionId)
      .map(this::mapToDomain);
  }

  @Override
  public reactor.core.publisher.Flux<User> findAll() {
    return userRepository.findAll()
      .map(this::mapToDomain);
  }

  private User mapToDomain(UserEntity userEntity) {
    User user = new User(
      new UserId(userEntity.getUserId()),
      new UserName(userEntity.getFirstName()),
      new UserName(userEntity.getLastName()),
      userEntity.getUsername(),
      new Email(userEntity.getEmail()),
      new Password(userEntity.getPassword()),
      UserRole.valueOf(userEntity.getRole()),
      userEntity.getInstitutionId(),
      userEntity.getAvatarUrl(),
      userEntity.getCreatedAt(),
      userEntity.getUpdatedAt(),
      userEntity.isActive()
    );
    user.setMustChangePassword(Boolean.TRUE.equals(userEntity.getMustChangePassword()));
    return user;
  }

  private UserEntity mapToEntity(User user) {
    UserEntity entity = new UserEntity(
      user.getId() != null ? user.getId().getValue() : null,
      user.getFirstName().getValue(),
      user.getLastName().getValue(),
      user.getUsername(),
      user.getEmail().getValue(),
      user.getPassword().getValue(),
      user.getRole().name(),
      user.getInstitutionId(),
      user.getAvatarUrl(),
      user.isActive(),
      user.getCreatedAt(),
      user.getUpdatedAt()
    );
    entity.setMustChangePassword(user.mustChangePassword());
    return entity;
  }

  @Override
  public Mono<Map<String, Long>> countActiveUsersByInstitution() {
    return userRepository.countActiveByInstitution()
      .collectMap(InstitutionUserCount::institutionId, InstitutionUserCount::total);
  }

  @Override
  public Mono<Void> updatePassword(UserId id, String encodedPassword) {
    return userRepository.updatePassword(id.getValue(), encodedPassword).then();
  }

  @Override
  public Flux<User> searchByInstitution(String institutionId, String search, int limit, long offset) {
    return userRepository.searchByInstitution(institutionId, pattern(search), limit, offset).map(this::mapToDomain);
  }

  @Override
  public Mono<Long> countByInstitution(String institutionId, String search) {
    return userRepository.countSearchByInstitution(institutionId, pattern(search));
  }

  /** ILIKE pattern for a free-text search ('' = no filter); % and _ in the text are literal. */
  private static String pattern(String search) {
    if (search == null || search.isBlank()) return "";
    return "%" + search.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
  }
}
