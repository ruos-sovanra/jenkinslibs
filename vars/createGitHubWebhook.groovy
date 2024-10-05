import groovy.json.JsonOutput
import groovy.json.JsonSlurper

def createGitHubWebhook(String githubToken, String gitRepoUrl) {
    // Extract the repository owner and name from the URL
    def repoParts = gitRepoUrl.split('/')
    def repoOwner = repoParts[-2] // Second-to-last part is the repo owner
    def repoName = repoParts[-1].replace('.git', '') // Last part is the repo name, removing .git if present

    // Webhook URL for Jenkins
    def webhookUrl = "${env.JENKINS_URL}github-webhook/"

    // API URL for GitHub webhooks
    def apiUrl = "https://api.github.com/repos/${repoOwner}/${repoName}/hooks"

    // Webhook payload
    def payload = [
        name   : 'web',
        active : true,
        events : ['push'],
        config : [
            url         : webhookUrl,
            content_type: 'json',
            insecure_ssl: '0'
        ]
    ]

    // Convert payload to JSON
    def payloadJson = JsonOutput.toJson(payload)

    // Execute GitHub API call to create the webhook
    def response = httpRequest(
        acceptType    : 'APPLICATION_JSON',
        contentType   : 'APPLICATION_JSON',
        customHeaders : [[name: 'Authorization', value: "token ${githubToken}", maskValue: true]],
        httpMode      : 'POST',
        requestBody   : payloadJson,
        url           : apiUrl
    )

    // Parse response
    def jsonResponse = new JsonSlurper().parseText(response.content)

    if (response.status == 201) {
        echo "Webhook created successfully: ${jsonResponse.url}"
    } else if (response.status == 422) {
        echo "Webhook already exists: ${jsonResponse.errors[0].message}"
    } else {
        error "Failed to create webhook: ${jsonResponse.message}"
    }
}
