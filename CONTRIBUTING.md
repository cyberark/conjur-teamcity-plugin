# Contributing
For general contribution and community guidelines, please see the [community repo](https://github.com/cyberark/community).

## Table of Contents

- [Development](#development)
- [Testing](#testing)
- [Releases](#releases)
- [Contributing](#contributing-workflow)

## Development
Tools required:
- docker
- docker-compose
- jdk 8
- mvn


Navigate into the `dev` directory and execute the following:
- [Conjur installation](https://github.com/cyberark/conjur-quickstart/blob/master/test_workflow.sh)
- Setup TeamCity: `docker-compose up -d`

## Testing
To run unit tests execute the following:
```bash
mvn test
```

## Releases
To create a new release in this project, follow the standard [release guidelines](https://github.com/cyberark/community/blob/master/Conjur/CONTRIBUTING.md#release-process).

## Contributing workflow

1. [Fork the project](https://help.github.com/en/github/getting-started-with-github/fork-a-repo)
2. [Clone your fork](https://help.github.com/en/github/creating-cloning-and-archiving-repositories/cloning-a-repository)
3. Make local changes to your fork by editing files
3. [Commit your changes](https://help.github.com/en/github/managing-files-in-a-repository/adding-a-file-to-a-repository-using-the-command-line)
4. [Push your local changes to the remote server](https://help.github.com/en/github/using-git/pushing-commits-to-a-remote-repository)
5. [Create new Pull Request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/creating-a-pull-request-from-a-fork)

From here your pull request will be reviewed and once you've responded to all
feedback it will be merged into the project. Congratulations, you're a contributor!
