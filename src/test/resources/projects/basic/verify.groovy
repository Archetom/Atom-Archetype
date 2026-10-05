def projectDir = new File(basedir as File, 'project/generated-app')

assert new File(projectDir, 'pom.xml').isFile()
assert new File(projectDir,
        'api/src/main/java/com/example/generated/api/dto/response/UserPageResponse.java').isFile()
assert new File(projectDir,
        'application/src/main/java/com/example/generated/application/service/template/QueryServiceTemplate.java').isFile()
assert new File(projectDir, 'README.md').text.contains('## 快速开始')

assert new File(projectDir, 'pom.xml').text.contains('<version>${atom.common.version}</version>')
// GHSA-xxph-c9ww-hj94 is fixed in Guava 33.7.2; generated dependency management must not downgrade it.
def generatedPom = new File(projectDir, 'pom.xml').text
def guavaVersion = (generatedPom =~ /<guava.version>([^<]+)<\/guava.version>/)[0][1]
assert guavaVersion.endsWith('-jre')
assert new org.apache.maven.artifact.versioning.ComparableVersion(guavaVersion) >=
        new org.apache.maven.artifact.versioning.ComparableVersion('33.7.2-jre')
assert generatedPom.contains('<version>${guava.version}</version>')
assert new File(projectDir,
        'application/src/main/java/com/example/generated/application/service/template/CommandServiceTemplate.java')
        .text.contains('@Value("${spring.application.name}")')
assert new File(projectDir, 'infra/persistence/src/main/resources/mapper/UserMapper.xml')
        .text.contains('#{tenantId}')
assert new File(projectDir, 'conf/logback-spring.xml').text.contains('${LOG_PATTERN}')
// The test profile enables trusted headers, so it must stay a test resource outside the runtime jar.
assert !new File(projectDir, 'conf/application-test.yml').exists()
assert new File(projectDir, 'start/src/test/resources/application-test.yml')
        .text.contains('name: generated-app-test')
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
