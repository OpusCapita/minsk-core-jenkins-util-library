def privateClosure = {
    echo "privateClosure"
}

// current implementation works with GutHub only
def sendNotification() {
    def result = currentBuild.currentResult
    // testing code extraction/dividing approach
    privateClosure()
    /*
    uncommented this code if you want notify message to slake between state change FAILURE -> SUCCESS || SUCCESS -> FAILURE
    def previousResult = currentBuild.previousBuild?.result
    notify = previousResult != "FAILURE" && result == "FAILURE") || (result == "SUCCESS" && (previousResult == "FAILURE" || previousResult == "UNSTABLE"))
    */
    def message = ""
    wrap([$class: 'BuildUser']) {
        def ref = currentBuild.changeSets.collect { changeSet ->
            changeSet.browser.getChangeSetLink(changeSet.find { true })?.toString()
        }.find { true }
        if (ref) {
            ref = ref.substring(0, ref.indexOf('/commit/'))
            message = "${env.BUILD_USER_ID}'s build (<${env.BUILD_URL}|${env.BUILD_DISPLAY_NAME}>; push) in <${ref}|${ref - "https://github.com/"}> (${env.BRANCH_NAME})"
        } else {
            def scmInfo = checkout scm
            message = "${env.BUILD_USER_ID}'s build (<${env.BUILD_URL}|${env.BUILD_DISPLAY_NAME}>) no changes in repo <${scmInfo.GIT_URL}|${scmInfo.GIT_URL - "https://github.com/"}>    (${env.BRANCH_NAME})"
        }
    }
    message = "${(result == 'FAILURE')?'Failed':'Success'}: ${message}"
    currentBuild.changeSets.each { changeSet ->
        def browser = changeSet.browser
        changeSet.each { change ->
            def link = browser.getChangeSetLink(change).toString()
            message = "${message}\n- ${change.msg} (<${link}|${link.substring(link.lastIndexOf('/') + 1, link.length()).substring(0, 7)}> by ${change.author.toString()})"
        }
    }
    slackSend message: message, color: (result == 'FAILURE')?'danger':'good'
}
