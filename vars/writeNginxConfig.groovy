def call(String subdomain, String domain, String deployPort) {
    if (!subdomain?.trim() || !domain?.trim() || !deployPort?.trim()) {
        error "subdomain, domain, and deployPort must be provided and cannot be empty"
    }

    def folderName = "${subdomain}.${domain}"
    def filePath = "/etc/nginx/sites-available/${folderName}"
    def symlinkPath = "/etc/nginx/sites-enabled/"


    echo >> "server {
                     listen 80;
                     server_name ${subdomain}.${domain};

                     location / {
                         proxy_pass http://localhost:${deployPort};
                         proxy_set_header Host \$host;
                         proxy_set_header X-Real-IP \$remote_addr;
                         proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
                         proxy_set_header X-Forwarded-Proto \$scheme;
                     }
                 } " > ${filePath}

    def nginxConfig = """
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
    """

    try {
        echo "Writing Nginx config to ${filePath}"
        sh """
        echo '${nginxConfig}' > ${filePath}
        """
        echo "Nginx config written successfully"

        // Create symlink in sites-enabled
        sh "ln -sf ${filePath} ${symlinkPath}"
        echo "Symlink created successfully"

        // Reload Nginx to apply changes
        sh "nginx -s reload"
        echo "Nginx reloaded successfully"
    } catch (Exception e) {
        error "Failed to write Nginx config or reload: ${e.getMessage()}"
    }
}