#!/usr/bin/groovy
import com.ascendmoney.cicd.Utils

def call(body) {

	def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    def libName = config.libName
    def libVersionExpect = config.libVersionExpect
    def fileLibPath = config.fileLibPath

    echo "libName ${libName}"
    echo "libVersionExpect ${libVersionExpect}"
    echo "fileLibPath ${fileLibPath}"
    if ( libName == "mod-wsgi" ) {
        result_no_specific_version = ""
        result_specific_version = ""
        try {
            result_no_specific_version = sh script: "cat ${fileLibPath} |grep -e \"mod-wsgi\$\" ", returnStdout: true
            result_no_specific_version = result_no_specific_version.length()
        }
        catch(Exception e) {
            result_no_specific_version = 0
        }
        result_specific_version = sh script: "cat ${fileLibPath} |grep -e \"mod-wsgi[\$=]\"", returnStdout: true
        result_specific_version = result_specific_version.length()
        if ( result_no_specific_version.toInteger() > 0 ) {
            line_full = sh script: "cat ${fileLibPath} |grep -n \"mod-wsgi\$\"", returnStdout: true
            line = line_full.substring(0, line_full.lastIndexOf(":"))
            sh "sed -i ''${line}'s/mod.*/${libVersionExpect}/g' ${fileLibPath}"
        }
        if ( result_specific_version.toInteger() > 0 ) {
            line_full = sh script: "cat ${fileLibPath} |grep -n \"mod-wsgi[\$=]\"", returnStdout: true
            line = line_full.substring(0, line_full.lastIndexOf(":"))
            sh "sed -i ''${line}'s/mod.*/${libVersionExpect}/g' ${fileLibPath}"
        }
    }

    return "replaced"
} // End Function
