def call(String subdomain, String domain, String deployPort) {
    if (!subdomain || !domain || !deployPort) {
        error "subdomain, domain, and deployPort must be provided"
    }

    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    if (subdomain.trim() == "" || domain.trim() == "" || deployPort.trim() == "") {
        error "subdomain, domain, and deployPort cannot be empty"
    }

    writeNginxConfig(subdomain, domain, deployPort)
}