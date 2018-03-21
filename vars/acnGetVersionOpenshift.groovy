#!/usr/bin/groovy
import com.ascendmoney.vulcan.Utils

def call(body) {

    def openshiftVersionOriginal = sh script: "oc version |grep openshift", returnStdout: true
    openshiftVersion = openshiftVersionOriginal.substring(openshiftVersionOriginal.indexOf("v") + 1);
    openshiftVersion = openshiftVersion.substring(0, openshiftVersion.lastIndexOf("."));
    echo "openshiftVersionOriginal ${openshiftVersionOriginal}"
    echo "openshiftVersion ${openshiftVersion}"

    return openshiftVersion

} // End Function
