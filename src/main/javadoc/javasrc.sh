#!/bin/sh

usage="usage: ${0} version"

exit_code=0

release_dir="${HOME}/Releases/smc-${1}"
tar_prefix="SmcSrc_${1}"
smc_modules="example-java-ex6 generator library main model parser smc-maven-plugin"

if [ "${1}" == "" ]
then
    echo "${0}: release version not specified."
    exit_code=1
elif [ "${1}" == "-h" -o "${1}" == "-?" ]
then
    echo ${usage}
else
    smc_sources=$(mktemp)

    if [ ! -d ${release_dir} ]
    then
        mkdir -p ${release_dir}
    fi

    echo "Finding source files."
    for module in ${smc_modules} ;
    do
        find ${module}/src/main/java -name "*.java" -print 1>> ${smc_sources} 2>> /dev/null;
        find ${module}/src/main/smc -name "*.sm" -print 1>> ${smc_sources} 2>> /dev/null;
        find ${module}/target/generated-sources/smc -name "*.java" -print 1>> ${smc_sources} 2>> /dev/null;
    done

    tarfile="${release_dir}/${tar_prefix}.tgz"
    zipfile="${release_dir}/${tar_prefix}.zip"

    echo "Generating tarfile=${tarfile}"
    tar czf ${tarfile} --disable-copyfile -T ${smc_sources}
    chmod 444 ${tarfile}

    echo "Generating zipfile=${zipfile}"
    zip -r ${zipfile} . -i@${smc_sources}
    chmod 444 ${zipfile}

    rm ${smc_sources}
fi

exit ${exit_code}
