#!/usr/bin/env bash

SOURCE="$0"
while [[ -h "$SOURCE"  ]]; do # resolve $SOURCE until the file is no longer a symlink
    scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
    SOURCE="$(readlink "$SOURCE")"
    [[ ${SOURCE} != /*  ]] && SOURCE="$scriptDir/$SOURCE" # if $SOURCE was a relative symlink, we need to resolve it relative to the path where the symlink file was located
done
scriptDir="$( cd -P "$( dirname "$SOURCE"  )" && pwd  )"
basedir=${scriptDir%/*}
echo "baseDir:"${basedir}
cd ${basedir}

if [[ ! -f "$basedir/.gradle/publish.gradle" ]];then
    echo please edit the publish.gradle:${basedir}/.gradle/publish.gradle
    cp ${basedir}/script/publish.gradle ${basedir}/.gradle/publish.gradle
else
    mv ${basedir}/script/publish.gradle ${basedir}/script/publish.gradle.bak
    cp ${basedir}/.gradle/publish.gradle ${basedir}/script/publish.gradle
    ./gradlew clean publish --stacktrace
    mv ${basedir}/script/publish.gradle.bak ${basedir}/script/publish.gradle
fi

