def projectDir = new File(basedir as File, 'project/generated-app')

assert new File(projectDir, 'pom.xml').isFile()
assert new File(projectDir,
        'api/src/main/java/com/example/generated/api/dto/response/UserPageResponse.java').isFile()
assert new File(projectDir,
        'application/src/main/java/com/example/generated/application/service/template/QueryServiceTemplate.java').isFile()
assert new File(projectDir, 'README.md').text.contains('## 快速开始')

// The fixture is copied under target; locate the source without assuming that depth.
def repositoryDir = basedir as File
while (repositoryDir != null &&
        !new File(repositoryDir, 'src/main/resources/META-INF/maven/archetype-metadata.xml').isFile()) {
    repositoryDir = repositoryDir.parentFile
}
assert repositoryDir != null : 'Cannot locate archetype sources for unfiltered-file verification'
def templateDir = new File(repositoryDir, 'src/main/resources/archetype-resources')
assert !new File(projectDir, 'CLAUDE.md').exists()
def unfilteredFiles = ['AGENTS.md', 'clean.sh', 'mvnw', 'mvnw.cmd',
                       '.mvn/wrapper/maven-wrapper.properties']
['docs', '.agents/skills', 'scripts'].each { directory ->
    def sourceDir = new File(templateDir, directory)
    assert sourceDir.isDirectory() : "Missing engineering foundation directory: ${directory}"
    sourceDir.eachFileRecurse { file ->
        if (file.isFile()) {
            unfilteredFiles << templateDir.toPath().relativize(file.toPath()).toString()
        }
    }
}
unfilteredFiles.each { relative ->
    def output = new File(projectDir, relative)
    assert output.isFile() : "Missing generated file: ${relative}"
    assert Arrays.equals(new File(templateDir, relative).bytes, output.bytes) :
            "Unfiltered content was changed: ${relative}"
}

['create-feature', 'add-api', 'add-migration', 'review-architecture', 'verify-change'].each { skill ->
    def content = new File(projectDir, ".agents/skills/${skill}/SKILL.md").getText('UTF-8')
    assert content.startsWith("---\nname: ${skill}\n") : "Invalid skill identity: ${skill}"
    assert content.contains('\ndescription: ') : "Missing skill trigger: ${skill}"
}
assert new File(projectDir, 'docs/domain/README.md').isFile()
assert new File(projectDir, 'docs/examples/domain-contract.md').isFile()
assert !new File(projectDir, 'docs/domain/user-example.md').exists()
assert new File(projectDir, 'scripts/affected-scope.py').isFile()
assert !new File(projectDir, 'benchmarks').exists()
assert new File(projectDir, 'Makefile').text.contains('agent-check:')

// A new project has no Git history yet. Docs scope must report that missing coverage,
// not accidentally validate the surrounding archetype worktree and claim success.
def smoke = new ProcessBuilder('make', '--no-print-directory', 'agent-check', 'SCOPE=docs')
        .directory(projectDir).start()
def smokeOutput = new StringBuffer()
def smokeErrors = new StringBuffer()
smoke.waitForProcessOutput(smokeOutput, smokeErrors)
assert smoke.exitValue() != 0 : "Docs scope silently accepted missing Git history: ${smokeErrors}"
def report = new groovy.json.JsonSlurper().parseText(smokeOutput.toString())
assert report.context == 'application'
assert report.scope == 'docs'
assert report.status == 'blocked'
assert report.checks.any { it.name == 'changed-file-whitespace' && it.status == 'skipped' }

// Check the relative Markdown links as delivered, including nested skill paths.
// This checks documentation markup only; architecture/security rules use Java tests.
projectDir.eachFileRecurse { file ->
    if (file.isFile() && !file.path.contains(File.separator + 'target' + File.separator) &&
            (file.name.endsWith('.md') || file.name == 'llms.txt')) {
        def fenced = false
        file.eachLine('UTF-8') { line ->
            if (line.trim().startsWith('```')) {
                fenced = !fenced
            } else if (!fenced) {
                (line =~ /\[[^\]]+\]\(([^\s)]+)\)/).each { match ->
                    def href = match[1]
                    if (!(href =~ /^[a-zA-Z][a-zA-Z0-9+.-]*:/) && !href.startsWith('//')) {
                        def path = href.split('#', 2)[0]
                        def target = path ? new File(file.parentFile, path).canonicalFile : file
                        assert target.exists() : "Broken local link ${href} in ${file}"
                        assert target.toPath().startsWith(projectDir.canonicalFile.toPath()) :
                                "Link escapes generated project: ${href} in ${file}"
                    }
                }
            }
        }
    }
}

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
