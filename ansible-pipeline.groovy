pipeline {
    agent any

    environment {
	ANSIBLE_COLLECTION_PATH = '/home/svc_install/.ansible/collections'
    }

    options {
        skipDefaultCheckout(true)
    }
    stage('Debug') {
    	steps {
            sh '''
            whoami
            pwd

	    echo "=== Ansible Version ==="
	    ansible --version

	    echo "=== Collection Path ==="
	    env | grep ANSIBLE

            echo "=== Mail Module Test ==="
            ansible-doc community.general.mail
            '''
            }
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
