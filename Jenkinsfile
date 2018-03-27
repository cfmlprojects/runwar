pipeline {
    agent any

    stages {
		   stage('Preparation') { // for display purposes
		      // Get some code from a GitHub repository
		      git 'https://github.com/cfmlprojects/runwar.git'
		   }
		   stage('Build') {
		      if (isUnix()) {
		         sh "'$WORKSPACE/gradlew' publishRunwarPublicationToMavenRepository"
		      } else {
		         bat("$WORKSPACE/runwar" publishRunwarPublicationToMavenRepository)
		      }
		   }
		   stage('Results') {
		      junit '**/target/surefire-reports/TEST-*.xml'
		      archive 'dist/*.jar'
		   }
    }

    post {
        always  {
            echo "Build completed. currentBuild.result = ${currentBuild.result}"
        }

        changed {
            echo 'Build result changed'

            script {
                if(currentBuild.result == 'SUCCESS') {
                    echo 'Build has changed to SUCCESS status'
                }
            }
        }

        failure {
            echo 'Build failed'
        }

        success {
            echo 'Build was a success'
        }
        unstable {
            echo 'Build has gone unstable'
        }
    }
}
