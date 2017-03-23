#!/usr/bin/env bash
NAME=${1:-$(cat build.sbt | egrep '^name :=' | awk -F\" '{ print $2 }')}
VERSION=${2:-$(cat build.sbt | egrep '^version :=' | awk -F\" '{ print $2 }')}
echo "Signing released files..."
curl -H "X-GPG-PASSPHRASE: $GPG_PASSPHRASE" "-u$BINTRAY_USER:$BINTRAY_API_KEY" -X POST "https://api.bintray.com/gpg/$BINTRAY_USER/releases/$NAME/versions/$VERSION"
