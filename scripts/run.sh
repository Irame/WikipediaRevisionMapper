#!/bin/bash

export MAVEN_OPTS="-Xmx12G"

args=(${@// /\\ })
mvn exec:java -Dexec.mainClass="de.mpii.wiki.WikipediaRevisionMapper" -Dorg.postgresql.forcebinary=true -Dexec.args="${args[*]}"
