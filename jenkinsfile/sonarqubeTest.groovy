#!groovy

def FILE_VERSION_SAVED = "version.log"
def FILE_TEST_RESULT_SAVED = "test-result.log"
def STATUS = "OK"

node {
//    /* STAGE 1. 소스코드관리 도구로부터 최신 버전을 가져온다 */
    stage('SCM checkout') {
        // Git Repository 로부터 코드를 가져오고 현재 프로젝트를 git 프로젝트로 인식할 수 있게 한다
        checkout scm
    }

    /* STAGE 2. 이전 버전과 같은 버전인지 이난니지를 확인한다 */
    stage('Git Version Check') {
        sh(
                script: "touch ${FILE_VERSION_SAVED}"
        )

        def GIT_CURRENT_VERSION = sh(
                script: "git rev-parse --verify HEAD",
                returnStdout: true
        )

        def GIT_OLD_VERSION = sh(
                script: "cat ${FILE_VERSION_SAVED}",
                returnStdout: true
        )

        println GIT_CURRENT_VERSION
        println GIT_OLD_VERSION
        if (GIT_CURRENT_VERSION == GIT_OLD_VERSION) {
            currentBuild.result = 'SUCCESS'
            STATUS = 'SKIP'
            println "이전 버전과 현재 버전이 같으므로 다음 stage를 모두 SKIP합니다"
            return
        } else {
            println "이전 버전과 현재 버전이 다르므로 다음 stage를 이어서 실행합니다."
        }
    }

    /* STAGE 3.이전 테스트 결과가 있으면 삭제한다 */
    stage('Remove previous test result') {
        if (STATUS == 'SKIP') {
            return
        }

        sh(
                script: "[ -f ${FILE_TEST_RESULT_SAVED} ] && rm ${FILE_TEST_RESULT_SAVED} || echo '지난 테스트 결과가 없습니다'"
        )
    }
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
        if (STATUS == 'SKIP') {
            return
        }

        withSonarQubeEnv('sonarqube') {
//            sh './gradlew -Dorg.gradle.daemon=false clean  check sonarqube build --info --stacktrace '
            sh './gradlew sonarqube ' +
                    '  -Dsonar.host.url=http://192.168.0.9:9000 ' +
                    '  -Dsonar.login=fb6900895a58035bb1cbd2f1abdcae178b0cb5c3'
        }
    }

    stage('Sonarqube Result Analysis') {
        if (STATUS == 'SKIP') {
            return
        }
        String sonarQubeResult = getSonarQubeResult()

        try {
            def parsedResult = readJSON text: sonarQubeResult
            def measures = parsedResult["component"]["measures"]
            def measuresMap = [:]
            for (measure in measures) {
                measuresMap[measure["metric"]] = measure["value"]
            }
            def diffMap = [:]
            for (diff in measures) {
                diffMap[diff["metric"]] = diff["periods"]["value"]
            }
            println measuresMap
            println diffMap
            println createSonarQubeResultmessage(measuresMap, diffMap)
        } catch (Exception e) {
            println "소나큐브 API를 확인해보세요"
            println e
        }
    }

    stage('Jacoco Reports') {
        if (STATUS == 'SKIP') {
            return
        }
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
        def USE_QUALITY_GATE = "${USE_QUALITY_GATE}"
        if (USE_QUALITY_GATE == "false" || STATUS == 'SKIP') {
            return
        }
        timeout(time: 5, unit: 'MINUTES') {

            def qg = waitForQualityGate()
            println "${qg.status}"

            if (qg.status == 'OK') {
                println "Quality Gate를 통과했습니다."
                currentBuild.result == "SUCCESS"
            } else if (qg.status == 'ERROR') {
                println "Pipeline aborted due to quality gate failure: ${qg.status}"
                currentBuild.result = "FAILURE"
            } else {
                currentBuild.result = "UNSTABLE"
            }
        }
    }
}



def getSonarQubeResult() {
    def sonarQubeUrl = "http://192.168.0.9:9000/api/measures/component"
    def componentKey = "com.dellife:basic-project"
    def metricKeys = "tests,test_failures,bugs,code_smells,line_coverage,branch_coverage,vulnerabilities,duplicated_lines_density"
    String analysisResult = sh(
            script: "curl -s \"${sonarQubeUrl}?componentKey=${componentKey}&metricKeys=${metricKeys}\"",
            returnStdout: true
    )
    return analysisResult
}

def createSonarQubeResultmessage(measuresMap, diffMap) {
    String commitMessage = sh(
            script: "git log -1 --pretty=%B",
            returnStdout: true
    )

    return "*브랜치정보*\n" +
            "- ${commitMessage.trim()}\n\n" +
            "*소나큐브 결과*\n" +
            String.format("%s                     %6s [%10s]\n", "- 테스트 총 개수", measuresMap["tests"], decideUpDownSide(diffMap["tests"], false)) +
            String.format("%s                  %6s [%10s]\n", "- 실패한 테스트 개수", measuresMap["test_failures"], decideUpDownSide(diffMap["test_failures"], false)) +
            String.format("%s                               %6s [%10s]\n", "- 버그 개수", measuresMap["bugs"], decideUpDownSide(diffMap["bugs"], false)) +
            String.format("%s             %6s [%10s]\n", "- CODE SMELL 개수", measuresMap["code_smells"], decideUpDownSide(diffMap["code_smells"], false)) +
            String.format("%s            %6s%% [%10s]\n", "- LINE COVERAGE", measuresMap["line_coverage"], decideUpDownSide(diffMap["line_coverage"], true)) +
            String.format("%s    %6s%% [%10s]\n", "- BRANCH COVERAGE", measuresMap["branch_coverage"], decideUpDownSide(diffMap["branch_coverage"], true)) +
            String.format("%s                            %6s [%10s]\n", "- 취약점 개수", measuresMap["vulnerabilities"], decideUpDownSide(diffMap["vulnerabilities"], false)) +
            String.format("%s                            %6s%% [%10s]\n", "- 중복 라인", measuresMap["duplicated_lines_density"], decideUpDownSide(diffMap["duplicated_lines_density"], true))
}

def decideUpDownSide(Object diffArray, boolean isRatio) {
    double value = Double.parseDouble(diffArray[0] as String)
    String percent = isRatio ? "%" : " "
    if (value > 0) {
        return String.format("%4s%s ↑", value.round(2), percent)
    }
    if (value < 0) {
        return String.format("%4s%s ↓", value.round(2), percent)
    }
    return String.format("%4s%s -", value.round(2), percent)
}
