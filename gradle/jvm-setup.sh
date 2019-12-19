#!/usr/bin/env sh
#
# This script may be sourced or run directly to install and/or update the following to specified versions:
# * Java
#
# If $JVM_JAVA_HOME is specified it will override JAVA_HOME and Java will not automatically be installed
#
JVM_MAJOR_VERSION_DEFAULT=8
JVM_RELEASE_DEFAULT=latest
JVM_IMPL_DEFAULT=hotspot
JVM_TYPE_DEFAULT=jdk
#set -x
set -e
# Check for environment JVM version and release variables, otherwise specify the default.
if [ -z "$JVM_MAJOR_VERSION" ]; then
	JVM_MAJOR_VERSION=${JVM_MAJOR_VERSION_DEFAULT}
fi
if [ -z "$JVM_RELEASE" ]; then
	JVM_RELEASE=${JVM_RELEASE_DEFAULT} # latest, jdk8u192-b12, ...
fi
if [ -z "$JVM_TYPE" ]; then
	JVM_TYPE=${JVM_TYPE_DEFAULT} # jdk, jre
fi
if [ -z "$JVM_IMPL" ]; then
	JVM_IMPL=${JVM_IMPL_DEFAULT} # hotspot, openj9
fi
if [ -z "$JVM_OS" ]; then
	JVM_OS=linux # windows, linux, mac
fi
if [ -z "$JVM_ARCH" ]; then
	JVM_ARCH=x64 # x64, x32, ppc64, s390x, ppc64le, aarch64
fi
if [ -z "$JVM_HEAP" ]; then
	JVM_HEAP=normal # normal, large
fi

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ]; do
	ls=$(ls -ld "$PRG")
	link=$(expr "$ls" : '.*-> \(.*\)$')
	if expr "$link" : '/.*' >/dev/null; then
		PRG="$link"
	else
		PRG=$(dirname "$PRG")"/$link"
	fi
done
SAVED="$(pwd)"
cd "$(dirname "$PRG")/" || exit 1
SCRIPT_DIR="$(pwd -P)"
PROJECT_ROOT="$( cd "$SCRIPT_DIR" && pwd )"
cd "$SAVED" || exit 1

# Check for environment variables for where to store the JVM, otherwise specify the default.
if [ -z "$JVM_DIR" ]; then
	JVM_DIR="${PROJECT_ROOT}/gradle/jvm/${JVM_MAJOR_VERSION}/${JVM_TYPE}${JVM_RELEASE}"
fi
if [ -z "$JVM_INFO_FILE" ]; then
	JVM_INFO_FILE="/tmp/jvm_info_${JVM_RELEASE}.json"
fi
if [ -z "$JVM_ARCHIVE" ]; then
	JVM_ARCHIVE="/tmp/jvm_${JVM_RELEASE}.tar.gz"
fi
# Check for environment variable defining different http client, otherwise use curl
if [ -z "$HTTP_CLIENT" ]; then
	HTTP_CLIENT="curl -sS -f -L -o"
fi

string_replace() {
	echo "$1" | awk -v srch="$2" -v repl="$3" '{ sub(srch,repl,$0); print $0 }'
}

file_age() {
	file_created_time=$(date -r "$1" +%s)
	time_now=$(date +%s)
	age=$((time_now - file_created_time))
	echo "${age}"
}

url_file() {
	sourceURL=$1
	destFile=$2
	if ${HTTP_CLIENT} "${destFile}" "${sourceURL}"; then
	echo "Downloaded ${sourceURL} to ${destFile}"
	else
		echo "Failed to download ${sourceURL} to ${destFile}"
		echo "If you have an old version of libssl you may not have the correct"
		echo "certificate authority. Either upgrade or set HTTP_CLIENT to insecure:"
		echo "  export HTTP_CLIENT=\"wget --no-check-certificate -O\" # or"
		echo "  export HTTP_CLIENT=\"curl --insecure -f -L -o"
		rm "${destFile}" 2>/dev/null
		exit 1
	fi
}

sha_for() {
	if command -v sha256sum >/dev/null; then
		sha256sum "$1" | awk '{print $1}'
	elif command -v shasum >/dev/null; then
		shasum -a 256 "$1" | awk '{print $1}'
	else
		echo "Neither shasum nor sha256sum were found in the PATH"
		exit 1
	fi
}

shaurl_file() {
	archiveURL=$1
	shaURL=$2
	archiveFile=$3
	url_file "${shaURL}" "${archiveFile}.sha256.txt"
	url_file "${archiveURL}" "${archiveFile}"
	sha=$(awk '{print $1}' <"${archiveFile}.sha256.txt")
	calculated_sha=$(sha_for "${archiveFile}")
	if [ "${calculated_sha}" = "${sha}" ]; then
		echo "sha ok"
	else
		echo "sha did not match!"
		exit 1
	fi
}

shaurl_unarchive() {
	archiveURL=$1
	shaURL=$2
	archiveFile=$3
	destDir=$4
	shaurl_file "${archiveURL}" "${shaURL}" "${archiveFile}"
	mkdir -p "${destDir}"
	echo "Expanding ${archiveURL} to ${destDir}"
	stripCount=$(tar -tzf "${archiveFile}" | grep '/lib/ext/$' | awk -F"/" '{print NF-4}')
	if tar -xf "${archiveFile}" --strip-components=$stripCount -C "${destDir}"; then
		if ! rm "${archiveFile}" || ! rm "${archiveFile}.sha256.txt"; then
			echo "Could not delete either ${archiveFile} or ${archiveFile}.sha256.txt"
		fi
	else
		echo "Failed to expand ${archiveURL} to ${destDir}"
		exit 1
	fi
}

jvm_install() {
	echo "Requesting Java ${JVM_MAJOR_VERSION} ${JVM_RELEASE} release"
	needsJvmInfo=true
	if [ -f "${JVM_INFO_FILE}" ]; then
		fileAge=$(file_age "${JVM_INFO_FILE}")
		if [ "$fileAge" -lt 86400 ]; then
			needsJvmInfo=false
			echo "${JVM_INFO_FILE} is less than 24 hours old, not re-downloading"
		fi
	fi
	if [ "$needsJvmInfo" = "true" ]; then
		jvmInfoURL="https://api.adoptopenjdk.net/v2/info/releases/openjdk${JVM_MAJOR_VERSION}?openjdk_impl=${JVM_IMPL}&arch=${JVM_ARCH}&os=${JVM_OS}&type=${JVM_TYPE}&release=${JVM_RELEASE}"
		url_file "${jvmInfoURL}" "${JVM_INFO_FILE}"
	fi
	releaseName=$(grep -Po '"release_name":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	binaryURL=$(grep -Po '"binary_link":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	shaURL=$(grep -Po '"checksum_link":.*?[^\\]",' "${JVM_INFO_FILE}" | awk -F'"' '{print $4}')
	needsJvmDownload=true
	if [ "$JVM_RELEASE" = "latest" ]; then
		JVM_DIR=$(string_replace "${JVM_DIR}" "latest" "${releaseName}")
	fi
	if [ -f "${JVM_DIR}/bin/java" ] && [ -x "${JVM_DIR}/bin/java" ]; then
		needsJvmDownload=false
		echo "${JVM_DIR} exists, not re-downloading"
	fi
	if [ "$needsJvmDownload" = "true" ]; then
		echo "Downloading ${binaryURL}"
		shaurl_unarchive "${binaryURL}" "${shaURL}" "${JVM_ARCHIVE}" "${JVM_DIR}"
	fi
	export JAVA_HOME=${JVM_DIR}
	echo "Java ${JVM_TYPE} ${JVM_MAJOR_VERSION} ${releaseName} - JAVA_HOME: ${JVM_DIR}"
}

if [ -z "$JVM_JAVA_HOME" ]; then
	if [ -f "${JVM_DIR}/bin/java" ] && [ -x "${JVM_DIR}/bin/java" ]; then
		JAVA_HOME=${JVM_DIR}
	else
		echo "${JVM_DIR} does not exist!  Getting JVM..."
		jvm_install
	fi
else
	echo "Using JVM_JAVA_HOME: $JVM_JAVA_HOME"
	JAVA_HOME=$JVM_JAVA_HOME
fi
export JAVA_HOME
