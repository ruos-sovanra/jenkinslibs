def call(String githubToken, String repoUrl) {
    // Ensure required variables are provided and non-empty
    if (!githubToken?.trim() || !repoUrl?.trim()) {
        error "GitHub token and repository URL must be provided and cannot be empty"
    }

    echo "GitHub Token: ${githubToken}"
    echo "Repository URL: ${repoUrl}"

    // Write the GitHub token and repository URL to a temporary file
    def tempFile = 'temp_input.txt'
    writeFile file: tempFile, text: "${githubToken}\n${repoUrl}"

    // List the contents of the vars directory
    sh "ls -l vars"

    // Make the shell script executable
    sh "chmod +x vars/createGitHubWebhook.sh"

    // Execute the shell script
    sh """
    #!/bin/bash
    vars/createGitHubWebhook.sh < ${tempFile}
    """

    // Clean up the temporary file
    sh "rm -f ${tempFile}"
}