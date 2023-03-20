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

if [[ ! -f "$basedir/.gradle/publish.properties" ]];then
    echo please edit the publish.properties:${basedir}/.gradle/publish.properties
    cp ${basedir}/script/publish.properties ${basedir}/.gradle/publish.properties
else
    mv ${basedir}/script/publish.properties ${basedir}/script/publish.properties.bak
    cp ${basedir}/.gradle/publish.properties ${basedir}/script/publish.properties
    cp ${basedir}/gradle.properties ${basedir}/gradle.properties.bak
    echo '\n' >> ${basedir}/gradle.properties
    cat ${basedir}/script/publish.properties >> ${basedir}/gradle.properties
    echo "start clean"
    ./gradlew clean --stacktrace
    echo "start publish"
    ./gradlew publish --stacktrace
    echo "publish completed"
    mv ${basedir}/script/publish.properties.bak ${basedir}/script/publish.properties
    mv ${basedir}/gradle.properties.bak ${basedir}/gradle.properties
fi

