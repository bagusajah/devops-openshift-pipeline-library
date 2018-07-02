#!/usr/bin/groovy

def call(body) {
    // evaluate the body block, and collect configuration into the object
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def actor = ""
    def cause = ""
    def action = ""

    def timeoutTime= config.timeoutTime ?: 30
    def proceedMessage = """Would you like to promote version ${config.version} to the next environment?
"""
    timeout(time:timeoutTime, unit:'MINUTES'){
        try {
            input message: proceedMessage, ok: 'Approve'
            action = "approve"
            return true
        }
        catch(org.jenkinsci.plugins.workflow.steps.FlowInterruptedException e) {
            cause = e.causes.get(0)
            actor = cause.getUser().toString()
        }
        finally {
            // if approve --> go on
            // if abort --> stop
            // if no action --> go on
            if ( actor == "SYSTEM" || action == "approve" ) {
                return true
            } else if ( actor != "SYSTEM" ) {
                return false
            }
        }
        
    }

}