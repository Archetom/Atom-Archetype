package ${package}.application.service.impl;

import ${package}.api.dto.request.UserCreateRequest;
import ${package}.api.dto.request.UserQueryRequest;
import ${package}.api.enums.UserStatus;
import ${package}.application.assembler.UserAssembler;
import ${package}.application.service.UserService;
import ${package}.application.service.template.AbstractOperatorServiceTemplate;
import ${package}.application.service.template.AbstractQueryServiceTemplate;
import ${package}.application.vo.UserVO;
import ${package}.domain.entity.User;
import ${package}.domain.event.DomainEventPublisher;
import ${package}.domain.exception.UserNotFoundException;
import ${package}.domain.factory.UserFactory;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.UserDomainService;
import ${package}.domain.valueobject.UserId;
import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.enums.EventEnum;
import ${package}.shared.exception.AppException;
import ${package}.shared.lock.DistributedLock;
import ${package}.shared.template.ServiceCallback;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * user application service implementation
 * @author hanfeng
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final DomainEventPublisher domainEventPublisher;
    private final AbstractOperatorServiceTemplate operatorTemplate;
    private final AbstractQueryServiceTemplate queryTemplate;
    private final UserCacheService userCacheService;
    private final DistributedLock distributedLock;
    private final UserFactory userFactory;

    public UserServiceImpl(UserRepository userRepository,
                           UserDomainService userDomainService,
                           DomainEventPublisher domainEventPublisher,
                           AbstractOperatorServiceTemplate operatorTemplate,
                           AbstractQueryServiceTemplate queryTemplate,
                           UserCacheService userCacheService,
                           @Qualifier("redisDistributedLock") DistributedLock distributedLock,
                           UserFactory userFactory) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.domainEventPublisher = domainEventPublisher;
        this.operatorTemplate = operatorTemplate;
        this.queryTemplate = queryTemplate;
        this.userCacheService = userCacheService;
        this.distributedLock = distributedLock;
        this.userFactory = userFactory;
    }

    @Override
    @Transactional
    public Result<UserVO> createUser(UserCreateRequest request) {
        return operatorTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<UserVO>() {
            private User user;
            private UserVO userVO;
            private String lockKey;

            @Override
            public void checkParam() {
                if (request == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "Request parameters must not be empty");
                }
            }

            @Override
            public void buildContext() {
                // distributed lock create user
                lockKey = "user:create:" + request.getUsername();
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(10))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "User creation is in progress, Please try again later");
                }

                try {
                    // create user entity
                    if (request.getPhoneNumber() != null) {
                        user = userFactory.createUserWithPhone(
                                request.getUsername(),
                                request.getEmail(),
                                request.getPhoneNumber(),
                                request.getPassword(),
                                request.getRealName()
                        );
                    } else {
                        user = userFactory.createStandardUser(
                                request.getUsername(),
                                request.getEmail(),
                                request.getPassword(),
                                request.getRealName()
                        );
                    }
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public UserVO process() {
                try {
                    // save user
                    user = userRepository.save(user);

                    // convert as VO
                    userVO = UserAssembler.toVO(user);

                    return userVO;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // cache user
                userCacheService.cacheUser(userVO);
                if (user.getId() != null) {
                    userCacheService.cacheUsernameMapping(user.getUsernameValue(), user.getId().getValue());
                }

                // publish domain event
                if (user.hasDomainEvents()) {
                    domainEventPublisher.publishAll(user.getDomainEvents());
                    user.clearDomainEvents();
                }

                log.info("User created successfully, user ID: {}, username: {}",
                        user.getId() != null ? user.getId().getValue() : null,
                        user.getUsernameValue());
            }
        });
    }

    @Override
    public Result<UserVO> getUserById(Long userId) {
        return queryTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<UserVO>() {
            @Override
            public void checkParam() {
                if (userId == null || userId <= 0) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, " user ID must not be empty or in etc. in 0");
                }
            }

            @Override
            public UserVO process() {
                // first from cache get
                UserVO cachedUser = userCacheService.getCachedUser(userId);
                if (cachedUser != null) {
                    return cachedUser;
                }

                // cache miss, from database get
                User user = userRepository.findById(new UserId(userId))
                        .orElseThrow(() -> new UserNotFoundException(userId));

                UserVO userVO = UserAssembler.toVO(user);

                // cache user
                userCacheService.cacheUser(userVO);

                return userVO;
            }
        });
    }

    @Override
    public Result<Pager<UserVO>> queryUsers(UserQueryRequest request) {
        return queryTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<Pager<UserVO>>() {
            @Override
            public void checkParam() {
                if (request == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "Query request must not be empty");
                }
                if (request.getPage() == null || request.getPage() < 1) {
                    request.setPage(1);
                }
                if (request.getSize() == null || request.getSize() < 1) {
                    request.setSize(20);
                }
            }

            @Override
            public Pager<UserVO> process() {
                UserStatus status = null;
                if (request.getStatus() != null) {
                    status = UserStatus.fromCode(request.getStatus());
                }

                Pager<User> userPager = userRepository.findUsers(
                        request.getUsername(),
                        request.getEmail(),
                        status,
                        request.getPage(),
                        request.getSize()
                );

                return UserAssembler.toVOPager(userPager);
            }
        });
    }

    @Override
    @Transactional
    public Result<Void> updateUserStatus(Long userId, String status) {
        return operatorTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<Void>() {
            private User user;
            private String lockKey;

            @Override
            public void checkParam() {
                if (userId == null || userId <= 0) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, " user ID must not be empty or in etc. in 0");
                }
                if (status == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "Status must not be empty");
                }
            }

            @Override
            public void buildContext() {
                // distributed lock
                lockKey = "user:update:" + userId;
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(5))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "User status update is in progress, Please try again later");
                }

                try {
                    user = userRepository.findById(new UserId(userId))
                            .orElseThrow(() -> new UserNotFoundException(userId));
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public Void process() {
                try {
                    UserStatus newStatus = UserStatus.fromCode(status);
                    user.changeStatus(newStatus, " administrator ");
                    userRepository.save(user);
                    return null;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // clear cache
                userCacheService.evictUser(userId);

                // publish domain event
                if (user.hasDomainEvents()) {
                    domainEventPublisher.publishAll(user.getDomainEvents());
                    user.clearDomainEvents();
                }

                log.info("User status updated successfully, user ID: {}, new status: {}", userId, status);
            }
        });
    }

    @Override
    @Transactional
    public Result<Void> deleteUser(Long userId) {
        return operatorTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<Void>() {
            private User user;
            private String lockKey;

            @Override
            public void checkParam() {
                if (userId == null || userId <= 0) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, " user ID must not be empty or in etc. in 0");
                }
            }

            @Override
            public void buildContext() {
                // distributed lock delete
                lockKey = "user:delete:" + userId;
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(5))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "User deletion is in progress, Please try again later");
                }

                try {
                    user = userRepository.findById(new UserId(userId))
                            .orElseThrow(() -> new UserNotFoundException(userId));
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public Void process() {
                try {
                    // check whether can delete
                    if (!userDomainService.canDeleteUser(user)) {
                        throw new AppException(ErrorCodeEnum.NOT_SUPPORT_OPERATE_EXP, "This user cannot be deleted");
                    }

                    user.delete();
                    userRepository.save(user);
                    return null;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // clear cache
                userCacheService.evictUser(userId);

                // publish domain event
                if (user.hasDomainEvents()) {
                    domainEventPublisher.publishAll(user.getDomainEvents());
                    user.clearDomainEvents();
                }

                log.info("User deleted successfully, user ID: {}", userId);
            }
        });
    }
}
