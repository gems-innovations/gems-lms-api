package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

/**
 * Deletes the account for good (deactivating is {@link ToggleUserStatusUseCase}). The user's learning
 * data lives in ms-education and is removed there (DELETE /students/{id}/learning-data).
 */
public class DeleteUserUseCase {

    private final UserGateway userGateway;

    public DeleteUserUseCase(UserGateway userGateway) {
        this.userGateway = userGateway;
    }

    public Mono<Void> execute(UserId userId) {
        return userGateway.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        String.format(AuthAppConstants.USER_NOT_FOUND_MESSAGE, userId.getValue())
                )))
                .flatMap(user -> userGateway.deleteById(user.getId()));
    }
}