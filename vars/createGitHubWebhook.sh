#!/bin/bash

# Configuration
WEBHOOK_URL="https://jenkins.psa-khmer.world/github-webhook/"  # Replace with your webhook URL
WEBHOOK_SECRET="1102b43d4bae5e52ede6fc05ee5dc20e91"        # Replace with your webhook secret
WEBHOOK_EVENTS='["push"]'   # Customize the events you want

# Prompt user for GitHub Token
read -s -p "Enter your GitHub token: " GITHUB_TOKEN
echo

# Prompt user for the GitHub repository URL
read -p "Enter the GitHub repository URL: " REPO_URL

# Check for jq (JSON processor)
if ! command -v jq &> /dev/null; then
    echo "jq could not be found. Please install jq."
    exit 1
fi

# Extract GITHUB_USER_OR_ORG and REPO_NAME from the URL using regex
if [[ $REPO_URL =~ github\.com[:/](.+)/(.+)\.git ]]; then
    GITHUB_USER_OR_ORG="${BASH_REMATCH[1]}"
    REPO_NAME="${BASH_REMATCH[2]}"
elif [[ $REPO_URL =~ github\.com[:/](.+)/(.+) ]]; then
    GITHUB_USER_OR_ORG="${BASH_REMATCH[1]}"
    REPO_NAME="${BASH_REMATCH[2]}"
else
    echo "Invalid GitHub URL format. Please ensure the URL is correct."
    exit 1
fi

# Function to create webhook for the specific repository
create_webhook() {
    local repo=$1
    echo "Creating webhook for $repo"

    response=$(curl -s -X POST -H "Authorization: token $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        "https://api.github.com/repos/$repo/hooks" \
        -d '{
            "name": "web",
            "active": true,
            "events": '"$WEBHOOK_EVENTS"',
            "config": {
                "url": "'"$WEBHOOK_URL"'",
                "content_type": "json",
                "secret": "'"$WEBHOOK_SECRET"'",
                "insecure_ssl": "0"
            }
        }')

    # Check if the webhook creation was successful
    if echo "$response" | grep -q '"id"'; then
        echo "Webhook successfully created for $repo"
    else
        echo "Failed to create webhook for $repo: $response"
    fi
}

# Create webhook for the specific repository
FULL_REPO_NAME="$GITHUB_USER_OR_ORG/$REPO_NAME"
create_webhook "$FULL_REPO_NAME"

echo "Webhook setup process completed for $FULL_REPO_NAME!"