package com.gems.auth.infrastructure.driven.postgresql;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.entities.User;
import com.gems.auth.domain.values.Email;
import com.gems.auth.domain.values.Password;
import com.gems.auth.domain.values.UserId;
import com.gems.auth.domain.values.UserName;
import com.gems.auth.domain.values.UserRole;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

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
    return new User(
      new UserId(userEntity.getUserId()),
      new UserName(userEntity.getName()),
      new Email(userEntity.getEmail()),
      new Password(userEntity.getPassword()),
      UserRole.valueOf(userEntity.getRole()),
      userEntity.getInstitutionId(),
      userEntity.getCreatedAt(),
      userEntity.getUpdatedAt(),
      userEntity.isActive()
    );
  }

  private UserEntity mapToEntity(User user) {
    return new UserEntity(
      user.getId() != null ? user.getId().getValue() : null,
      user.getName().getValue(),
      user.getEmail().getValue(),
      user.getPassword().getValue(),
      user.getRole().name(),
      user.getInstitutionId(),
      user.isActive(),
      user.getCreatedAt(),
      user.getUpdatedAt()
    );
  }
}
