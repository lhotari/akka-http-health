#!/usr/bin/env bash
NAME=${1:-$(cat build.sbt | egrep '^name :=' | awk -F\" '{ print $2 }')}
VERSION=${2:-$(cat build.sbt | egrep '^version :=' | awk -F\" '{ print $2 }')}
echo "Syncing files to Sonatype OSS..."
curl "-u$BINTRAY_USER:$BINTRAY_API_KEY" -H "Content-Type: application/json" \
-d "{\"username\": \"$SONATYPE_USER\", \"password\": \"$SONATYPE_PASS\", \"close\": \"0\"}" \
-X POST "https://api.bintray.com/maven_central_sync/$BINTRAY_USER/releases/$NAME/versions/$VERSION"
echo -e "\nDone."
