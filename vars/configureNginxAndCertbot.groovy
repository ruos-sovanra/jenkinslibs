def call(String subdomain, String domain, String deployPort) {
    // Ensure required variables are provided and non-empty
    if (!subdomain?.trim() || !domain?.trim() || !deployPort?.trim()) {
        error "subdomain, domain, and deployPort must be provided and cannot be empty"
    }

    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    // Using Groovy variable interpolation inside the shell block
    sh """
    #!/bin/bash

    folder_name="${subdomain}.${domain}"
    file_path="/etc/nginx/sites-available/\${folder_name}"

    # Create Nginx configuration file using 'tee' to write to the file
    tee "\${file_path}" << EOF
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

    # Enable the site by creating a symbolic link
    ln -sf "\${file_path}" /etc/nginx/sites-enabled/\${folder_name}

    # Reload Nginx to apply the new configuration
    nginx -s reload
    """
}
