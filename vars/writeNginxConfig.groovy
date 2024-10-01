def call(String subdomain, String domain, String deployPort) {
    if (!subdomain || !domain || !deployPort) {
        error "subdomain, domain, and deployPort must be provided"
    }

    if (subdomain.trim() == "" || domain.trim() == "" || deployPort.trim() == "") {
        error "subdomain, domain, and deployPort cannot be empty"
    }

    def folderName = "${subdomain}.${domain}"
    def filePath = "/etc/nginx/sites-available/${folderName}"

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

    writeFile file: filePath, text: nginxConfig
    sh "ln -sf ${filePath} /etc/nginx/sites-enabled/${folderName}"
}