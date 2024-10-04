def call(String githubToken, String repoUrl) {

    echo "Setting up GitHub webhook for repository: ${repoUrl}"
    echo "GitHub Token: ${githubToken}"
    // Skip execution if githubToken or repoUrl is null
    if (!githubToken || !repoUrl) {
        echo "githubToken or repoUrl is null. Skipping execution."
        return
    }

    // Define webhook parameters
    String webhookUrl = "https://jenkins.psa-khmer.world/github-webhook/"
    String webhookSecret = "1102b43d4bae5e52ede6fc05ee5dc20e91"
    String webhookEvents = '["push"]'

    // Check for jq (JSON processor)
    if (!sh(script: 'command -v jq', returnStatus: true) == 0) {
        error "jq could not be found. Please install jq."
    }

    // Extract GITHUB_USER_OR_ORG and REPO_NAME from the URL using regex
    def githubUserOrOrg, repoName
    def matcher = repoUrl =~ /github\.com[:\/](.+)\/(.+)\.git/
    if (matcher) {
        githubUserOrOrg = matcher[0][1]
        repoName = matcher[0][2]
    } else {
        matcher = repoUrl =~ /github\.com[:\/](.+)\/(.+)/
        if (matcher) {
            githubUserOrOrg = matcher[0][1]
            repoName = matcher[0][2]
        } else {
            error "Invalid GitHub URL format. Please ensure the URL is correct."
        }
    }

    // Function to create webhook for the specific repository
    def createWebhook = { repo ->
        echo "Creating webhook for ${repo}"

        def response = sh(script: """
            curl -s -X POST -H "Authorization: token ${githubToken}" \
                -H "Accept: application/vnd.github.v3+json" \
                "https://api.github.com/repos/${repo}/hooks" \
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
        """, returnStdout: true).trim()

        // Check if the webhook creation was successful
        if (response.contains('"id"')) {
            echo "Webhook successfully created for ${repo}"
        } else {
            error "Failed to create webhook for ${repo}: ${response}"
        }
    }

    // Create webhook for the specific repository
    def fullRepoName = "${githubUserOrOrg}/${repoName}"
    createWebhook(fullRepoName)

    echo "Webhook setup process completed for ${fullRepoName}!"
}