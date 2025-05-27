#!/bin/bash

# 清除生成项目中的示例代码
echo "正在清除项目示例代码..."

# 检查是否在正确的项目目录中
if [ ! -f "pom.xml" ]; then
    echo "错误：请在项目根目录中运行此脚本"
    exit 1
fi

# 获取项目的包路径
PACKAGE_PATH=$(find . -name "Bootstrap.java" | head -1 | sed 's|/Bootstrap.java||' | sed 's|./start/src/main/java/||')

if [ -z "$PACKAGE_PATH" ]; then
    echo "错误：无法找到项目包路径"
    exit 1
fi

echo "检测到包路径: $PACKAGE_PATH"

# =============================================================================
# API 层示例代码
# =============================================================================
echo "清理 API 层示例代码..."
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserQueryRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserCreateRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/response/UserResponse.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/enums/UserStatus.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/facade/UserFacade.java

# =============================================================================
# Domain 层示例代码
# =============================================================================
echo "清理 Domain 层示例代码..."
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/entity/User.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/repository/UserRepository.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/UserDomainService.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/impl/UserDomainServiceImpl.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/messaging/UserCreatedEvent.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/event/UserCreatedEvent.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/event/UserStatusChangedEvent.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/exception/UserAlreadyExistsException.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/exception/UserDomainException.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/exception/UserNotFoundException.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/external/EmailService.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/external/SmsService.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/factory/UserFactory.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/policy/PasswordPolicy.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/policy/UserCreationPolicy.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/policy/UserStatusPolicy.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/specification/UserSpecification.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/valueobject/Address.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/valueobject/Email.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/valueobject/PhoneNumber.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/valueobject/UserId.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/valueobject/Username.java

# =============================================================================
# Application 层示例代码
# =============================================================================
echo "清理 Application 层示例代码..."
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/UserService.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/impl/UserServiceImpl.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/impl/UserCacheService.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/vo/UserVO.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/assembler/UserAssembler.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/converter/UserConverter.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/event/DomainEventPublisherImpl.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/event/listener/UserEventListener.java

# =============================================================================
# Infrastructure 层示例代码
# =============================================================================
echo "清理 Infrastructure 层示例代码..."

# Persistence
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/converter/UserPOConverter.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/po/UserPO.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/mapper/UserMapper.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao/UserDao.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao/impl/UserDaoImpl.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/repository/UserRepositoryImpl.java
rm -f infra/persistence/src/main/resources/mapper/UserMapper.xml

# Rest
rm -f infra/rest/src/main/java/${PACKAGE_PATH}/infra/rest/controller/UserController.java

# RPC
rm -f infra/rpc/src/main/java/${PACKAGE_PATH}/infra/rpc/UserFacadeImpl.java

# External
rm -f infra/external/src/main/java/${PACKAGE_PATH}/infra/external/EmailServiceImpl.java
rm -f infra/external/src/main/java/${PACKAGE_PATH}/infra/external/SmsServiceImpl.java

# Messaging
rm -f infra/messaging/src/main/java/${PACKAGE_PATH}/infra/messaging/UserEventPublisher.java

# =============================================================================
# 数据库脚本和配置文件（保留基础配置，删除示例数据）
# =============================================================================
echo "清理数据库脚本和示例配置..."
rm -f infra/persistence/src/main/resources/sql/schema.sql
rm -f docker/mysql/init/01-init-database.sql

# =============================================================================
# 测试代码
# =============================================================================
echo "清理测试代码..."
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/entity/UserTest.java
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/specification/UserSpecificationTest.java
rm -f start/src/test/java/${PACKAGE_PATH}/UserControllerIntegrationTest.java
rm -f start/src/test/resources/sql/init-test-data.sql

# =============================================================================
# 清理空目录（但保留重要的目录结构）
# =============================================================================
echo "清理空目录..."

# 定义需要保留的重要目录
KEEP_DIRS=(
    "api/src/main/java/${PACKAGE_PATH}/api/dto/request"
    "api/src/main/java/${PACKAGE_PATH}/api/dto/response"
    "api/src/main/java/${PACKAGE_PATH}/api/enums"
    "api/src/main/java/${PACKAGE_PATH}/api/facade"
    "domain/src/main/java/${PACKAGE_PATH}/domain/entity"
    "domain/src/main/java/${PACKAGE_PATH}/domain/repository"
    "domain/src/main/java/${PACKAGE_PATH}/domain/service"
    "domain/src/main/java/${PACKAGE_PATH}/domain/service/impl"
    "domain/src/main/java/${PACKAGE_PATH}/domain/valueobject"
    "domain/src/main/java/${PACKAGE_PATH}/domain/event"
    "domain/src/main/java/${PACKAGE_PATH}/domain/factory"
    "domain/src/main/java/${PACKAGE_PATH}/domain/policy"
    "domain/src/main/java/${PACKAGE_PATH}/domain/specification"
    "application/src/main/java/${PACKAGE_PATH}/application/service"
    "application/src/main/java/${PACKAGE_PATH}/application/service/impl"
    "application/src/main/java/${PACKAGE_PATH}/application/vo"
    "application/src/main/java/${PACKAGE_PATH}/application/assembler"
    "application/src/main/java/${PACKAGE_PATH}/application/converter"
    "infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/po"
    "infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/mapper"
    "infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao"
    "infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao/impl"
    "infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/repository"
    "infra/persistence/src/main/resources/mapper"
    "infra/rest/src/main/java/${PACKAGE_PATH}/infra/rest/controller"
    "infra/rpc/src/main/java/${PACKAGE_PATH}/infra/rpc"
    "infra/external/src/main/java/${PACKAGE_PATH}/infra/external"
    "infra/messaging/src/main/java/${PACKAGE_PATH}/infra/messaging"
)

# 重新创建重要目录
for dir in "${KEEP_DIRS[@]}"; do
    mkdir -p "$dir"
done

# 在重要目录中创建 .gitkeep 文件
for dir in "${KEEP_DIRS[@]}"; do
    if [ -d "$dir" ] && [ -z "$(ls -A "$dir")" ]; then
        touch "$dir/.gitkeep"
    fi
done

# 清理其他空目录
find . -type d -empty -not -path "./.git/*" -delete 2>/dev/null || true

# =============================================================================
# 创建基础示例文件（可选）
# =============================================================================
echo "创建基础示例文件..."

# 创建基础的 package-info.java 文件
cat > "api/src/main/java/${PACKAGE_PATH}/api/dto/request/package-info.java" << EOF
/**
 * 请求 DTO 包
 */
package ${PACKAGE_PATH//\//.}.api.dto.request;
EOF

cat > "api/src/main/java/${PACKAGE_PATH}/api/dto/response/package-info.java" << EOF
/**
 * 响应 DTO 包
 */
package ${PACKAGE_PATH//\//.}.api.dto.response;
EOF

cat > "domain/src/main/java/${PACKAGE_PATH}/domain/entity/package-info.java" << EOF
/**
 * 领域实体包
 */
package ${PACKAGE_PATH//\//.}.domain.entity;
EOF

cat > "application/src/main/java/${PACKAGE_PATH}/application/service/package-info.java" << EOF
/**
 * 应用服务包
 */
package ${PACKAGE_PATH//\//.}.application.service;
EOF

cat > "infra/rest/src/main/java/${PACKAGE_PATH}/infra/rest/controller/package-info.java" << EOF
/**
 * REST 控制器包
 */
package ${PACKAGE_PATH//\//.}.infra.rest.controller;
EOF

echo ""
echo "项目示例代码清除完成！"
echo ""
echo "已清除的内容："
echo "1. 用户管理系统相关的所有示例代码"
echo "2. 用户相关的测试文件"
echo "3. 示例数据库脚本"
echo "4. 用户相关的配置文件"
echo ""
echo "保留的内容："
echo "1. 基础框架代码和工具类"
echo "2. 配置文件模板（application.yml 等）"
echo "3. 基础设施配置（MyBatis、Redis 等）"
echo "4. 目录结构和 package-info.java"
echo ""
echo "项目已恢复到干净状态，可以开始开发新功能。"
