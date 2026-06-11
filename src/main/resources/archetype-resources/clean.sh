#!/bin/bash

# Remove sample code from the generated project
echo "Cleaning project sample code..."

# check whether in of directory in
if [ ! -f "pom.xml" ]; then
    echo " error: Please run this script from the project root directory"
    exit 1
fi

# get of package path
PACKAGE_PATH=$(find . -name "Bootstrap.java" | head -1 | sed 's|/Bootstrap.java||' | sed 's|./start/src/main/java/||')

if [ -z "$PACKAGE_PATH" ]; then
    echo " error: Unable to find the project package path"
    exit 1
fi

echo "Detected package path: $PACKAGE_PATH"

# =============================================================================
# API layer sample code
# =============================================================================
echo " clean API layer sample code..."
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserQueryRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/request/UserCreateRequest.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/dto/response/UserResponse.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/enums/UserStatus.java
rm -f api/src/main/java/${PACKAGE_PATH}/api/facade/UserFacade.java

# =============================================================================
# Domain layer sample code
# =============================================================================
echo " clean Domain layer sample code..."
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/entity/User.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/repository/UserRepository.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/UserDomainService.java
rm -f domain/src/main/java/${PACKAGE_PATH}/domain/service/impl/UserDomainServiceImpl.java
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
# Application layer sample code
# =============================================================================
echo " clean Application layer sample code..."
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/UserService.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/impl/UserServiceImpl.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/service/impl/UserCacheService.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/vo/UserVO.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/assembler/UserAssembler.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/converter/UserConverter.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/event/DomainEventPublisherImpl.java
rm -f application/src/main/java/${PACKAGE_PATH}/application/event/listener/UserEventListener.java

# =============================================================================
# Infrastructure layer sample code
# =============================================================================
echo " clean Infrastructure layer sample code..."

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

# Facade
rm -f infra/facade/src/main/java/${PACKAGE_PATH}/infra/facade/UserFacadeImpl.java

# External
rm -f infra/external/src/main/java/${PACKAGE_PATH}/infra/external/EmailServiceImpl.java
rm -f infra/external/src/main/java/${PACKAGE_PATH}/infra/external/SmsServiceImpl.java

# Messaging
rm -f infra/messaging/src/main/java/${PACKAGE_PATH}/infra/messaging/UserEventPublisher.java

# =============================================================================
# database and configuration file (retain base configuration, delete sample data)
# =============================================================================
echo " clean database and sample configuration..."
rm -f infra/persistence/src/main/resources/sql/schema.sql
rm -f docker/mysql/init/01-init-database.sql

# =============================================================================
# test code
# =============================================================================
echo " clean test code..."
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/entity/UserTest.java
rm -f domain/src/test/java/${PACKAGE_PATH}/domain/specification/UserSpecificationTest.java
rm -f start/src/test/java/${PACKAGE_PATH}/UserControllerIntegrationTest.java
rm -f start/src/test/resources/sql/init-test-data.sql

# =============================================================================
# clean directory (but retain important of directory structure)
# =============================================================================
echo " clean directory..."

# define need retain of important directory
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
    "infra/facade/src/main/java/${PACKAGE_PATH}/infra/facade"
    "infra/external/src/main/java/${PACKAGE_PATH}/infra/external"
    "infra/messaging/src/main/java/${PACKAGE_PATH}/infra/messaging"
)

# new create important directory
for dir in "${KEEP_DIRS[@]}"; do
    mkdir -p "$dir"
done

# in important directory in create.gitkeep file
for dir in "${KEEP_DIRS[@]}"; do
    if [ -d "$dir" ] && [ -z "$(ls -A "$dir")" ]; then
        touch "$dir/.gitkeep"
    fi
done

# clean directory
find . -type d -empty -not -path "./.git/*" -delete 2>/dev/null || true

# =============================================================================
# create base sample file (optional)
# =============================================================================
echo " create base sample file..."

# create base of package-info.java file
cat > "api/src/main/java/${PACKAGE_PATH}/api/dto/request/package-info.java" << EOF
/**
 * request DTO package
 */
package ${PACKAGE_PATH//\//.}.api.dto.request;
EOF

cat > "api/src/main/java/${PACKAGE_PATH}/api/dto/response/package-info.java" << EOF
/**
 * response DTO package
 */
package ${PACKAGE_PATH//\//.}.api.dto.response;
EOF

cat > "domain/src/main/java/${PACKAGE_PATH}/domain/entity/package-info.java" << EOF
/**
 * domain entity package
 */
package ${PACKAGE_PATH//\//.}.domain.entity;
EOF

cat > "application/src/main/java/${PACKAGE_PATH}/application/service/package-info.java" << EOF
/**
 * application service package
 */
package ${PACKAGE_PATH//\//.}.application.service;
EOF

cat > "infra/rest/src/main/java/${PACKAGE_PATH}/infra/rest/controller/package-info.java" << EOF
/**
 * REST controller package
 */
package ${PACKAGE_PATH//\//.}.infra.rest.controller;
EOF

echo ""
echo "Project sample code cleanup complete!"
echo ""
echo " already clear of content: "
echo "1. all sample code related to the user management system "
echo "2. user-related test files "
echo "3. sample database "
echo "4. user-related configuration files "
echo ""
echo " retain of content: "
echo "1. base framework code and utilities "
echo "2. configuration templates (application.yml etc.)"
echo "3. infrastructure configuration (MyBatis, Redis etc.)"
echo "4. directory structure and package-info.java"
echo ""
echo "The project has been restored to a clean state and is ready for new development."
