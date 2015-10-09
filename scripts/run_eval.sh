#!/bin/bash

export MAVEN_OPTS="-Xmx128G"

args=(${@// /\\ })
mvn exec:java -Dexec.mainClass="de.mpii.wiki.evaluation.WikipediaMappingEvaluator" -Dorg.postgresql.forcebinary=true -Dexec.args="${args[*]}"

