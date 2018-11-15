#!/bin/bash

MIN_CORES=1
MAX_CORES=12
OUTPUT_FILE="benchmarks"

lscpu > $OUTPUT_FILE
echo >> $OUTPUT_FILE
echo >> $OUTPUT_FILE

for cores in `seq $MIN_CORES $MAX_CORES` ; do
	echo "RUNNING ON $cores CORES"
	echo "RUNNING ON $cores CORES" >> $OUTPUT_FILE
	make run PC=$cores >> $OUTPUT_FILE
	echo -e "-----------------------\n\n" >> $OUTPUT_FILE
done

echo "FINISHED"

