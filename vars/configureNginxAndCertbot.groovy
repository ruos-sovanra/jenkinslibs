def call(String subdomain, String domain, String deployPort) {
    // Ensure required variables are provided and non-empty
    if (!subdomain?.trim() || !domain?.trim() || !deployPort?.trim()) {
        error "subdomain, domain, and deployPort must be provided and cannot be empty"
    }

    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    def templateFile = libraryResource 'nginx Templates/configNginx.template'
    def configFilePath = "/etc/nginx/sites-available/${subdomain}.${domain}"

    // Check if the config file already exists
    if (fileExists(configFilePath)) {
        echo "Nginx configuration for ${subdomain}.${domain} already exists."
        return // Exit the function early to avoid overwriting
    }

    // Replace placeholders in the template
    def configContent = templateFile.replace('${domain}', domain)
                                    .replace('${subdomain}', subdomain)
                                    .replace('${deployPort}', deployPort)

    // Write the configuration to the file
    writeFile file: configFilePath, text: configContent

    // Debug step: print out the variables to ensure they are not empty
    echo "Generated Nginx Config:"
    echo configContent

    // Using Groovy variable interpolation inside the shell block
    sh """
    #!/bin/bash

    # Create a symlink to enable the site in Nginx
    ln -s ${configFilePath} /etc/nginx/sites-enabled/

    # Test Nginx configuration and reload Nginx
    nginx -t && systemctl reload nginx

    echo "Nginx configuration for ${subdomain}.${domain} has been created and deployed."

    certbot --nginx -d ${subdomain}.${domain}

    echo "Certbot has been run for ${subdomain}.${domain}"

    """

    echo "Your website is now live at https://${subdomain}.${domain}"
}
