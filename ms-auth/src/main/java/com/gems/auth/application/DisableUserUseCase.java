package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.domain.constants.UserConstants;
import com.gems.auth.domain.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

public class DisableUserUseCase {

    private final UserGateway userGateway;

    public DisableUserUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public Mono<Void> execute(UserId userId) {
        return userGateway.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        String.format(UserConstants.USER_NOT_FOUND_MESSAGE, userId.getValue())
                )))
                .flatMap(user -> {
                    user.deactivate();
                    return userGateway.save(user).then();
                });
    }
}