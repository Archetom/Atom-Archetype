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
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserQueryRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserCreateRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/response/UserResponse.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/enums/UserStatus.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/facade/UserFacade.java

# =============================================================================
# Domain 层示例代码
# =============================================================================
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/entity/User.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/repository/UserRepository.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/UserDomainService.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/impl/UserDomainServiceImpl.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/messaging/UserCreatedEvent.java

# =============================================================================
# Application 层示例代码
# =============================================================================
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/UserService.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/impl/UserServiceImpl.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/vo/UserVO.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/assembler/UserAssembler.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/converter/UserConverter.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/config/DatabaseInitializer.java

# =============================================================================
# Infrastructure 层示例代码
# =============================================================================
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/converter/UserPOConverter.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/po/UserPO.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/mapper/UserMapper.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao/UserDao.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/mysql/dao/impl/UserDaoImpl.java
rm -f infra/persistence/src/main/java/${PACKAGE_PATH}/infra/persistence/repository/UserRepositoryImpl.java
rm -f infra/persistence/src/main/resources/mapper/UserMapper.xml
rm -f infra/rest/src/main/java/${PACKAGE_PATH}/infra/rest/controller/UserController.java
rm -f infra/rpc/src/main/java/${PACKAGE_PATH}/infra/rpc/UserFacadeImpl.java

# =============================================================================
# 配置文件和数据库脚本
# =============================================================================
rm -f start/src/main/resources/application.yml
rm -f start/src/test/resources/application-test.yml
rm -f infra/persistence/src/main/resources/sql/schema.sql
rm -f start/src/test/resources/sql/init-test-data.sql
rm -f infra/persistence/src/main/resources/mybatis-plus.yml

# =============================================================================
# 测试代码
# =============================================================================
rm -f application/src/test/java/${PACKAGE_PATH}/application/service/UserServiceTest.java
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/service/UserDomainServiceTest.java
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/entity/UserTest.java
rm -f start/src/test/java/${PACKAGE_PATH}/test/UserControllerIntegrationTest.java
rm -f shared/src/test/java/${PACKAGE_PATH}/shared/event/UserEventListenerTest.java

# =============================================================================
# 清理空目录
# =============================================================================
find . -type d -empty -delete 2>/dev/null || true

echo "项目示例代码清除完成！"
echo ""
echo "已清除的内容："
echo "1. 用户管理系统示例代码"
echo "2. 所有测试文件"
echo "3. 示例配置文件"
echo "4. 数据库脚本"
echo ""
echo "项目已恢复到原始状态。"
