#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

    def openshiftVersionOriginal = sh script: "oc version |grep openshift", returnStdout: true
    openshiftVersion = openshiftVersionOriginal.substring(openshiftVersionOriginal.indexOf("v") + 1);
    openshiftVersion = openshiftVersion.substring(0, openshiftVersion.lastIndexOf("."));

    return openshiftVersion

} // End Function
