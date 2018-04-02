#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def timeoutTime= config.timeoutTime ?: 30
    def proceedMessage = """Would you like to promote version ${config.version} to the next environment?
"""
    try {
        timeout(time:timeoutTime, unit:'MINUTES') {
            input id: 'Proceed', message: "\n${proceedMessage}"
        }
        echo "approve"
    } catch(Exception e) {
        return true
    }
    return true

}