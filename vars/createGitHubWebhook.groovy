def call(String githubToken, String repoUrl) {
    def webhookUrl = "https://jenkins.psa-khmer.world/github-webhook/"
    def webhookSecret = "1102b43d4bae5e52ede6fc05ee5dc20e91"
    def webhookEvents = '["push"]'

    // Validate GitHub Token
    if (!githubToken) {
        error "GitHub token is required to create the webhook."
    }

    // Validate Repository URL
    if (!repoUrl) {
        error "GitHub repository URL is required."
    }

    // Extract user/organization and repository name (do this inline)
    def userOrOrg = ''
    def repoName = ''

    // Adjust regex to handle both cases: with and without ".git"
    if (repoUrl ==~ /github\.com[:\/](.+?)\/(.+?)(?:\.git)?$/) {
        def matcher = repoUrl =~ /github\.com[:\/](.+?)\/(.+?)(?:\.git)?$/
        userOrOrg = matcher[0][1]
        repoName = matcher[0][2]
    } else {
        error "Invalid GitHub repository URL format."
    }

    def fullRepoName = "${userOrOrg}/${repoName}"

    // Create Webhook
    def response = sh(
        script: """
            curl -s -X POST -H "Authorization: token ${githubToken}" \
            -H "Accept: application/vnd.github.v3+json" \
            https://api.github.com/repos/${fullRepoName}/hooks \
            -d '{
                "name": "web",
                "active": true,
                "events": ${webhookEvents},
                "config": {
                    "url": "${webhookUrl}",
                    "content_type": "json",
                    "secret": "${webhookSecret}",
                    "insecure_ssl": "0"
                }
            }'
        """,
        returnStdout: true
    ).trim()

    if (response.contains('"id"')) {
        echo "Webhook successfully created for ${fullRepoName}"
    } else {
        error "Failed to create webhook for ${fullRepoName}: ${response}"
    }
}
