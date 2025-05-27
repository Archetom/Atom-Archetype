#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
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
import ${package}.domain.messaging.UserCreatedEvent;
import ${package}.domain.repository.UserRepository;
import ${package}.domain.service.UserDomainService;
import ${package}.shared.enums.ErrorCodeEnum;
import ${package}.shared.enums.EventEnum;
import ${package}.shared.event.EventPublisherHelper;
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
 * 用户应用服务实现
 * @author hanfeng
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final EventPublisherHelper eventPublisher;
    private final AbstractOperatorServiceTemplate operatorTemplate;
    private final AbstractQueryServiceTemplate queryTemplate;
    private final UserCacheService userCacheService;
    private final DistributedLock distributedLock;

    public UserServiceImpl(UserRepository userRepository, UserDomainService userDomainService, EventPublisherHelper eventPublisher, AbstractOperatorServiceTemplate operatorTemplate, AbstractQueryServiceTemplate queryTemplate, UserCacheService userCacheService, @Qualifier("redisDistributedLock") DistributedLock distributedLock) {
        this.userRepository = userRepository;
        this.userDomainService = userDomainService;
        this.eventPublisher = eventPublisher;
        this.operatorTemplate = operatorTemplate;
        this.queryTemplate = queryTemplate;
        this.userCacheService = userCacheService;
        this.distributedLock = distributedLock;
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
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "请求参数不能为空");
                }
            }

            @Override
            public void buildContext() {
                // 使用分布式锁防止重复创建用户
                lockKey = "user:create:" + request.getUsername();
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(10))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "用户创建操作正在进行中，请稍后重试");
                }

                try {
                    // 验证用户创建规则
                    userDomainService.validateUserCreation(request.getUsername(), request.getEmail());

                    // 创建用户实体（带手机号）
                    if (request.getPhoneNumber() != null) {
                        user = User.create(
                                request.getUsername(),
                                request.getEmail(),
                                request.getPhoneNumber(),
                                request.getPassword(),
                                request.getRealName()
                        );
                    } else {
                        user = User.create(
                                request.getUsername(),
                                request.getEmail(),
                                request.getPassword(),
                                request.getRealName()
                        );
                    }

                    // 加密密码
                    String encryptedPassword = userDomainService.encryptPassword(request.getPassword());
                    user.setPassword(encryptedPassword);
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public UserVO process() {
                try {
                    // 保存用户
                    user = userRepository.save(user);

                    // 转换为VO
                    userVO = UserAssembler.toVO(user);

                    return userVO;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // 缓存用户信息
                userCacheService.cacheUser(userVO);
                userCacheService.cacheUsernameMapping(user.getUsername(), user.getId());

                // 发布用户创建事件
                UserCreatedEvent event = new UserCreatedEvent(
                        this, user.getId(), user.getUsername(), user.getEmailValue()
                );
                eventPublisher.publish(event);

                log.info("用户创建成功，用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
            }
        });
    }

    @Override
    public Result<UserVO> getUserById(Long userId) {
        return queryTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<UserVO>() {
            @Override
            public void checkParam() {
                if (userId == null || userId <= 0) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户ID不能为空或小于等于0");
                }
            }

            @Override
            public UserVO process() {
                // 先从缓存获取
                UserVO cachedUser = userCacheService.getCachedUser(userId);
                if (cachedUser != null) {
                    return cachedUser;
                }

                // 缓存未命中，从数据库获取
                User user = userRepository.findById(userId);
                if (user == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                }

                UserVO userVO = UserAssembler.toVO(user);

                // 缓存用户信息
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
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "查询请求不能为空");
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
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户ID不能为空或小于等于0");
                }
                if (status == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "状态不能为空");
                }
            }

            @Override
            public void buildContext() {
                // 使用分布式锁防止并发修改
                lockKey = "user:update:" + userId;
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(5))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "用户状态更新操作正在进行中，请稍后重试");
                }

                try {
                    user = userRepository.findById(userId);
                    if (user == null) {
                        throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                    }
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public Void process() {
                try {
                    UserStatus newStatus = UserStatus.fromCode(status);
                    user.updateStatus(newStatus);
                    userRepository.save(user);
                    return null;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // 清除缓存
                userCacheService.evictUser(userId);
                log.info("用户状态更新成功，用户ID: {}, 新状态: {}", userId, status);
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
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户ID不能为空或小于等于0");
                }
            }

            @Override
            public void buildContext() {
                // 使用分布式锁防止并发删除
                lockKey = "user:delete:" + userId;
                if (!distributedLock.tryLock(lockKey, Duration.ofSeconds(5))) {
                    throw new AppException(ErrorCodeEnum.MAIN_TRANS_CONTROL_EXP, "用户删除操作正在进行中，请稍后重试");
                }

                try {
                    user = userRepository.findById(userId);
                    if (user == null) {
                        throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                    }
                } catch (Exception e) {
                    distributedLock.unlock(lockKey);
                    throw e;
                }
            }

            @Override
            public Void process() {
                try {
                    user.delete();
                    userRepository.save(user);
                    return null;
                } finally {
                    distributedLock.unlock(lockKey);
                }
            }

            @Override
            public void after() {
                // 清除缓存
                userCacheService.evictUser(userId);
                log.info("用户删除成功，用户ID: {}", userId);
            }
        });
    }
}
