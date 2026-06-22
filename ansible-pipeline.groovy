pipeline {
    agent any

    environment {
	ANSIBLE_COLLECTION_PATH = '/home/svc_install/.ansible/collections'
    }

    options {
        skipDefaultCheckout(true)
    }

    stages {
        stage('Checkout Code') {
            steps {
                git branch : 'main',
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
                    string (credentialsId: 'gmail-username', variable: 'GMAIL_USER'),
                    string (credentialsId: 'gmail-password', variable: 'GMAIL_PASS')
                ]) {
                    sh '''
                    ansible --version

                    ansible-playbook -i inventory.ini check_services.yaml --syntax-check \
                    --vault-password-file $VAULT_FILE
                    '''
                }
            }
        }
        }
    }
