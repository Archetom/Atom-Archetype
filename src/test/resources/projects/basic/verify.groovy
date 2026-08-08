def projectDir = new File(basedir as File, 'project/generated-app')

assert new File(projectDir, 'pom.xml').isFile()
assert new File(projectDir,
        'api/src/main/java/com/example/generated/api/dto/response/UserPageResponse.java').isFile()
assert new File(projectDir,
        'application/src/main/java/com/example/generated/application/service/template/QueryServiceTemplate.java').isFile()
assert new File(projectDir, 'README.md').text.contains('## 快速开始')

assert new File(projectDir, 'pom.xml').text.contains('<version>${atom.common.version}</version>')
assert new File(projectDir,
        'application/src/main/java/com/example/generated/application/service/template/CommandServiceTemplate.java')
        .text.contains('@Value("${spring.application.name}")')
assert new File(projectDir, 'infra/persistence/src/main/resources/mapper/UserMapper.xml')
        .text.contains('#{tenantId}')
assert new File(projectDir, 'conf/logback-spring.xml').text.contains('${LOG_PATTERN}')
assert new File(projectDir,
        'start/src/test/java/com/example/generated/UserControllerIntegrationTest.java')
        .text.contains('jsonPath("$.username")')

def unresolved = ['${dollar}', '${pound}', '${symbol_dollar}', '${symbol_pound}',
                  '${groupId}', '${artifactId}', '${package}', '${packageInPathFormat}',
                  '${rootArtifactId}', '${version}', '$h2', '#set(']
projectDir.eachFileRecurse { file ->
    if (file.isFile() && !file.path.contains(File.separator + 'target' + File.separator)) {
        def text = file.getText('UTF-8')
        unresolved.each { marker ->
            assert !text.contains(marker) : "Unresolved marker ${marker} in ${file}"
        }
    }
}

return true
