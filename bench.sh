#!/usr/bin/env bash

declare -a wls=("a" "b" "c" "f")


echo "Running the KV Store benchmark against the currently running store."
echo "Enter the name of the store (for folder creation purposes:"
read kvname

echo "Loading test data"
bin/ycsb load ohua -P workloads/workloada > /dev/null


for wl in "${wls[@]}"
do
    for tc in {1..24}
    do
        mkdir -p results/$kvname/$wl/$tc
        for it in {1..30}
        do
            bin/ycsb run ohua -P workloads/workload$wl -threads $tc > results/$kvname/$wl/$tc/$it.txt
        done
    done
done



