pipeline {
   agent any
   stages {

      stage ('Build') {
         steps {
            sh "rm -rf build/libs/"
            sh "chmod +x gradlew"
            sh "./gradlew build"

            archiveArtifacts artifacts: '**/build/libs/*.jar', fingerprint: true
         }
      }

      stage ('Archive artifacts') {
         //Only publish to maven when on master branch
         when {
            branch 'master'
         }
         steps {
            sh "./gradlew upload"
         }
      }

   }
}