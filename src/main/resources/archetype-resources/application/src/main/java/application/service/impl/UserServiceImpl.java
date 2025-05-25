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
import ${package}.shared.template.ServiceCallback;
import io.github.archetom.common.result.Pager;
import io.github.archetom.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户应用服务实现
 * @author hanfeng
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final EventPublisherHelper eventPublisher;
    private final AbstractOperatorServiceTemplate operatorTemplate;
    private final AbstractQueryServiceTemplate queryTemplate;

    @Override
    @Transactional
    public Result<UserVO> createUser(UserCreateRequest request) {
        return operatorTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<UserVO>() {
            private User user;
            private UserVO userVO;
            
            @Override
            public void checkParam() {
                if (request == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "请求参数不能为空");
                }
            }
            
            @Override
            public void buildContext() {
                // 验证用户创建规则
                userDomainService.validateUserCreation(request.getUsername(), request.getEmail());
                
                // 创建用户实体
                user = User.create(
                        request.getUsername(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getRealName()
                );

                // 加密密码
                String encryptedPassword = userDomainService.encryptPassword(request.getPassword());
                user.setPassword(encryptedPassword);
            }
            
            @Override
            public UserVO process() {
                // 保存用户
                user = userRepository.save(user);
                
                // 转换为VO
                userVO = UserAssembler.toVO(user);
                
                return userVO;
            }
            
            @Override
            public void after() {
                // 发布用户创建事件
                UserCreatedEvent event = new UserCreatedEvent(
                    this, user.getId(), user.getUsername(), user.getEmail()
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
                User user = userRepository.findById(userId);
                if (user == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                }
                
                return UserAssembler.toVO(user);
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
                user = userRepository.findById(userId);
                if (user == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                }
            }
            
            @Override
            public Void process() {
                UserStatus newStatus = UserStatus.fromCode(status);
                user.updateStatus(newStatus);
                userRepository.save(user);
                return null;
            }
            
            @Override
            public void after() {
                log.info("用户状态更新成功，用户ID: {}, 新状态: {}", userId, status);
            }
        });
    }
    
    @Override
    @Transactional
    public Result<Void> deleteUser(Long userId) {
        return operatorTemplate.execute(EventEnum.NOT_SUPPORT_EVENT, new ServiceCallback<Void>() {
            private User user;
            
            @Override
            public void checkParam() {
                if (userId == null || userId <= 0) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户ID不能为空或小于等于0");
                }
            }
            
            @Override
            public void buildContext() {
                user = userRepository.findById(userId);
                if (user == null) {
                    throw new AppException(ErrorCodeEnum.PARAM_CHECK_EXP, "用户不存在");
                }
            }
            
            @Override
            public Void process() {
                user.delete();
                userRepository.save(user);
                return null;
            }
            
            @Override
            public void after() {
                log.info("用户删除成功，用户ID: {}", userId);
            }
        });
    }
}
