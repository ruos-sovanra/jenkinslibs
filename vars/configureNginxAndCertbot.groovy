def call(String subdomain, String domain, String deployPort) {
    // Check if the variables are passed
    if (!subdomain || !domain || !deployPort) {
        error "subdomain, domain, and deployPort must be provided"
    }

    // Echo the variables to check if they are passed down correctly
    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    if (subdomain.trim() == "" || domain.trim() == "" || deployPort.trim() == "") {
        error "subdomain, domain, and deployPort cannot be empty"
    }

    // Use double quotes for the sh block to allow Groovy variable interpolation
    sh """
    #!/bin/bash

    folder_name="${subdomain}.${domain}"
    file_path="/etc/nginx/sites-available/\${folder_name}"

    # Write the specific Nginx configuration to the file
    cat  << EOF > \${file_path}
    server {
        listen 80;
        server_name ${subdomain}.${domain};

        location / {
            proxy_pass http://localhost:${deployPort};
            proxy_set_header Host \$host;
            proxy_set_header X-Real-IP \$remote_addr;
            proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto \$scheme;
        }
    }
    EOF
    """
}
