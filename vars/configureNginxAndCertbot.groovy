def call(String subdomain, String domain, String deployPort) {
    // Check if the required variables are passed
    if (!subdomain || !domain || !deployPort) {
        error "subdomain, domain, and deployPort must be provided"
    }

    // Echo the variables for debugging purposes
    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    // Trim variables and ensure they are not empty
    if (subdomain.trim() == "" || domain.trim() == "" || deployPort.trim() == "") {
        error "subdomain, domain, and deployPort cannot be empty"
    }

    // Use double quotes for the sh block to allow Groovy variable interpolation
    sh """
    #!/bin/bash

    folder_name="${subdomain}.${domain}"
    file_path="/etc/nginx/sites-available/\${folder_name}"

    # Check if Nginx config file already exists
    if [ -f "\${file_path}" ]; then
        echo "Nginx config for \${folder_name} already exists."
        exit 0
    fi

    # Write the Nginx configuration to the file
    sudo bash -c "cat > \${file_path} <<EOL
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
    EOL"

    # Create a symlink in /etc/nginx/sites-enabled/ to enable the site
    if [ ! -L /etc/nginx/sites-enabled/\${folder_name} ]; then
        sudo ln -s \${file_path} /etc/nginx/sites-enabled/\${folder_name}
    else
        echo "Symlink already exists for \${folder_name}"
    fi

    # Test the Nginx configuration for syntax errors
    sudo nginx -t
    if [ $? -eq 0 ]; then
        # Reload Nginx to apply the new configuration
        sudo systemctl reload nginx
        echo "Nginx configuration reloaded for \${folder_name}"
    else
        echo "Nginx configuration test failed"
        exit 1
    fi
    """
}
