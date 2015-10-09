#!/bin/bash

export MAVEN_OPTS="-Xmx128G -agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"

args=(${@// /\\ })
mvn exec:java -Dexec.mainClass="de.mpii.wiki.WikipediaRevisionMapper" -Dorg.postgresql.forcebinary=true -Dexec.args="${args[*]}"
