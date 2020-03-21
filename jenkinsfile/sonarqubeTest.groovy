#!groovy

def FILE_VERSION_SAVED = "version.log"
def FILE_TEST_RESULT_SAVED = "test-result.log"

node {
//    /* STAGE 1. 소스코드관리 도구로부터 최신 버전을 가져온다 */
    stage('SCM checkout') {
        // Git Repository 로부터 코드를 가져오고 현재 프로젝트를 git 프로젝트로 인식할 수 있게 한다
        checkout scm
    }
//
//    /* STAGE 2. 이전 버전과 같은 버전인지 이난니지를 확인한다 */
//    stage('Git Version Check') {
//        sh(
//                script: "touch ${FILE_VERSION_SAVED}"
//        )
//
//        def GIT_CURRENT_VERSION = sh(
//                script: "git rev-parse --verify HEAD",
//                returnStdout: true
//        )
//
//        def GIT_OLD_VERSION = sh(
//                script: "cat ${FILE_VERSION_SAVED}",
//                returnStdout: true
//        )
//
//        if (GIT_CURRENT_VERSION == GIT_OLD_VERSION) {
//            currentBuild.result = 'SUCCESS'
//            println "이전 버전과 현재 버전이 같으므로 다음 stage를 모두 SKIP합니다"
//            return
//        } else {
//            println "이전 버전과 현재 버전이 다르므로 다음 stage를 이어서 실행합니다."
//        }
//    }
//
//    /* STAGE 3.이전 테스트 결과가 있으면 삭제한다 */
//    stage('Remove previous test result') {
//        if (shouldPassStep()) {
//            return
//        }
//
//        sh (
//                script: "[ -f ${FILE_TEST_RESULT_SAVED} ] && rm ${FILE_TEST_RESULT_SAVED} || echo '지난 테스트 결과가 없습니다'"
//        )
//    }
//
//    /* STAGE 4. 실제로 테스트를 돌리고 실패하는 경우는 슬랙 알림을 보낸다 */
//    stage('Test') {
//        if (shouldPassStep()) {
//            return
//        }
//
//        println "테스트를 수행하겠습니다"
//        sh (
//                script: "./gradlew test --rerun-tasks >> ${FILE_TEST_RESULT_SAVED}"
//        )
//        String testResult = sh(
//                script: "cat ${FILE_TEST_RESULT_SAVED}",
//                returnStdout: true
//        )
//
//        if (testResult.contains("FAILED")) {
//            String commitMessage = sh(
//                    script: "git log -1 --pretty=%B",
//                    returnStdout: true
//            )
//
//            println "테스트가 실패했습니다. 슬랙 알림을 보내고 종료합니다."
//            sendSlack(SLACK_CHANNEL, "danger", "${GIT_BRANCH} 브랜치 테스트가 실패했습니다.\n${commitMessage.trim()}")
//            throw new IllegalArgumentException("테스트가 실패했습니다. 슬랙 알림을 보내고 종료합니다.")
//        }
//    }
//
//    /* STAGE 4. 소나큐브에 소스코드 분석 요청을 보낸다 */
//    stage('Sonarqube Linkage') {
//        if (shouldPassStep()) {
//            return
//        }
//
//        println "테스트가 성공했으므로 소나큐브 연동을 시도합니다"
//        sh(
//                script: "./gradlew sonarqube -x test"
//        )
//        // 소나 큐브에 결과가 잘 전송되어 저장되는데 시간이 필요할 수 있으니 60초 정도 대기
//        sh(
//                script: "sleep 60s"
//        )
//    }
//
//    /* STAGE 5. 소나큐브로부터 분석 결과를 받아와 슬랙으로 알림을 보낸다 */
//    stage('Sonarqube Result Analysis') {
//        if (shouldPassStep()) {
//            return
//        }
//
//        String sonarQubeResult = getSonarQubeResult()
//
//        try {
//            def parsedResult = readJSON text: sonarQubeResult
//            def measures = parsedResult["component"]["measures"]
//            def measuresMap = [:]
//            for (measure in measures) {
//                measuresMap[measure["metric"]] = measure["value"]
//            }
//            def diffMap = [:]
//            for (diff in measures) {
//                diffMap[diff["metric"]] = diff["periods"]["value"]
//            }
//            println measuresMap
//            println diffMap
//            println createSonarQubeResultmessage(measuresMap, diffMap)
////            sendSlack(SLACK_CHANNEL, "#3cb371", createSonarQubeResultmessage(measuresMap, diffMap))
//        } catch (Exception e) {
//            println "소나큐브 API를 확인해보세요"
//            println e
//        }
//    }
//
//    /* STAGE 6. 다음 테스트를 위해 현재 버전을 파일에 저장한다 */
//    stage('Save Current Version') {
//        if (shouldPassStep()) {
//            return
//        }
//
//        println "현재 버전을 저장합니다"
//        sh(
//                script: "rm ${FILE_VERSION_SAVED} && git rev-parse --verify HEAD >> ${FILE_VERSION_SAVED}"
//        )
//    }
//
//    /* STAGE 7. */
//    stage('Export Junit Test Report') {
//        junit '**/build/test-results/test/*.xml'
//
//    }

    stage('Build') {
        withSonarQubeEnv('sonarqube') {
            sh './gradlew -Dorg.gradle.daemon=false clean  check sonarqube build --info --stacktrace '
        }
    }

    stage('Jacoco Reports') {
        jacoco execPattern: '**/build/jacoco/*.exec',
                classPattern: '**/build/classes/java',
                inclusionPattern: '**/*.class',
                exclusionPattern: '**/*Test.class,**/Q*.class',
                sourcePattern: '**/src/main/java',
                sourceInclusionPattern: '**/*.java',
                changeBuildStatus: true,
                maximumBranchCoverage: params.MINIMUM_BRANCH_COVERAGE,
                minimumBranchCoverage: params.MINIMUM_BRANCH_COVERAGE,
                maximumLineCoverage: params.MINIMUM_LINE_COVERAGE,
                minimumLineCoverage: params.MINIMUM_LINE_COVERAGE
    }

    stage("Quality Gate") {
        timeout(time: 10, unit: 'MINUTES') {
            script {
                def qg = waitForQualityGate()
                println "${pg.status}"
                if (qg.status != 'OK') {
                    println "Pipeline aborted due to quality gate failure: ${qg.status}"
                }
            }
        }
    }
}

def shouldPassStep() {
    return currentBuild.result == 'SUCCESS'
}

def sendSlack(slackChannel, color, text) {
    def header = "Content-Type: application/json"
    def data = "{\'channel\': \'${slackChannel}\', \'attachments\': [{\'color\': \'${color}\', \'text\': \'${text.replaceAll("\'", "")}\'}]}"
    def url = "https://woowahan.slack.com/services/hooks/jenkins-ci?token=m1tJ2CquQ920xesVv3jEcA7L"
    sh(
            script: "curl -X POST -H \"${header}\" --data \"${data}\" -s \"${url}\""
    )
}

def getSonarQubeResult() {
    def sonarQubeUrl = "http://sonarqube.woowa.in:8080/api/measures/component"
    def componentKey = "settler"
    def metricKeys = "tests,test_failures,bugs,code_smells,line_coverage,branch_coverage,vulnerabilities,duplicated_lines_density"
    String analysisResult = sh(
            script: "curl -s \"${sonarQubeUrl}?componentKey=${componentKey}&metricKeys=${metricKeys}\"",
            returnStdout: true
    )
    return analysisResult
}


