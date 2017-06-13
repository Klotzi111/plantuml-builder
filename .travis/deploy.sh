#!/bin/bash

# Pull requests and commits to other branches shouldn't try to deploy, just build to verify
if [ "$TRAVIS_PULL_REQUEST" != "false" -o "$TRAVIS_BRANCH" != "master" ]; then
    echo "Skipping deploy; just doing a build."
    exit 0
fi

MAKE_RELEASE='false'

if [[ "${TRAVIS_COMMIT_MESSAGE}" =~ ^make\ release ]]; then 
    echo "commit message indicate that a release must be create"
    MAKE_RELEASE='true'
fi

if [ "$MAKE_RELEASE" = 'true' ]; then
    echo "create release from actual SNAPSHOT"
    if ! mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion} versions:commit; then
        err "set release version failed"
        exit 1
    fi
else
    echo "keep snapshot version in pom.xml"
fi

if [ "$TRAVIS_BRANCH" = 'master' ] && [ "$TRAVIS_PULL_REQUEST" = 'false' ]; then
    echo "deploy version to maven centrale"
    if ! mvn deploy --settings .travis/settings.xml -DperformRelease=true -DskipTests=true -B -U; then
        err "maven deploy failed"
        exit 1
    fi
fi

if [ "$MAKE_RELEASE" = 'true' ]; then
    git config user.name "Travis CI"
    git config user.email "travis-ci@ifocusit.ch"
    #c8a1526c7ec595d2fca457de79893f9b8d631276
    PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version | grep -v "^\[")
    GIT_TAG=v$PROJECT_VERSION
    echo "create git tag $GIT_TAG"
    if ! git tag "$GIT_TAG" -a -m "Generated tag from TravisCI for build $TRAVIS_BUILD_NUMBER"; then
        err "failed to create git tag: $git_tag"
        exit 1
    fi

    echo "set next development version"
    if ! mvn build-helper:parse-version versions:set -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.nextMinorVersion}-SNAPSHOT versions:commit; then
        err "set next dev version failed"
        exit 1
    fi
    
    # push new version
    if ! git push --quiet --tags "https://$GITHUB_TOKEN@github.com/$TRAVIS_REPO_SLUG" > /dev/null 2>&1; then
        err "failed to push git tags"
        exit 1
    fi
fi
