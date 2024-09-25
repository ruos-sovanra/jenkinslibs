def call(String projectPath) {
    // Detect the project type based on the project files
    def projectType = detectProjectType(projectPath)

    if (projectType) {
        // Log the detected project type in the Jenkins console
        echo "Detected project type: ${projectType}"
        // Write the appropriate Dockerfile based on the detected project type
        writeDockerfile(projectType, projectPath)
        return projectType
    } else {
        // If unable to detect project type, throw an error
        error "Unable to detect the project type."
    }
}

// Function to detect the project type based on the project directory structure
def detectProjectType(String projectPath) {
    // Check for Node.js-based projects by looking for a package.json file
    if (fileExists("${projectPath}/package.json")) {
        def packageJson = readJSON file: "${projectPath}/package.json"

        // Check for Next.js project by identifying 'next' in dependencies
        if (packageJson.dependencies?.'next') {
            return 'nextjs'
        }
        // Check for React project by identifying 'react' in dependencies
        else if (packageJson.dependencies?.'react') {
            return 'react'
        }
    }
    // Check for Java Spring Boot projects by looking for pom.xml (Maven) or build.gradle (Gradle) file
    else if (fileExists("${projectPath}/pom.xml")) {
        return 'springboot-maven'
    } else if (fileExists("${projectPath}/build.gradle")) {
        return 'springboot-gradle'
    }

    // If no match, return null to indicate the type couldn't be detected
    return null
}

// Function to write the Dockerfile for the detected project type
def writeDockerfile(String projectType, String projectPath) {
    // Load the appropriate Dockerfile template from the shared library's resources
    def dockerfileContent = libraryResource "dockerfileTemplates/Dockerfile-${projectType}"

    // Write the loaded Dockerfile content into the project directory
    writeFile file: "${projectPath}/Dockerfile", text: dockerfileContent

    // Log the Dockerfile creation to the Jenkins console
    echo "Dockerfile written for ${projectType} project at ${projectPath}/Dockerfile"
}
