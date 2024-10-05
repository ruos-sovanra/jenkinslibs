def call(String githubToken, String repoUrl) {
    // Ensure required variables are provided and non-empty
    if (!githubToken?.trim() || !repoUrl?.trim()) {
        error "GitHub token and repository URL must be provided and cannot be empty"
    }

    echo "GitHub Token: ${githubToken}"
    echo "Repository URL: ${repoUrl}"

    // List the contents of the current directory for debugging
    sh "ls "

    // Execute the webhook setup directly using a Bash script embedded in the Groovy script
    sh """
    #!/bin/bash

    # GitHub Token and Repository URL from the parameters
    GITHUB_TOKEN="${githubToken}"
    REPO_URL="${repoUrl}"

    # Webhook Configuration
    WEBHOOK_URL="https://jenkins.psa-khmer.world/github-webhook/"  # Replace with your webhook URL
    WEBHOOK_SECRET="1102b43d4bae5e52ede6fc05ee5dc20e91"            # Replace with your webhook secret
    WEBHOOK_EVENTS=("push")   # Customize the events you want

    # Check for jq (JSON processor)
    if ! command -v jq &> /dev/null; then
        echo "jq could not be found. Installing jq..."
        sudo apt-get update
        sudo apt-get install -y jq
    fi

    # Extract GITHUB_USER_OR_ORG and REPO_NAME from the URL using regex
    if [[ \$REPO_URL =~ github\\.com[:/](.+)/(.+)\\.git ]]; then
        GITHUB_USER_OR_ORG="\${BASH_REMATCH[1]}"
        REPO_NAME="\${BASH_REMATCH[2]}"
    elif [[ \$REPO_URL =~ github\\.com[:/](.+)/(.+) ]]; then
        GITHUB_USER_OR_ORG="\${BASH_REMATCH[1]}"
        REPO_NAME="\${BASH_REMATCH[2]}"
    else
        echo "Invalid GitHub URL format. Please ensure the URL is correct."
        exit 1
    fi

    # Convert WEBHOOK_EVENTS array to a JSON-compatible string
    WEBHOOK_EVENTS_JSON=\$(printf '%s\n' "\${WEBHOOK_EVENTS[@]}" | jq -R . | jq -s .)

    # Function to create webhook for the specific repository
    create_webhook() {
        local repo=\$1
        echo "Creating webhook for \$repo"

        response=\$(curl -s -X POST -H "Authorization: token \$GITHUB_TOKEN" \\
            -H "Accept: application/vnd.github.v3+json" \\
            "https://api.github.com/repos/\$repo/hooks" \\
            -d '{
                "name": "web",
                "active": true,
                "events": '"\$WEBHOOK_EVENTS_JSON"',
                "config": {
                    "url": "'"\$WEBHOOK_URL"'",
                    "content_type": "json",
                    "secret": "'"\$WEBHOOK_SECRET"'",
                    "insecure_ssl": "0"
                }
            }')

        # Check if the webhook creation was successful
        if echo "\$response" | grep -q '"id"'; then
            echo "Webhook successfully created for \$repo"
        else
            echo "Failed to create webhook for \$repo: \$response"
        fi
    }

    # Create webhook for the specific repository
    FULL_REPO_NAME="\$GITHUB_USER_OR_ORG/\$REPO_NAME"
    create_webhook "\$FULL_REPO_NAME"

    echo "Webhook setup process completed for \$FULL_REPO_NAME!"
    """
}
