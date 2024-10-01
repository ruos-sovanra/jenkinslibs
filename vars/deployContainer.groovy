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
        set -e  # Exit on error

        # Paths
        NGINX_CONF="/etc/nginx/sites-available/${subdomain}.${domain}"
        NGINX_CONF_LINK="/etc/nginx/sites-enabled/${subdomain}.${domain}"
        EMAIL="admin@${domain}"

        # Ensure the directory for Nginx config exists
        if [ ! -d "/etc/nginx/sites-available/${subdomain}.${domain}" ]; then
            echo "Creating directory /etc/nginx/sites-available..."
            sudo mkdir -p /etc/nginx/sites-available/${subdomain}.${domain}
        fi

        # Check and install Nginx and Certbot if missing
        PACKAGES=""
        if ! command -v nginx > /dev/null; then
            echo "Nginx not found. Installing..."
            PACKAGES+="nginx "
        fi
        if ! command -v certbot > /dev/null; then
            echo "Certbot not found. Installing..."
            PACKAGES+="certbot python3-certbot-nginx "
        fi

        if [ -n "$PACKAGES" ]; then
            echo "Installing necessary packages: \$PACKAGES"
            sudo apt-get update
            sudo apt-get install -y \$PACKAGES
        fi

        # Create Nginx configuration
        echo "Configuring Nginx for ${subdomain}.${domain}..."
        sudo tee \$NGINX_CONF > /dev/null <<EOL
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
        EOL

        # Enable the site by creating a symbolic link
        if [ ! -L "\$NGINX_CONF_LINK" ]; then
            echo "Enabling site for ${subdomain}.${domain}..."
            sudo ln -sf \$NGINX_CONF \$NGINX_CONF_LINK
        fi

        # Test and restart Nginx
        echo "Testing and restarting Nginx..."
        sudo nginx -t && sudo systemctl restart nginx

        # Obtain SSL certificate with Certbot
        echo "Obtaining SSL certificate for ${subdomain}.${domain}..."
        sudo certbot --nginx -d ${subdomain}.${domain} --non-interactive --agree-tos --redirect -m \$EMAIL

        # Final Nginx restart
        echo "Restarting Nginx to apply changes..."
        sudo systemctl restart nginx

        echo "HTTPS configured for https://${subdomain}.${domain}"
    """
}
