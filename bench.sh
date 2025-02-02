#!/usr/bin/env bash

set -euo pipefail

# declare -a wls=("a" "b" "c" "f")
declare -a wls=("a" "b" "c")
# declare -a wls=("a")
#declare -a stores=("seq-kv-store" "stm-kv-store" "ohua-kv-store")
#YCSB_THREADCOUNT=10
#YCSB_THREADCOUNT=8
YCSB_THREADCOUNT=1
RUNS=1
#THREADS=$(seq 1 10)
#THREADS=$(seq 2 2 10)
# While I can't compile, there's no point in having threads
THREADS=$(seq 1)
#THREADS=$(seq 1 9 10)


echo "Running the modified kv-store against the original example from smoltcp"
#echo "Enter the name of the store (for folder creation purposes):"
#read kvname


###############################################################################
# spin up the seq store
# echo "Spinning up sequential kv store"
# cd ../ycsb-kv-store/
# cargo build --quiet --release --bin seq-kv-store > /dev/null
# cargo run --quiet --release --bin seq-kv-store > /dev/null &
# KVPID="$!"
# cd ../YCSB/
# 
# sleep 5
# 
# echo "Loading test data"
# bin/ycsb.sh load ohua -P workloads/workloada > /dev/null 2>&1
# 
# echo "Running measurements"
# for wl in "${wls[@]}"
# do
#     echo -n " - ($wl) "
#     mkdir -p results/seq/$wl
#     for it in $(seq 1 $RUNS)
#     do
#         echo -n "."
#         bin/ycsb.sh run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/seq/$wl/$it.txt 2> /dev/null
#     done
#     echo " done!"
# done
# 
# kill $KVPID



###############################################################################
#for tc in ${THREADS[@]}
#do
    ## spin up the ohua store
    #echo "Spinning up ohua kv store ($tc threads)"
    #cd ../ycsb-kv-store/ohua/

    ## compile algorithm
    #sed -i "s/data-parallelism: [0-9]\+/data-parallelism: $tc/" ohua-config.yaml
    #./ohua-compile.sh
    #cd ../

    #cargo build --quiet --release --bin ohua-kv-store > /dev/null
    #cargo run --quiet --release --bin ohua-kv-store -- Ohua > /dev/null &
    #KVPID="$!"
    #cd ../YCSB/
    
    #echo "Loading test data"
    #bin/ycsb.sh load ohua -P workloads/workloada > /dev/null 2>&1
    
    #echo "Running measurements"
    #for wl in "${wls[@]}"
    #do
        #echo -n " - ($wl) "
        #mkdir -p results/ohua/$wl/$tc
        #for it in $(seq 1 $RUNS)
        #do
            #echo -n "."
            #bin/ycsb.sh run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/ohua/$wl/$tc/$it.txt 2> /dev/null
        #done
        #echo " done!"
    #done
    
    #kill $KVPID
#done



###############################################################################
#for tc in ${THREADS[@]}
#do
    ## spin up the stm store
    #echo "Spinning up stm kv store ($tc threads)"
    #cd ../ycsb-kv-store/
    #cargo build --quiet --release --bin stm-kv-store > /dev/null
    #cargo run --quiet --release --bin stm-kv-store -- $tc > /dev/null &
    #KVPID="$!"
    #cd ../YCSB/

    #sleep 5
    
    #echo "Loading test data"
    #bin/ycsb.sh load ohua -P workloads/workloada -threads 1 > /dev/null 2>&1
    
    #echo "Running measurements"
    #for wl in "${wls[@]}"
    #do
        #echo -n " - ($wl) "
        #mkdir -p results/stm/$wl/$tc
        #for it in $(seq 1 $RUNS)
        #do
            #echo -n "."
            #bin/ycsb.sh run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/stm/$wl/$tc/$it.txt 2> /dev/null
        #done
        #echo " done!"
    #done
    
    #kill $KVPID
#done



###############################################################################
#for tc in ${THREADS[@]}
#do
    ## spin up the stm store
    #echo "Spinning up dstm kv store ($tc threads)"
    #cd ../ycsb-kv-store/
    #cargo build --quiet --release --bin dstm-kv-store > /dev/null
    #cargo run --quiet --release --bin dstm-kv-store -- $tc > /dev/null &
    #KVPID="$!"
    #cd ../YCSB/

    #sleep 5
    
    #echo "Loading test data"
    #bin/ycsb.sh load ohua -P workloads/workloada -threads 1 > /dev/null 2>&1
    
    #echo "Running measurements"
    #for wl in "${wls[@]}"
    #do
        #echo -n " - ($wl) "
        #mkdir -p results/dstm/$wl/$tc
        #for it in $(seq 1 $RUNS)
        #do
            #echo -n "."
            #bin/ycsb.sh run ohua -P workloads/workload$wl -threads $YCSB_THREADCOUNT > results/dstm/$wl/$tc/$it.txt 2> /dev/null
        #done
        #echo " done!"
    #done
    
    #kill $KVPID
#done



###############################################################################
for tc in ${THREADS[@]}
do
    # spin up the stm store
    echo "Spinning up original smoltcp version"
    cd ../smoltcp/k-v-original
    # build and throw away the output
    # cargo build --quiet --release --bin k-v-original  > /dev/null
    cargo run --quiet --release --bin k-v-original > /dev/null &
    # Get process id of the running store to close it after testing
    KVPID="$!"
    cd ../../YCSB/

    sleep 4
    
    echo "Loading test data"
    bin/ycsb.sh load ohua -threads 1 -P workloads/workloada > /dev/null 2>&1
    
    echo "Running measurements"
    mkdir -p results/k-v-original/
    for wl in "${wls[@]}"
    do
        echo -n " - ($wl) "
        bin/ycsb.sh run ohua -P workloads/workload$wl -threads 1 > results/k-v-original/$wl.txt 2> /dev/null
        echo " done!"
    done
    
    kill $KVPID
done

echo "Done with first round ... Will wait before second" 
sleep 2 
###############################################################################
for tc in ${THREADS[@]}
do
    # spin up the stm store
    echo "Spinning up Ohua rewritten smoltcp version"
    cd ../smoltcp/k-v-Ohua
    # build and throw away the output
    # cargo build --quiet --release --bin k-v-original  > /dev/null
    cargo run --quiet --release --bin k-v-Ohua > /dev/null &
    # Get process id of the running store to close it after testing
    KVPID="$!"
    cd ../../YCSB/

    sleep 4
    
    echo "Loading test data"
    bin/ycsb.sh load ohua -threads 1 -P workloads/workloada #> /dev/null 2>&1

    echo "Wait after loading data" 
    sleep 3 
    
    echo "Running measurements"
    mkdir -p results/k-v-Ohua/
    for wl in "${wls[@]}"
    do
        echo -n " - ($wl) "
        bin/ycsb.sh run ohua -P workloads/workload$wl -threads 1 > results/k-v-Ohua/$wl.txt #2> /dev/null
        echo " done!"
    done
    
    kill $KVPID
done
###############################################################################