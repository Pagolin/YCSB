#!/usr/bin/env bash

declare -a wls=("a" "b" "c" "f")
#declare -a stores=("seq-kv-store" "stm-kv-store" "ohua-kv-store")
YCSB_THREADCOUNT


#echo "Running the KV Store benchmark against the currently running store."
#echo "Enter the name of the store (for folder creation purposes):"
#read kvname


###############################################################################
# spin up the seq store
echo "Spinning up sequential kv store"
cd ../ycsb-kv-store/
cargo run --quiet --release --bin seq-kv-store > /dev/null &
KVPID="$!"
cd ../YCSB/

echo "Loading test data"
bin/ycsb load ohua -P workloads/workloada > /dev/null

echo "Running measurements"
for wl in "${wls[@]}"
do
    echo -n " - ($wl) "
    mkdir -p results/seq/$wl
    for it in {1..30}
    do
        echo -n "."
        bin/ycsb run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/$kvname/$wl/$it.txt
    done
    echo " done!"
done

kill $KVPID


###############################################################################
for tc in {1..12}
do
    # spin up the stm store
    echo "Spinning up stm kv store ($tc threads)"
    cd ../ycsb-kv-store/
    cargo run --quiet --release --bin stm-kv-store -- $tc > /dev/null &
    KVPID="$!"
    cd ../YCSB/
    
    echo "Loading test data"
    bin/ycsb load ohua -P workloads/workloada > /dev/null
    
    echo "Running measurements"
    for wl in "${wls[@]}"
    do
        echo -n " - ($wl) "
        mkdir -p results/stm/$wl/$tc
        for it in {1..30}
        do
            echo -n "."
            bin/ycsb run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/stm/$wl/$tc/$it.txt
        done
        echo " done!"
    done
    
    kill $KVPID
done



###############################################################################
for tc in {1..12}
do
    # spin up the ohua store
    echo "Spinning up ohua kv store ($tc threads)"
    cd ../ycsb-kv-store/
    sed -i "s/THREADCOUNT: usize = [0-9]\+/THREADCOUNT: usize = $tc/" ohua/src/generated.rs
    cargo run --quiet --release --bin ohua-kv-store > /dev/null &
    KVPID="$!"
    cd ../YCSB/
    
    echo "Loading test data"
    bin/ycsb load ohua -P workloads/workloada > /dev/null
    
    echo "Running measurements"
    for wl in "${wls[@]}"
    do
        echo -n " - ($wl) "
        mkdir -p results/ohua/$wl/$tc
        for it in {1..30}
        do
            echo -n "."
            bin/ycsb run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/ohua/$wl/$tc/$it.txt
        done
        echo " done!"
    done
    
    kill $KVPID
done


