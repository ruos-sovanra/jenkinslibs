def call(String subdomain, String domain, String deployPort) {
    if (!subdomain || !domain || !deployPort) {
        error "subdomain, domain, and deployPort must be provided"
    }

    sh '''
        #!/bin/bash
        # Install Nginx and Certbot if not installed
        if ! command -v nginx > /dev/null; then
            echo "Installing Nginx..."
            sudo apt-get update
            sudo apt-get install -y nginx
        fi

        if ! command -v certbot > /dev/null; then
            echo "Installing Certbot..."
            sudo apt-get install -y certbot python3-certbot-nginx
        fi

        # Configure Nginx
        NGINX_CONF="/etc/nginx/sites-available/${subdomain}.${domain}"
        NGINX_CONF_DIR=$(dirname $NGINX_CONF)

        # Create the directory if it doesn't exist
        if [ ! -d "$NGINX_CONF_DIR" ]; then
            sudo mkdir -p $NGINX_CONF_DIR
        fi

        echo "Configuring Nginx for ${subdomain}.${domain}..."
        sudo tee $NGINX_CONF > /dev/null <<EOL
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

        # Enable Nginx configuration
        sudo ln -sf /etc/nginx/sites-available/${subdomain}.${domain} /etc/nginx/sites-enabled/

        # Restart Nginx
        echo "Restarting Nginx..."
        sudo nginx -t && sudo systemctl restart nginx

        # Obtain SSL certificate with Certbot
        echo "Obtaining SSL certificate..."
        sudo certbot --nginx -d ${subdomain}.${domain} --non-interactive --agree-tos -m admin@${domain}

        # Restart Nginx after obtaining SSL certificate
        sudo systemctl restart nginx

        echo "HTTPS configured for https://${subdomain}.${domain}"
    '''
}