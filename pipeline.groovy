podTemplate(yaml: '''
apiVersion: v1
kind: Pod
metadata:
    name: node-pod
spec:
    containers:
    - name: jenkins-slave-image
      image: 610204504836.dkr.ecr.ap-south-1.amazonaws.com/jenkins-slave-image:v1
      command:
      - cat
      tty: true
      workingDir: "/home/jenkins/agent"
      securityContext:
      privileged: true
''') {
    node(POD_LABEL) {
        container('jenkins-slave-image') {
            try {
                stage('Setup') {
                    sh '''
                        apt update -y
                        apt install -y openjdk-17-jdk
                        apt install docker.io -y
                    '''
                }
                stage('Checkout SCM') {
                    // cleanWs()
                    echo "########## Checking out code for Netflix Project ###########"
                    dir('Project') {
                        checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/Rohit-kumar12/DevSecOps_Project.git']])
                    }
                }
                stage('SonarQube Analysis') {
                    echo "########### SonarQube Analysis ##########"
                    sh '''
                        cd Project/
                    '''
                    def scannerHome = tool 'SonarQubeScanner-5.0.1';
                    withSonarQubeEnv {
                        sh "${scannerHome}/bin/sonar-scanner \
                        -D sonar.projectKey=Netflix-App"
                    }
                    // def qg = waitForQualityGate()
                }
                stage('Login to ECR') {
                    withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'aws-jenkins-setup', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        echo "######### Login to ECR ##########"
                        sh '''
                            aws ecr get-login-password --region ap-south-1 | docker login --username AWS --password-stdin 610204504836.dkr.ecr.ap-south-1.amazonaws.com
                        '''    
                    }
                }
                stage('Build Docker Image') {
                    withCredentials([aws(accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: 'aws-jenkins-setup', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY')]) {
                        echo "######### BuildDocker Image #########"
                        sh '''
                            cd Project/
                            docker build --build-arg TMDB_V3_API_KEY=$TMDB_V3_API_KEY -t 610204504836.dkr.ecr.ap-south-1.amazonaws.com/netflix-project:latest --no-cache .
                        '''
                    }
                }
                stage('TRIVYSCAN'){
                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'jenkins-aws-setup',variable: 'AWS']]){
                        echo "###############TRIVY SCAN ####################"
                        sh'''
                            cd Django/
                            OUTPUT=$(git rev-parse --short HEAD)
                            export TAG=djangoqa
                            export SEVERITY=CRITICAL
                            export IMAGE=842928376651.dkr.ecr.ap-south-1.amazonaws.com/sams-qa-eks:${TAG}_${OUTPUT}
                            cd ..
                            trivy image -f json -o result.json --severity $SEVERITY $IMAGE;
                            mv /json_to_csv.py $WORKSPACE/
                            python3 json_to_csv.py
                            mv `echo $IMAGE | cut -d"/" -f2`.csv ${SEVERITY}_`echo $IMAGE | cut -d"/" -f2`.csv
                        '''  
                    }
                    archiveArtifacts artifacts: '*.csv', fingerprint: true, onlyIfSuccessful: true
                } 
                stage('Push Docker Image'){
                    withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'jenkins-aws-setup',variable: 'AWS']]){
                        echo "############### Push Docker Image ####################"
                        sh'''
                            cd Django/
                            export TAG=djangoqa
                            docker push 842928376651.dkr.ecr.ap-south-1.amazonaws.com/sams-qa-eks:${TAG}_`git rev-parse --short HEAD`
                        '''
                    }
                }
            }
            catch (e) {
                throw e
            }
        }
    }
}
