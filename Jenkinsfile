#!groovy

pipeline {

  agent { label 'master' }

  environment {
    app = "fetchr"
    package_name = ""

    author = ""
    commit_message = ""
    profile = "dev"
  }

  stages {

    stage("Gather Facts"){
      steps{
        script {
          def configs = [
                  [branch:"new_develop", suffix: "dev", deploy: true],
                  [branch:"release", suffix: "qa", deploy: true]
          ]

          commit_message = sh(script: "git log -1 --pretty=%B ", returnStdout:true).trim().toLowerCase()
          def conf = null

          configs.each{ config ->
            if(env.GIT_BRANCH.contains(config.branch)){
              conf = config
            }
          }

          if(conf == null){
            currentBuild.result = 'ABORTED'
            error("No suitable config found. Build is aborted")
          }

          profile = conf.profile

          def commit_id = sh(script: "git log -1 --pretty=%h ", returnStdout:true).trim()
          package_name = "${app}.${commit_id}.${conf.suffix}"

          author = sh(script: "git log -1 --pretty=%an ", returnStdout:true).trim()
          version_name= sh(script: "cat gradle.properties |grep versionName|awk '{print \$2}'",returnStdout:true).trim()
          version_code= sh(script: "cat gradle.properties |grep versionCode|awk '{print \$2}'",returnStdout:true).trim()

          echo "Package: ${package_name}"
        }
      }
    }

    stage("Build"){
      steps{

        sh "./gradlew -x test -Pkeypass='fetchr' -PstorePass='fetchr' -PstoreFilePath='/var/lib/jenkins/keystores/fetchr.jks' clean assemble crashlyticsUploadDistributionQa"
      }
    }

  } // stages

  post {
         always {
                    archiveArtifacts artifacts: '**/*.apk', onlyIfSuccessful: true
                }
      success {
        googlechatnotification url: "id:gchat-android-builds", message: """SUCCESS Jenkins Build Number:#${env.BUILD_NUMBER}
        Job Name:[${env.JOB_NAME}] Author:[${author}]
        Last Commit:${commit_message}
        VersionName:${version_name}
        VersionCode:${version_code}"""
      }

      failure {
        googlechatnotification url: "id:gchat-android-builds", message: "*FAILED* #${env.BUILD_NUMBER} [${env.JOB_NAME}] [${author}] ${commit_message}. <${env.BUILD_URL}|Logs>"
      }
    }

} // pipeline
