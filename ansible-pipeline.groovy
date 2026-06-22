pipeline {
    agent any

    environment {
        ANSIBLE_COLLECTIONS_PATHS = '/home/svc_install/.ansible/collections:/usr/share/ansible/collections'
    }

    options {
        skipDefaultCheckout(true)
    }

    stages {

        stage('Debug') {
            steps {
                sh '''
                echo "===== USER ====="
                whoami

                echo "===== WORKSPACE ====="
                pwd

                echo "===== ANSIBLE VERSION ====="
                ansible --version

                echo "===== COLLECTION ENV ====="
                env | grep ANSIBLE || true

                echo "===== COLLECTION PATHS ====="
                ansible-config dump | grep COLLECTION || true

                echo "===== INSTALLED COLLECTIONS ====="
                ansible-galaxy collection list | grep community.general || true

                echo "===== MAIL MODULE TEST ====="
                ansible-doc community.general.mail || true
                '''
            }
        }

        stage('Checkout Code') {
            steps {
                git branch: 'main',
                    credentialsId: 'git',
                    url: 'git@github.com:Nithindatta/Ansible-Service-Check.git'
            }
        }

        stage('Syntax Check') {
            steps {
                withCredentials([
                    file(credentialsId: 'vault.pass', variable: 'VAULT_FILE'),
                    sshUserPrivateKey(
                        credentialsId: 'ansible-ssh-key',
                        keyFileVariable: 'SSH_KEY',
                        usernameVariable: 'SSH_USER'
                    ),
                    string(credentialsId: 'gmail-username', variable: 'GMAIL_USER'),
                    string(credentialsId: 'gmail-password', variable: 'GMAIL_PASS')
                ]) {

                    sh '''
                    echo "===== VERIFY MAIL MODULE ====="
                    ansible-doc community.general.mail

                    echo "===== VERIFY VAULT FILE ====="
                    ls -l $VAULT_FILE

                    echo "===== PLAYBOOK SYNTAX CHECK ====="
                    ansible-playbook \
                      -i inventory.ini \
                      check_services.yaml \
                      --syntax-check \
                      --vault-password-file $VAULT_FILE
                    '''
                }
            }
        }
    }

    post {
        always {
            echo "Pipeline completed"
        }

        success {
            echo "Syntax check successful"
        }

        failure {
            echo "Syntax check failed"
        }
    }
}
