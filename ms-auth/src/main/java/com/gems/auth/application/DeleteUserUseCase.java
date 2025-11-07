package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

public class DeleteUserUseCase {

    private final UserGateway userGateway;

    public DeleteUserUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public Mono<Void> execute(UserId userId) {
        return userGateway.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        String.format("User with ID %s not found", userId.getValue())
                )))
                .flatMap(user -> userGateway.deleteById(userId));
    }
}