#! /bin/bash

# the num of lines stored in each partition
part_root=$1
size=$2
inputFile=$3
diff_on=$4

# set up environment variables
echo "export PARTITION_SIZE=$size" > ./.env-local
echo "export PARTITION_ROOT=$part_root" >> ./.env-local
if [[ diff_on -eq 1 ]]
then
    echo "set diff_on=true"
    echo "export DIFF_ON=true" >> ./.env-local
else
    echo "set diff_on=false"
    echo "export DIFF_ON=false" >> ./.env-local
fi

lines=$(wc -l < $inputFile)

rm -rf ./tmp && mkdir tmp

for ((start=1; start<=lines; start+=size)); do
    end=$((start+size-1))
    output="output-$start-$end.txt"
    sed -n "$start,$end"'p' $inputFile > "./tmp/$output"
done;

echo "Done processing $inputFile with partition size $size with diff_on=$diff_on"