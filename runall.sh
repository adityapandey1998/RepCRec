#!/bin/bash

max=23
mkdir -p myoutputs

for i in `seq 1 $max`
do
   echo "Running test case ${i}:"
   java -jar RepCRep.jar tests/test${i}.txt > myoutputs/out${i}.txt
   echo "Passed!"
done
