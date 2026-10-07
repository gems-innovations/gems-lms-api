package com.gems.auth.application;

import com.gems.auth.application.gateway.UserGateway;
import com.gems.auth.application.gateway.LearningDataRemovalGateway;
import com.gems.auth.application.constants.AuthAppConstants;
import com.gems.auth.application.exceptions.UserNotFoundException;
import com.gems.auth.domain.values.UserId;
import reactor.core.publisher.Mono;

/**
 * Deletes the account for good (deactivating is {@link ToggleUserStatusUseCase}). The user's learning
 * data is removed in ms-education first; a failed cleanup leaves the account available for retry.
 */
public class DeleteUserUseCase {

    private final UserGateway userGateway;
    private final LearningDataRemovalGateway learningData;

    public DeleteUserUseCase(UserGateway userGateway, LearningDataRemovalGateway learningData) {
        this.userGateway = userGateway;
        this.learningData = learningData;
    }

    public Mono<Void> execute(UserId userId) {
        return userGateway.findById(userId)
                .switchIfEmpty(Mono.error(new UserNotFoundException(
                        String.format(AuthAppConstants.USER_NOT_FOUND_MESSAGE, userId.getValue())
                )))
                .flatMap(user -> learningData.remove(user.getId())
                  .then(Mono.defer(() -> userGateway.deleteById(user.getId()))));
    }
}
